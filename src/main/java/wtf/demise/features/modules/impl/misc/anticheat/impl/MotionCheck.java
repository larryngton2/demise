package wtf.demise.features.modules.impl.misc.anticheat.impl;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.anticheat.Check;
import wtf.demise.utils.player.MoveUtil;

public class MotionCheck extends Check {

    @Override
    public String getName() {
        return "Invalid motion";
    }

    @Override
    public void onPacketReceive(PacketEvent event, EntityPlayer player) {
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        double base = MoveUtil.getBaseMoveSpeed(player);
        double speed = Math.hypot(player.motionX, player.motionZ);
        if (speed > (base * 1.25f) && player.hurtTime == 0) {
            flag(player, "Too fast");
        }

        if (!player.onGround && !MoveUtil.isMoving(player) && player.motionY == 0.0D && player.offGroundTicks >= 5) {
            flag(player, "Not moving on air for a long time");
        }
    }
}