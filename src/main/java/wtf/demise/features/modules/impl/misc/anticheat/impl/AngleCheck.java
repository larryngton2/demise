package wtf.demise.features.modules.impl.misc.anticheat.impl;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.anticheat.Check;

public class AngleCheck extends Check {

    @Override
    public void onUpdate(EntityPlayer player) {
        if (Math.abs(player.rotationYaw - player.prevRotationYaw) > 50 && player.swingProgress != 0F) {
            flag(player, "Too fast rotate speed");
        }

        if (player.rotationPitch > 90 || player.rotationPitch < -90) {
            flag(player, "Invalid rotation pitch");
        }
    }

    @Override
    public String getName() {
        return "Angle";
    }

    @Override
    public void onPacketReceive(PacketEvent event, EntityPlayer player) {
    }
}