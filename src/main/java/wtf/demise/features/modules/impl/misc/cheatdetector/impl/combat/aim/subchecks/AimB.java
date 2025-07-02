package wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat.aim.subchecks;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;

public class AimB extends Check {

    @Override
    public String getName() {
        return "Aim B";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        if (player.rotationPitchHead > 90 || player.rotationPitchHead < -90) {
            flag(player, "Invalid pitch");
        }
    }
}
