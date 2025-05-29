package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.Range;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketReleaseEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
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
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;

@ModuleInfo(name = "FakeLag", description = "Abuses latency in order to be unpredictable to your target.", category = ModuleCategory.Combat)
public class FakeLag extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Pulse", "Spoof"}, "Pulse", this);
    private final BoolValue smart = new BoolValue("Smart", true, this);
    private final BoolValue modifyMovementYaw = new BoolValue("Modify movement yaw", true, this, smart::get);
    private final SliderValue attackRange = new SliderValue("Attack range", 4, 0, 15, 0.1f, this, () -> !smart.get());
    private final BoolValue alwaysSpoof = new BoolValue("Always spoof", false, this);
    private final SliderValue searchRange = new SliderValue("Search range", 6, 1, 15, 0.1f, this, () -> !alwaysSpoof.get());
    private final SliderValue recoilTime = new SliderValue("Recoil time (ms)", 50, 0, 1000, this);
    private final SliderValue delayMin = new SliderValue("Delay (min ms)", 100, 0, 1000, this);
    private final SliderValue delayMax = new SliderValue("Delay (max ms)", 250, 1, 1000, this);
    private final SliderValue criticalPing = new SliderValue("Critical Ping", 100, 0, 500, this);
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

    @Override
    public void onEnable() {
        blinking = false;
        picked = false;
        x = y = z = lerpX = lerpY = lerpZ = 0;
        target = null;

        delay.reset();
        recoilTimer.reset();
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

        if ((onlyOnGround.get() && !mc.thePlayer.onGround) || (onlyKillAura.get() && !getModule(KillAura.class).isEnabled())) {
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
                    ms = calculateDynamicDelay();
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
                        ms = calculateDynamicDelay();
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
    public void onPacketRelease(PacketReleaseEvent e) {
        if (e.getTimedPacket().getPacket() instanceof C03PacketPlayer c03) {
            x = c03.getPositionX();
            y = c03.getPositionY();
            z = c03.getPositionZ();

            /*
            if (attacked) {
                e.setCancelled(true);

                //kinda shit but ok
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {}

                    ChatUtils.sendMessageClient("did the αστείο");
                    PacketUtils.queue(c03);
                    PingSpoofComponent.packets.remove(e.getTimedPacket());
                }).start();
            }
            */
        }
    }

    @EventPriority(-100)
    @EventTarget
    public void onMove(MoveInputEvent e) {
        if (smart.get() && shouldLag() && attemptingToStrafe() && modifyMovementYaw.get()) {
            MoveUtil.fixMovement(e, RotationUtils.shouldRotate() ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw, yaw);
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (e.getTargetEntity() != null && mc.objectMouseOver.entityHit == e.getTargetEntity()) {
            attacked = true;
        }
    }

    private boolean shouldLag() {
        return target != null && smart.get() ? smartCriteria() : simpleCriteria() && mc.thePlayer.canEntityBeSeen(target);
    }

    private boolean attemptingToStrafe() {
        return mc.thePlayer.movementInput.moveStrafe != 0 || RotationUtils.getRotationDifferenceClientRot(target) > 25;
    }

    private boolean smartCriteria() {
        float calcYawClientPos = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
        float diffXClientPos = Math.abs(MathHelper.wrapAngleTo180_float(calcYawClientPos - target.rotationYaw));
        float calcYawRealPos = (float) (MathHelper.atan2(z - target.posZ, x - target.posX) * 180.0 / Math.PI - 90.0);
        float diffXRealPos = Math.abs(MathHelper.wrapAngleTo180_float(calcYawRealPos - target.rotationYaw));

        boolean attacked = this.attacked && target.hurtTime <= 3;
        boolean rangeCheck = PlayerUtils.getDistanceToEntityBox(target) <= (alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get());

        if (mc.thePlayer.hurtTime != 0) {
            hurtTimer.reset();
        }

        boolean selfHurtTimeCheck = hurtTimer.hasTimeElapsed(100);

        //boolean minDistanceCheck = PlayerUtils.getDistanceToEntityBox(target) > 2.5;
        boolean isTargetAimingAtServerPos = !attemptingToStrafe() || diffXClientPos > 25 && diffXRealPos < 25;

        /*
        if (blinking) {
            if (attacked && !isTargetAimingAtServerPos) {
                attacked = false;
            }
        }
        */

        if (attacked && !forceWTap && mc.thePlayer.hurtTime == 0 && PlayerUtils.getDistanceToEntityBox(target) > 2.5) {
            forceWTap = true;
        }

        return !attacked && rangeCheck && selfHurtTimeCheck; //&& minDistanceCheck;
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

    private int calculateDynamicDelay() {
        if (target.swingProgress > 0 && PlayerUtils.getDistanceToEntityBox(target) < 3) {
            return (int) (criticalPing.get() * 0.75);
        }
        return MathUtils.randomizeInt(delayMin.get(), delayMax.get());
    }
}