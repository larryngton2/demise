package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.*;
import org.apache.commons.lang3.Range;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.packet.PacketReleaseEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.legit.BackTrack;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimedVec3;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.packet.LagUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;

@ModuleInfo(name = "FakeLag", description = "Abuses latency in order to be unpredictable to your target.")
public class FakeLag extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Pulse", "Spoof"}, "Pulse", this);
    private final BoolValue smart = new BoolValue("Smart", true, this);
    private final BoolValue resetInRange = new BoolValue("Reset in range", true,this, smart::get);
    private final SliderValue hurtTimeToAttack = new SliderValue("HurtTime to register attack", 3, 0, 10, 1, this, smart::get);
    private final BoolValue earlyRelease = new BoolValue("Early release", false, this, smart::get);
    private final SliderValue attackRange = new SliderValue("Attack range", 4, 0, 15, 0.1f, this, () -> !smart.get());
    private final BoolValue alwaysSpoof = new BoolValue("Always spoof", false, this);
    private final SliderValue searchRange = new SliderValue("Search range", 6, 1, 15, 0.1f, this, () -> !alwaysSpoof.get());
    private final SliderValue recoilTime = new SliderValue("Recoil time (ms)", 50, 0, 1000, this);
    private final SliderValue delayMin = new SliderValue("Delay (min ms)", 100, 0, 1000, this);
    private final SliderValue delayMax = new SliderValue("Delay (max ms)", 100, 0, 1000, this);
    private final SliderValue hurtTimeToStop = new SliderValue("HurtTime to stop (>)", 0, 0, 10, 1, this);
    private final BoolValue pauseOnBacktrack = new BoolValue("Pause on backtrack", false, this);
    private final BoolValue forceFirstHit = new BoolValue("Force first hit", false, this);
    private final BoolValue realPos = new BoolValue("Display real pos", true, this);
    private final ModeValue renderMode = new ModeValue("Render mode", new String[]{"Box", "FakePlayer", "Line"}, "FakePlayer", this, realPos::get);
    private final BoolValue onlyOnGround = new BoolValue("Only on ground", false, this);
    private final BoolValue onlyKillAura = new BoolValue("Only on killAura", false, this);

    public FakeLag() {
        mode.setDescription("Pulse: simulates packet loss, Spoof: simulates high ping.");
        hurtTimeToAttack.setDescription("An attack will be registered only if the target's hurt time is bellow this value.");
        earlyRelease.setDescription("Only use on bad anticheats. Teleports you to the best past position.");
        alwaysSpoof.setDescription("Makes FakeLag ignore search range.");
        recoilTime.setDescription("Delay to lag again after stopping.");
    }

    public static boolean blinking = false, picked = false;
    private final TimerUtils delay = new TimerUtils();
    private final TimerUtils recoilTimer = new TimerUtils();
    private static double x, y, z;
    private double lerpX, lerpY, lerpZ;
    public EntityLivingBase target;
    private int ms;
    private boolean attacked;
    private boolean dispatched;
    private final TimerUtils hurtTimer = new TimerUtils();
    private boolean forceWTap;
    private boolean isFirstHit = true;
    private boolean isPreTick;
    private final List<TimedVec3> path = new ArrayList<>();

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
                LagUtils.disable();
                LagUtils.dispatch();
                break;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(ms + " ms");

        target = PlayerUtils.getTarget(alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get() + 1);

        if (ms == 0) ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());

        if (target == null || (onlyOnGround.get() && !mc.thePlayer.onGround) || (onlyKillAura.get() && !getModule(KillAura.class).isEnabled()) || (pauseOnBacktrack.get() && getModule(BackTrack.class).isEnabled() && BackTrack.shouldLag) || mc.thePlayer.isDead) {
            if (blinking) {
                switch (mode.get()) {
                    case "Pulse":
                        BlinkComponent.dispatch(true);
                        break;
                    case "Spoof":
                        LagUtils.disable();
                        LagUtils.dispatch();
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
                        ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());

                        LagUtils.spoof(ms, true, false, false, false, true, true, false);
                        blinking = true;
                        dispatched = false;
                    } else {
                        blinking = false;
                    }
                } else {
                    if (!dispatched) {
                        LagUtils.disable();
                        LagUtils.dispatch();
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
        if (target != null && isFirstHit && !mc.thePlayer.isUsingItem() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            if (forceFirstHit.get()) {
                mc.clickMouse();
                ChatUtils.sendMessageClient("forced first hit");
            }

            if (attacked) {
                isFirstHit = false;
            }
        }
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent e) {
        isPreTick = e.getState() == PlayerTickEvent.State.PRE;
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (e.getTargetEntity() == target) {
            attacked = true;
        }
    }

    @EventTarget
    public void onPacketRelease(PacketReleaseEvent e) {
        if (e.getTimedPacket().getPacket() instanceof C03PacketPlayer c03) {
            x = c03.getPositionX();
            y = c03.getPositionY();
            z = c03.getPositionZ();

            //flags, only for testing
            if (shouldLag() && blinking && PlayerUtils.getCustomDistanceToEntityBox(new Vec3(c03.getPositionX(), c03.getPositionY() + mc.thePlayer.getEyeHeight(), c03.getPositionZ()), target) <= 3.0 && smart.get() && earlyRelease.get()) {
                mc.thePlayer.setPosition(c03.getPositionX(), c03.getPositionY(), c03.getPositionZ());

                if (isPreTick) {
                    ChatUtils.sendMessageClient("released early");

                    switch (mode.get()) {
                        case "Pulse":
                            BlinkComponent.dispatch(false);
                            break;
                        case "Spoof":
                            LagUtils.disable();
                            LagUtils.packets.clear();
                            break;
                    }
                    blinking = false;
                }
            }
        }
    }

    private boolean shouldLag() {
        return target != null && smart.get() ? smartCriteria() : simpleCriteria() && target.canEntityBeSeen(mc.thePlayer);
    }

    private boolean smartCriteria() {
        boolean attacked = this.attacked && target.hurtTime <= hurtTimeToAttack.get();
        boolean rangeCheck = PlayerUtils.getDistanceToEntityBox(target) <= (alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get());

        if (mc.thePlayer.hurtTime > hurtTimeToStop.get()) {
            hurtTimer.reset();
        }

        boolean selfHurtTimeCheck = hurtTimer.hasTimeElapsed(100);
        boolean distanceDiffCheck = mc.thePlayer.getDistanceToEntity(target) < mc.thePlayer.getCustomDistanceToEntity(new Vec3(x, y, z), target);

        if (!resetInRange.get()) {
            return isFirstHit && selfHurtTimeCheck && distanceDiffCheck;
        }

        return !attacked && rangeCheck && selfHurtTimeCheck && distanceDiffCheck;
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
        return mc.thePlayer.hurtTime <= hurtTimeToStop.get() && Range.between(attackRange.get(), alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get()).contains((float) PlayerUtils.getDistanceToEntityBox(target));
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (blinking) {
            if (realPos.get() && renderMode.is("Line")) {
                if (mc.thePlayer.lastTickPosX != mc.thePlayer.posX || mc.thePlayer.lastTickPosY != mc.thePlayer.posY || mc.thePlayer.lastTickPosZ != mc.thePlayer.posZ) {
                    path.add(new TimedVec3(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ), System.currentTimeMillis()));
                }
            }

            long currentTime = System.currentTimeMillis();
            path.removeIf(timedVec -> currentTime - timedVec.time > (ms));
        } else {
            path.clear();
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (realPos.get() && mc.gameSettings.thirdPersonView != 0) {
            if (!renderMode.is("Line")) {
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
            } else {
                if (blinking) {
                    GlStateManager.pushMatrix();
                    GlStateManager.disableDepth();
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glEnable(GL11.GL_LINE_SMOOTH);

                    GL11.glLineWidth(2.0f);

                    long currentTime = System.currentTimeMillis();

                    GL11.glBegin(GL11.GL_LINE_STRIP);
                    for (int i = 0; i < path.size(); i++) {
                        TimedVec3 timedVec = path.get(i);
                        Vec3 v = timedVec.vec;

                        double x = v.xCoord - mc.getRenderManager().renderPosX;
                        double y = v.yCoord - mc.getRenderManager().renderPosY;
                        double z = v.zCoord - mc.getRenderManager().renderPosZ;

                        float alpha = 1.0f - (float) (currentTime - timedVec.time) / ms;
                        alpha = Math.max(0.0f, Math.min(1.0f, alpha));

                        Color color = new Color(getModule(Interface.class).color(i));
                        GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, alpha);

                        GL11.glVertex3d(x, y, z);
                    }
                    GL11.glEnd();

                    GL11.glDisable(GL11.GL_LINE_SMOOTH);
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                    GlStateManager.enableDepth();
                    GlStateManager.popMatrix();
                }
            }
        }
    }
}