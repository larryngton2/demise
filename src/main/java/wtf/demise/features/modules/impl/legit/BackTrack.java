package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ColorValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.animations.ContinualAnimation;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;

@ModuleInfo(name = "BackTrack", description = "Abuses latency for increased reach. (BACKTRACK???)", category = ModuleCategory.Legit)
public class BackTrack extends Module {
    private final BoolValue onlyWhenNeeded = new BoolValue("Only when needed", false, this);
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 0.1f, 8, 0.1f, this, onlyWhenNeeded::get);
    private final SliderValue minRange = new SliderValue("Min range", 3, 1, 8, 0.1f, this, () -> !onlyWhenNeeded.get());
    private final SliderValue maxRange = new SliderValue("Max range", 6, 1, 8, 0.1f, this, () -> !onlyWhenNeeded.get());
    private final BoolValue onlyDouble = new BoolValue("Only double", true, this, onlyWhenNeeded::get);
    private final BoolValue extraCheck = new BoolValue("Extra check", true, this);
    private final SliderValue ms = new SliderValue("Delay ms", 50, 0, 5000, 5, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final ModeValue esp = new ModeValue("Mode", new String[]{"Off", "Box", "FakePlayer"}, "Box", this);
    private final ColorValue color = new ColorValue("Color", new Color(0, 0, 0, 100), this, () -> esp.is("Box"));

    private EntityPlayer target;
    public static Vec3 realPosition = new Vec3(0, 0, 0);
    public static Vec3 realLastPos = new Vec3(0, 0, 0);
    private final ContinualAnimation animatedX = new ContinualAnimation();
    private final ContinualAnimation animatedY = new ContinualAnimation();
    private final ContinualAnimation animatedZ = new ContinualAnimation();
    public static boolean shouldLag;
    private boolean dispatched;
    private boolean outOfRange;

    @Override
    public void onDisable() {
        PingSpoofComponent.disable();
        PingSpoofComponent.dispatch();
        shouldLag = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag((int) ms.get() + " ms");
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPost()) {
            if (mc.thePlayer.isDead) return;

            target = PlayerUtils.getTarget(8, teamCheck.get());

            if (target == null) return;

            double realDistance = PlayerUtils.getCustomDistanceToEntityBox(realPosition, mc.thePlayer);
            double clientDistance = PlayerUtils.getDistToTargetFromMouseOver(target);

            if (clientDistance > attackRange.get() && target.hurtTime != 0) {
                outOfRange = true;
            }

            if ((target.hurtTime == 10 && onlyDouble.get()) || (realDistance < 3 && !onlyDouble.get())) {
                outOfRange = false;
            }

            boolean distanceCheck = PlayerUtils.getCustomDistanceToEntityBox(target.getPositionVector(), mc.thePlayer) >= PlayerUtils.getCustomDistanceToEntityBox(target.getPrevPositionVector(), mc.thePlayer);
            boolean extraCheck = distanceCheck || !this.extraCheck.get();
            boolean onlyNeeded = extraCheck && (realDistance > attackRange.get() || outOfRange) && realDistance < attackRange.get() + 1.5 && clientDistance <= attackRange.get();
            boolean on = extraCheck && realDistance > minRange.get() && realDistance < maxRange.get();

            shouldLag = onlyWhenNeeded.get() ? onlyNeeded : on;

            if (shouldLag) {
                if (realDistance > 3 && target.hurtTime == 10) {
                    ChatUtils.sendMessageClient("Attacked from " + realDistance + " blocks");
                }

                PingSpoofComponent.spoof((int) ms.get(), true, true, true, true, false, false);
                dispatched = false;
            } else {
                if (!dispatched) {
                    PingSpoofComponent.disable();
                    PingSpoofComponent.dispatch();
                    dispatched = true;
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            realLastPos = realPosition;

            if (e.getPacket() instanceof S14PacketEntity s14PacketEntity) {
                if (s14PacketEntity.getEntityId() == target.getEntityId()) {
                    realPosition = realPosition.addVector(s14PacketEntity.getX() / 32.0D, s14PacketEntity.getY() / 32.0D, s14PacketEntity.getZ() / 32.0D);
                }
            } else if (e.getPacket() instanceof S18PacketEntityTeleport s18PacketEntityTeleport) {
                if (s18PacketEntityTeleport.getEntityId() == target.getEntityId()) {
                    realPosition = new Vec3(s18PacketEntityTeleport.getX() / 32D, s18PacketEntityTeleport.getY() / 32D, s18PacketEntityTeleport.getZ() / 32D);
                }

                if (s18PacketEntityTeleport.getEntityId() == mc.thePlayer.getEntityId()) {
                    dispatched = false;
                    shouldLag = false;
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (target != null) {
            double x = realPosition.xCoord - mc.getRenderManager().viewerPosX;
            double y = realPosition.yCoord - mc.getRenderManager().viewerPosY;
            double z = realPosition.zCoord - mc.getRenderManager().viewerPosZ;

            switch (esp.get()) {
                case "Box": {
                    animatedX.animate((float) x, 20);
                    animatedY.animate((float) y, 20);
                    animatedZ.animate((float) z, 20);

                    AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
                    AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + animatedX.getOutput(), box.minY - mc.thePlayer.posY + animatedY.getOutput(), box.minZ - mc.thePlayer.posZ + animatedZ.getOutput(), box.maxX - mc.thePlayer.posX + animatedX.getOutput(), box.maxY - mc.thePlayer.posY + animatedY.getOutput(), box.maxZ - mc.thePlayer.posZ + animatedZ.getOutput());

                    if (shouldLag) {
                        RenderUtils.drawAxisAlignedBB(axis, true, false, color.get().getRGB());
                    }
                }
                break;
                case "FakePlayer": {
                    animatedX.animate((float) x, 20);
                    animatedY.animate((float) y, 20);
                    animatedZ.animate((float) z, 20);

                    if (shouldLag) {
                        GlStateManager.pushMatrix();
                        GL11.glPushAttrib(GL_ALL_ATTRIB_BITS);
                        GlStateManager.color(0.6f, 0.6f, 0.6f);
                        mc.getRenderManager().doRenderEntity(target, animatedX.getOutput(), animatedY.getOutput(), animatedZ.getOutput(), target.rotationYawHead, e.partialTicks(), true);
                        GlStateManager.popAttrib();
                        GlStateManager.popMatrix();
                    }
                }
                break;
            }
        }
    }
}
