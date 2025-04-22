package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.BlockAABBEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "FreeCam", category = ModuleCategory.Visual)
public class FreeCam extends Module {
    private final BoolValue fly = new BoolValue("Fly", true, this);
    private final SliderValue flySpeed = new SliderValue("Fly Speed", 1, 0.1f, 9.5f, 0.1f, this, fly::get);
    private final BoolValue noClip = new BoolValue("No clip", true, this, fly::get);

    private EntityOtherPlayerMP freecamEntity;
    public static double startX, startY, startZ;
    private float startYaw, startPitch;

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (!e.isPre()) {
            return;
        }

        if (mc.thePlayer.ticksExisted < 5) {
            this.toggle();
            return;
        }

        if (noClip.get() && fly.get())
            mc.thePlayer.noClip = true;

        if (fly.get()) {
            mc.thePlayer.motionY = mc.gameSettings.keyBindJump.isKeyDown() ? flySpeed.get() : mc.gameSettings.keyBindSneak.isKeyDown() ? -flySpeed.get() : 0;

            if (MoveUtil.isMoving()) {
                MoveUtil.strafe(flySpeed.get());
            } else {
                MoveUtil.stopXZ();
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        final Packet<?> p = e.getPacket();

        if (e.getState() == PacketEvent.State.OUTGOING) {
            if (!(p instanceof C01PacketChatMessage || p instanceof C08PacketPlayerBlockPlacement || p instanceof C0FPacketConfirmTransaction || p instanceof C00PacketKeepAlive || p instanceof C09PacketHeldItemChange || p instanceof C12PacketUpdateSign || p instanceof C10PacketCreativeInventoryAction || p instanceof C0EPacketClickWindow || p instanceof C0DPacketCloseWindow || p instanceof C16PacketClientStatus || p instanceof C0APacketAnimation || p instanceof C02PacketUseEntity))
                e.setCancelled(true);
        } else {
            if (p instanceof S08PacketPlayerPosLook && mc.thePlayer.ticksExisted > 1)
                e.setCancelled(true);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (fly.get()) {
            if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                mc.thePlayer.setSneaking(false);
            }
        }
    }

    @EventTarget
    public void onBlockCollide(BlockAABBEvent event) {
        if (noClip.get() && fly.get()) event.setCancelled(true);
    }

    @Override
    public void onEnable() {
        freecamEntity = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        freecamEntity.setPositionAndRotation(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        freecamEntity.rotationYawHead = mc.thePlayer.rotationYawHead;
        freecamEntity.setSprinting(mc.thePlayer.isSprinting());
        freecamEntity.setInvisible(mc.thePlayer.isInvisible());
        freecamEntity.setSneaking(mc.thePlayer.isSneaking());

        mc.theWorld.addEntityToWorld(freecamEntity.getEntityId(), freecamEntity);

        startPitch = mc.thePlayer.rotationPitch;
        startYaw = mc.thePlayer.rotationYaw;
        startX = mc.thePlayer.posX;
        startY = mc.thePlayer.posY;
        startZ = mc.thePlayer.posZ;
    }

    @Override
    public void onDisable() {
        if (freecamEntity != null) {
            mc.theWorld.removeEntityFromWorld(freecamEntity.getEntityId());
            mc.thePlayer.setPositionAndRotation(startX, startY, startZ, startYaw, startPitch);
        }
        mc.thePlayer.noClip = false;
        mc.thePlayer.motionY = 0;
        MoveUtil.strafe(0);
    }
}
