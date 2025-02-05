package wtf.demise.features.modules.impl.misc.anticheat.impl;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.anticheat.Check;

public class OmniSprintCheck extends Check {
    @Override
    public String getName() {
        return "OmniSprint";
    }

    @Override
    public void onPacketReceive(PacketEvent event, EntityPlayer player) {
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        if (player.isSprinting() && (player.moveForward < 0.0f || player.moveForward == 0.0f && player.moveStrafing != 0.0f)) {
            flag(player, "Sprinting when moving backward");
        }
    }
}