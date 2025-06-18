package wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat.aim.AimA;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat.aim.AimB;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AimCheck extends Check {
    private final List<Check> checks = Arrays.asList(new AimA(), new AimB());

    @Override
    public String getName() {
        return "Aim";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        checks.forEach(check -> check.onUpdate(player));
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        checks.forEach(check -> check.cleanup(onlineUUIDs));
    }
}
