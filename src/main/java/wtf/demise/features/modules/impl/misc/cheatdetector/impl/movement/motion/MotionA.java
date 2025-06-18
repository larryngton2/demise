package wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.motion;

import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.Range;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.utils.player.MoveUtil;

public class MotionA extends Check {

    @Override
    public String getName() {
        return "Motion A";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        if (player.capabilities.isFlying || Range.between(5, 10).contains(player.hurtTime)) {
            return;
        }

        double base = MoveUtil.getBaseMoveSpeed(player);
        double speed = MoveUtil.getSpeed(player);
        if (speed > (base * 1.258f) && MoveUtil.isMoving(player) && player.offGroundTicks > 3 /* to not false on stairs lol */) {
            flag(player, "Moving too fast");
        }

        if (speed >= 1) {
            flag(player, "Moving WAY too fast");
        }
    }
}
