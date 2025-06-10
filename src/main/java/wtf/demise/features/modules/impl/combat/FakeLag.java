package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.*;
import net.minecraft.util.*;
import org.apache.commons.lang3.Range;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.packet.PacketReleaseEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.legit.BackTrack;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;

@ModuleInfo(name = "FakeLag", description = "Abuses latency in order to be unpredictable to your target.", category = ModuleCategory.Combat)
public class FakeLag extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Pulse", "Spoof"}, "Pulse", this);
    private final BoolValue smart = new BoolValue("Smart", true, this);
    private final BoolValue modifyMovementYaw = new BoolValue("Modify movement yaw", false, this, smart::get);
    private final SliderValue attackRange = new SliderValue("Attack range", 4, 0, 15, 0.1f, this, () -> !smart.get());
    private final BoolValue alwaysSpoof = new BoolValue("Always spoof", false, this);
    private final SliderValue searchRange = new SliderValue("Search range", 6, 1, 15, 0.1f, this, () -> !alwaysSpoof.get());
    private final SliderValue recoilTime = new SliderValue("Recoil time (ms)", 50, 0, 1000, this);
    private final SliderValue delayMin = new SliderValue("Delay (min ms)", 100, 0, 1000, this);
    private final SliderValue delayMax = new SliderValue("Delay (max ms)", 100, 1, 1000, this);
    private final SliderValue delayOnOpponentAttack = new SliderValue("Delay on opponent attack", 200, 0, 500, this, () -> mode.is("Spoof"));
    private final ModeValue keepRangeMode = new ModeValue("Keep range mode", new String[]{"None", "WTap", "Timer down"}, "None", this, smart::get);
    private final SliderValue timer = new SliderValue("Timer", 0.75f, 0.01f, 1, 0.01f, this, () -> keepRangeMode.get().equals("Timer down"));
    private final SliderValue timerTicks = new SliderValue("Timer ticks", 1, 1, 10, 1, this, () -> keepRangeMode.get().equals("Timer down"));
    private final BoolValue experimentalDynamicHurtTime = new BoolValue("Experimental dynamic hurt time", false, this, smart::get);
    private final BoolValue pauseOnBacktrack = new BoolValue("Pause on backtrack", false, this);
    private final BoolValue forceFirstHit = new BoolValue("Force first hit", false, this);
    private final BoolValue realPos = new BoolValue("Display real pos", true, this);
    private final ModeValue renderMode = new ModeValue("Render mode", new String[]{"Box", "FakePlayer"}, "FakePlayer", this, realPos::get);
    private final BoolValue onlyOnGround = new BoolValue("Only onGround", false, this);
    private final BoolValue onlyKillAura = new BoolValue("Only on killAura", false, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);

    public static boolean blinking = false, picked = false;
    private final TimerUtils delay = new TimerUtils();
    private final TimerUtils recoilTimer = new TimerUtils();
    private static double x, y, z;
    private double lerpX, lerpY, lerpZ;
    public EntityPlayer target;
    private int ms;
    private boolean attacked;
    private boolean dispatched;
    private float yaw;
    private final TimerUtils hurtTimer = new TimerUtils();
    private boolean forceWTap;
    private boolean isFirstHit = true;
    private boolean shouldTimer;
    private int targetHits;

    //lol
    private final TimerUtils timerTimer = new TimerUtils();

    @Override
    public void onEnable() {
        blinking = false;
        picked = false;
        x = y = z = lerpX = lerpY = lerpZ = 0;
        target = null;

        delay.reset();
        recoilTimer.reset();
        isFirstHit = true;
    }

    @Override
    public void onDisable() {
        switch (mode.get()) {
            case "Pulse":
                BlinkComponent.dispatch(true);
                break;
            case "Spoof":
                PingSpoofComponent.disable();
                PingSpoofComponent.dispatch();
                break;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(ms + " ms");

        target = PlayerUtils.getTarget(alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get() + 1, teamCheck.get());

        if (ms == 0) ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());

        if (target == null || (onlyOnGround.get() && !mc.thePlayer.onGround) || (onlyKillAura.get() && !getModule(KillAura.class).isEnabled()) || (pauseOnBacktrack.get() && getModule(BackTrack.class).isEnabled() && BackTrack.shouldLag) || mc.thePlayer.isDead) {
            if (blinking) {
                switch (mode.get()) {
                    case "Pulse":
                        BlinkComponent.dispatch(true);
                        break;
                    case "Spoof":
                        PingSpoofComponent.disable();
                        PingSpoofComponent.dispatch();
                        break;
                }
                blinking = false;
            }

            if (picked) picked = false;
            return;
        }

        if (PlayerUtils.getDistanceToEntityBox(target) > 3) {
            isFirstHit = true;
        }

        switch (mode.get()) {
            case "Pulse":
                if (shouldLag()) {
                    if (recoilTimer.hasTimeElapsed((long) recoilTime.get())) {
                        blinking = true;
                    }

                    if (delay.hasTimeElapsed(ms) && blinking) {
                        blinking = false;
                        delay.reset();
                        recoilTimer.reset();
                    }

                    if (blinking) {
                        if (!picked) {
                            x = mc.thePlayer.posX;
                            y = mc.thePlayer.posY;
                            z = mc.thePlayer.posZ;
                            picked = true;
                        }
                        BlinkComponent.blinking = true;
                    } else {
                        BlinkComponent.dispatch(true);
                        picked = false;
                    }
                } else {
                    ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());
                    if (blinking) {
                        BlinkComponent.dispatch(true);
                    }
                    picked = false;
                    if (delay.hasTimeElapsed(ms) && blinking) {
                        blinking = false;
                        delay.reset();
                        recoilTimer.reset();
                    }
                }
                break;
            case "Spoof":
                if (shouldLag()) {
                    if (recoilTimer.hasTimeElapsed((long) recoilTime.get())) {
                        if (isTargetAimingAtServerPos() && target.isSwingInProgress && smart.get()) {
                            ms = (int) delayOnOpponentAttack.get();
                        } else {
                            ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());
                        }

                        PingSpoofComponent.spoof(ms, true, false, false, false, true, true, false);
                        blinking = true;
                        dispatched = false;
                    } else {
                        blinking = false;
                    }

                    if (smart.get() && attemptingToStrafe() && modifyMovementYaw.get()) {
                        float targetYaw = target.rotationYaw + 90;

                        double posX = -MathHelper.sin((float) Math.toRadians(targetYaw)) * 2 + target.posX;
                        double posZ = MathHelper.cos((float) Math.toRadians(targetYaw)) * 2 + target.posZ;

                        this.yaw = getYaw(mc.thePlayer, new Vec3(posX, target.posY, posZ));
                    }
                } else {
                    if (!dispatched) {
                        PingSpoofComponent.disable();
                        PingSpoofComponent.dispatch();
                        dispatched = true;
                    }
                    blinking = false;
                    recoilTimer.reset();
                    delay.reset();
                    picked = false;
                }

                if (!blinking) {
                    x = lerpX = mc.thePlayer.posX;
                    y = lerpY = mc.thePlayer.posY;
                    z = lerpZ = mc.thePlayer.posZ;
                }
                break;
        }

        attacked = false;
    }

    @EventTarget
    public void onTick(TickEvent e) {
        if (target != null && forceFirstHit.get() && isFirstHit && !mc.thePlayer.isUsingItem() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            mc.clickMouse();
            ChatUtils.sendMessageClient("forced first hit");
            isFirstHit = false;
        }
    }

    @EventTarget
    public void onPacketRelease(PacketReleaseEvent e) {
        if (e.getTimedPacket().getPacket() instanceof C03PacketPlayer c03) {
            x = c03.getPositionX();
            y = c03.getPositionY();
            z = c03.getPositionZ();
        }
    }

    @EventPriority(-100)
    @EventTarget
    public void onMove(MoveInputEvent e) {
        if (smart.get() && shouldLag() && attemptingToStrafe() && modifyMovementYaw.get()) {
            MoveUtil.fixMovement(e, RotationManager.shouldRotate() ? RotationManager.currentRotation[0] : mc.thePlayer.rotationYaw, yaw);
        }
    }

    private boolean shouldLag() {
        return target != null && smart.get() ? smartCriteria() : simpleCriteria() && mc.thePlayer.canEntityBeSeen(target);
    }

    private boolean attemptingToStrafe() {
        return mc.thePlayer.movementInput.moveStrafe != 0 || RotationUtils.getRotationDifferenceClientRot(target) > 25;
    }

    private boolean isTargetAimingAtServerPos() {
        float calcYawClientPos = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
        float diffXClientPos = Math.abs(MathHelper.wrapAngleTo180_float(calcYawClientPos - target.rotationYaw));
        float calcYawRealPos = (float) (MathHelper.atan2(z - target.posZ, x - target.posX) * 180.0 / Math.PI - 90.0);
        float diffXRealPos = Math.abs(MathHelper.wrapAngleTo180_float(calcYawRealPos - target.rotationYaw));

        return diffXClientPos > 25 && diffXRealPos < 25;
    }

    private boolean smartCriteria() {
        boolean attacked = this.attacked && target.hurtTime <= 3;
        boolean rangeCheck = PlayerUtils.getDistanceToEntityBox(target) <= (alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get());

        if (mc.thePlayer.hurtTime != 0) {
            hurtTimer.reset();
        }

        boolean selfHurtTimeCheck = hurtTimer.hasTimeElapsed(100);
        boolean distanceDiffCheck = mc.thePlayer.getDistanceToEntity(target) < mc.thePlayer.getCustomDistanceToEntity(new Vec3(x, y, z), target);

        //boolean minDistanceCheck = PlayerUtils.getDistanceToEntityBox(target) > 2.5;
        //boolean isTargetAimingAtServerPos1 = !attemptingToStrafe() || diffXClientPos > 25 && diffXRealPos < 25;

        /*
        if (blinking) {
            if (attacked && !isTargetAimingAtServerPos1) {
                attacked = false;
            }
        }
        */

        if (attacked && mc.thePlayer.hurtTime == 0 && PlayerUtils.getDistanceToEntityBox(target) > 2.5 && PlayerUtils.getDistanceToEntityBox(target) <= 3) {
            switch (keepRangeMode.get()) {
                case "WTap":
                    forceWTap = true;
                    break;
                case "Timer down":
                    timerTimer.reset();
                    shouldTimer = true;
                    break;
            }
        }

        if (experimentalDynamicHurtTime.get()) {
            // hurtTime == 10 doesn't add at all,
            // hurtTime == 9 adds twice
            // fuck
            if (mc.thePlayer.hurtTime == 9) {
                targetHits++;
            }

            if (target.hurtTime == 10) {
                targetHits = 0;
            }

            if (PlayerUtils.getDistanceToEntityBox(target) > 4.5) {
                targetHits = 0;
            }

            if (targetHits > 2 && PlayerUtils.getDistToTargetFromMouseOver(target) > 3) {
                return true;
            }
        }

        return !attacked && rangeCheck && selfHurtTimeCheck && distanceDiffCheck; //&& minDistanceCheck;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof C02PacketUseEntity c02) {
            if (c02.getAction() == C02PacketUseEntity.Action.ATTACK) {
                if (target.hurtTime <= 3) {
                    attacked = true;
                }
            }
        }
    }

    @EventTarget
    public void onUpdate1(UpdateEvent e) {
        if (smart.get() && shouldTimer) {
            if (!timerTimer.hasTimeElapsed(timerTicks.get() * 50L)) {
                mc.timer.timerSpeed = timer.get();
            } else {
                mc.timer.timerSpeed = 1;
                shouldTimer = false;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (forceWTap) {
            e.setForward(0);
            forceWTap = false;
        }
    }

    public static float getYaw(EntityPlayer from, Vec3 pos) {
        return from.rotationYaw + MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(pos.zCoord - from.posZ, pos.xCoord - from.posX)) - 90f - from.rotationYaw);
    }

    private boolean simpleCriteria() {
        return mc.thePlayer.hurtTime == 0 && Range.between(attackRange.get(), alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get()).contains((float) PlayerUtils.getDistanceToEntityBox(target));
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (realPos.get() && mc.gameSettings.thirdPersonView != 0) {
            lerpX = MathUtils.interpolate(lerpX, x);
            lerpY = MathUtils.interpolate(lerpY, y);
            lerpZ = MathUtils.interpolate(lerpZ, z);

            double x = lerpX - mc.getRenderManager().viewerPosX;
            double y = lerpY - mc.getRenderManager().viewerPosY;
            double z = lerpZ - mc.getRenderManager().viewerPosZ;

            if (blinking) {
                switch (renderMode.get()) {
                    case "Box":
                        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
                        AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
                        RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(getModule(Interface.class).color(1, 150), true).getRGB());
                        break;
                    case "FakePlayer":
                        GlStateManager.pushMatrix();
                        GL11.glPushAttrib(GL_ALL_ATTRIB_BITS);
                        float lightLevel = mc.theWorld.getLight(new BlockPos(mc.thePlayer.getPositionVector()));
                        GlStateManager.color(lightLevel, lightLevel, lightLevel);
                        mc.getRenderManager().doRenderEntity(mc.thePlayer, x, y, z, mc.thePlayer.rotationYawHead, e.partialTicks(), true, true);
                        GlStateManager.popAttrib();
                        GlStateManager.popMatrix();
                        break;
                }
            }
        }
    }
}