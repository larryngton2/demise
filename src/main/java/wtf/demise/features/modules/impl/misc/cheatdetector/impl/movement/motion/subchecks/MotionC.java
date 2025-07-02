package wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.motion.subchecks;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;

public class MotionC extends Check {

    @Override
    public String getName() {
        return "Motion C";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        if (player != mc.thePlayer) {
            //dfgg
        }
    }
}
