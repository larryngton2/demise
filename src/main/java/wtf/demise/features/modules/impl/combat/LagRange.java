package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.legit.BackTrack;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;

@ModuleInfo(name = "LagRange", description = "Abuses latency in order to simulate tickbasing.")
public class LagRange extends Module {
    private final SliderValue delay = new SliderValue("Delay", 50, 0, 1000, 50, this);
    private final SliderValue tickRange = new SliderValue("Attack range", 3f, 0.1f, 8f, 0.1f, this);
    private final SliderValue stopRange = new SliderValue("Stop range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 7f, 0.1f, 15, 0.1f, this);
    private final SliderValue lagTicks = new SliderValue("Lag ticks", 4, 1, 20, this);
    private final SliderValue maxMs = new SliderValue("Max ms", 300, 100, 1000, 1, this);
    private final SliderValue hurtTimeToStop = new SliderValue("HurtTime to stop (>)", 0, 0, 10, 1, this);
    private final BoolValue pauseOnBacktrack = new BoolValue("Pause on backtrack", false, this);
    private final BoolValue forceFirstHit = new BoolValue("Force first hit", false, this);
    private final BoolValue renderPredictedSelfPos = new BoolValue("Render predicted self pos", false, this);
    private final BoolValue realPos = new BoolValue("Display real pos", true, this);
    private final ModeValue renderMode = new ModeValue("Render mode", new String[]{"Box", "FakePlayer"}, "FakePlayer", this, realPos::get);
    private final BoolValue onlyKillAura = new BoolValue("Only on killAura", false, this);

    public LagRange() {
        delay.setDescription("Delay between lagging again.");
        stopRange.setDescription("Range to not lag at all.");
    }

    private PlayerUtils.PredictProcess selfPrediction;
    private final TimerUtils msTimer = new TimerUtils();
    private final TimerUtils timer = new TimerUtils();
    private EntityLivingBase target;
    private boolean isFirstHit;
    private boolean blinking;
    private double x, y, z;
    private boolean picked;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(String.valueOf(lagTicks.get()));

        target = PlayerUtils.getTarget(searchRange.get());

        if (target == null || (onlyKillAura.get() && !getModule(KillAura.class).isEnabled()) || (pauseOnBacktrack.get() && getModule(BackTrack.class).isEnabled() && BackTrack.shouldLag) || mc.thePlayer.isDead) {
            if (blinking) {
                BlinkComponent.dispatch(true);
                blinking = false;
            }

            picked = false;
            return;
        }

        if (PlayerUtils.getDistanceToEntityBox(target) > 3) {
            isFirstHit = true;
        }

        if (shouldStart()) {
            if (timer.hasTimeElapsed((long) delay.get())) {
                blinking = true;
            }

            if (msTimer.hasTimeElapsed((long) maxMs.get())) {
                if (blinking) {
                    BlinkComponent.dispatch(true);
                    blinking = false;
                }

                picked = false;
                timer.reset();
                msTimer.reset();
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
            if (blinking) {
                BlinkComponent.dispatch(true);
                blinking = false;
            }
            picked = false;
            msTimer.reset();
        }
    }

    @EventTarget
    public void onTick(TickEvent e) {
        if (target != null && forceFirstHit.get() && isFirstHit && !mc.thePlayer.isUsingItem() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            mc.clickMouse();
            ChatUtils.sendMessageClient("forced first hit");
            isFirstHit = false;
        }
    }

    private boolean shouldStart() {
        double predictedTargetDistance = PlayerUtils.getCustomDistanceToEntityBox(PlayerUtils.getPosFromAABB(target.getHitbox()).add(0, target.getEyeHeight(), 0), mc.thePlayer);
        double predictedSelfDistance = PlayerUtils.getCustomDistanceToEntityBox(selfPrediction.position.add(0, mc.thePlayer.getEyeHeight(), 0), target);

        return predictedSelfDistance < predictedTargetDistance &&
                predictedSelfDistance <= tickRange.get() &&
                predictedSelfDistance <= searchRange.get() &&
                PlayerUtils.getDistanceToEntityBox(target) >= stopRange.get() &&
                mc.thePlayer.canEntityBeSeen(target) &&
                target.canEntityBeSeen(mc.thePlayer) &&
                !selfPrediction.isCollidedHorizontally &&
                !mc.thePlayer.isCollidedHorizontally &&
                mc.thePlayer.hurtTime <= hurtTimeToStop.get();
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (renderPredictedSelfPos.get() && mc.gameSettings.thirdPersonView != 0) {
            double x = selfPrediction.position.xCoord - mc.getRenderManager().viewerPosX;
            double y = selfPrediction.position.yCoord - mc.getRenderManager().viewerPosY;
            double z = selfPrediction.position.zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1, 100), true).getRGB());
        }

        if (realPos.get() && mc.gameSettings.thirdPersonView != 0) {
            double x = this.x - mc.getRenderManager().viewerPosX;
            double y = this.y - mc.getRenderManager().viewerPosY;
            double z = this.z - mc.getRenderManager().viewerPosZ;

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

    @EventTarget
    public void onMove(MoveEvent e) {
        selfPrediction = PlayerUtils.predictPlayerPosition((int) lagTicks.get());
    }
}