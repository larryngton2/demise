package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
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
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.packet.BadPacketsComponent;
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

@ModuleInfo(name = "BackTrack", description = "Abuses latency for increased reach.", category = ModuleCategory.Legit)
public class BackTrack extends Module {
    private final BoolValue onlyWhenNeeded = new BoolValue("Only when needed", false, this);
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 0.1f, 8, 0.1f, this);
    private final SliderValue minRange = new SliderValue("Min range", 3, 1, 8, 0.1f, this, () -> !onlyWhenNeeded.get());
    private final SliderValue maxRange = new SliderValue("Max range", 6, 1, 8, 0.1f, this, () -> !onlyWhenNeeded.get());
    private final BoolValue extraCheck = new BoolValue("Extra check", true, this);
    private final SliderValue minMS = new SliderValue("Min ms", 50, 0, 5000, 5, this);
    private final SliderValue maxMS = new SliderValue("Max ms", 200, 0, 5000, 5, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final ModeValue esp = new ModeValue("Mode", new String[]{"Off", "Box", "FakePlayer"}, "Box", this);
    private final ColorValue color = new ColorValue("Color", new Color(0, 0, 0, 100), this, () -> esp.is("Box"));

    private EntityPlayer target;
    public static Vec3 realPosition = new Vec3(0, 0, 0);
    public static Vec3 realLastPos = new Vec3(0, 0, 0);
    private final ContinualAnimation animatedX = new ContinualAnimation();
    private final ContinualAnimation animatedY = new ContinualAnimation();
    private final ContinualAnimation animatedZ = new ContinualAnimation();
    private int ping;
    public static boolean shouldLag;
    private EntityOtherPlayerMP fakePlayer;
    private boolean addedEntity;
    private boolean dispatched;
    private boolean outOfRange;

    @Override
    public void onDisable() {
        PingSpoofComponent.disable();
        PingSpoofComponent.dispatch();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(ping + " ms");

        if (target != null && esp.is("FakePlayer")) {
            fakePlayer.setPositionAndRotation(realPosition.xCoord, realPosition.yCoord, realPosition.zCoord, target.rotationYaw, target.rotationPitch);
            fakePlayer.rotationYawHead = target.rotationYawHead;
            fakePlayer.setSprinting(target.isSprinting());
            fakePlayer.setInvisible(false);
            fakePlayer.setSneaking(target.isSneaking());
            fakePlayer.renderYawOffset = target.renderYawOffset;
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            if (mc.thePlayer.isDead) return;

            target = PlayerUtils.getTarget(8, teamCheck.get());

            if (ping == 0) ping = MathUtils.randomizeInt(minMS.get(), maxMS.get());

            if (target == null) return;

            double realDistance = PlayerUtils.getCustomDistanceToEntityBox(realPosition, mc.thePlayer);
            double clientDistance = PlayerUtils.getDistanceToEntityBox(target);

            if (clientDistance > attackRange.get() && target.hurtTime != 0) {
                outOfRange = true;
            }

            if (target.hurtTime == 10) {
                outOfRange = false;
            }

            boolean distanceCheck = PlayerUtils.getCustomDistanceToEntityBox(target.getPositionVector(), mc.thePlayer) > PlayerUtils.getCustomDistanceToEntityBox(target.getPrevPositionVector(), mc.thePlayer);
            boolean extraCheck = distanceCheck || !this.extraCheck.get();
            boolean onlyNeeded = extraCheck && (realDistance > attackRange.get() || outOfRange) && realDistance < attackRange.get() + 1.5 && clientDistance <= attackRange.get();
            boolean on = extraCheck && realDistance > minRange.get() && realDistance < maxRange.get();

            if (shouldLag) {
                ping = MathUtils.randomizeInt(minMS.get(), maxMS.get());
                PingSpoofComponent.spoof(ping, true, true, true, true, false, false);
                dispatched = false;
            } else {
                if (!dispatched) {
                    PingSpoofComponent.disable();
                    PingSpoofComponent.dispatch();
                    dispatched = true;
                }
            }

            shouldLag = onlyWhenNeeded.get() ? onlyNeeded : on;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            if (target == null) {
                return;
            }

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
            switch (esp.get()) {
                case "Box": {
                    double x = realPosition.xCoord - mc.getRenderManager().viewerPosX;
                    double y = realPosition.yCoord - mc.getRenderManager().viewerPosY;
                    double z = realPosition.zCoord - mc.getRenderManager().viewerPosZ;

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
                    if (shouldLag) {
                        if (!addedEntity) {
                            fakePlayer = new EntityOtherPlayerMP(mc.theWorld, target.getGameProfile());
                            mc.theWorld.addEntityToWorld(fakePlayer.getEntityId(), fakePlayer);
                            addedEntity = true;
                        }
                    } else {
                        if (addedEntity) {
                            mc.theWorld.removeEntityFromWorld(fakePlayer.getEntityId());
                            addedEntity = false;
                        }
                    }
                }
                break;
            }
        }
    }
}
