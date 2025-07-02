package wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.motion.subchecks;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;

public class MotionA extends Check {

    @Override
    public String getName() {
        return "Motion A";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        /*
        if (player.capabilities.isFlying || player.hurtTime != 0) {
            return;
        }

        double base = MoveUtil.getBaseMoveSpeed(player);
        double speed = MoveUtil.getSpeed(player);

        if (MoveUtil.isMovingMotion(player) && player.prevPosX != player.posX || player.prevPosZ != player.posZ) {
            if (speed >= 1) {
                flag(player, "Moving WAY too fast");
                return;
            }

            if (speed > (base * 1.258f) && player.offGroundTicks > 3) {
                flag(player, "Moving too fast");
            }
        }
        */
    }
}
