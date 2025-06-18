package wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.motion;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.utils.player.MoveUtil;

public class MotionB extends Check {

    @Override
    public String getName() {
        return "Motion B";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        if (player.capabilities.isFlying) {
            return;
        }

        if (player.motionY == 0 && !player.onGround && player.offGroundTicks > 5 && MoveUtil.isMovingMotion(player)) {
            flag(player, "Ignoring gravity");
        }
    }
}
