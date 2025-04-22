package wtf.demise.features.modules.impl.movement;

import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "Freeze", category = ModuleCategory.Movement)
public class Freeze extends Module {
    private double motionX;
    private double motionY;
    private double motionZ;
    private double x;
    private double y;
    private double z;

    @Override
    public void onEnable() {
        x = mc.thePlayer.posX;
        y = mc.thePlayer.posY;
        z = mc.thePlayer.posZ;
        motionX = mc.thePlayer.motionX;
        motionY = mc.thePlayer.motionY;
        motionZ = mc.thePlayer.motionZ;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        mc.thePlayer.motionX = 0.0;
        mc.thePlayer.motionY = 0.0;
        mc.thePlayer.motionZ = 0.0;
        mc.thePlayer.setPositionAndRotation(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof C03PacketPlayer) {
            e.setCancelled(true);
        }

        if (e.getPacket() instanceof S08PacketPlayerPosLook s08) {
            x = s08.getX();
            y = s08.getY();
            z = s08.getZ();
            motionX = 0.0;
            motionY = 0.0;
            motionZ = 0.0;
        }
    }

    @Override
    public void onDisable() {
        mc.thePlayer.motionX = motionX;
        mc.thePlayer.motionY = motionY;
        mc.thePlayer.motionZ = motionZ;
        mc.thePlayer.setPositionAndRotation(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }
}