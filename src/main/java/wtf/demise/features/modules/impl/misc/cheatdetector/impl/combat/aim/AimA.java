package wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat.aim;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;

public class AimA extends Check {

    @Override
    public String getName() {
        return "Aim A";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        if (Math.abs(player.rotationYawHead - player.prevRotationYawHead) > 175 && player.swingProgress != 0) {
            flag(player, "Impossible yaw change");
        }
    }
}
