package wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.noslow.NoSlowA;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.noslow.NoSlowB;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NoSlowCheck extends Check {
    private final List<Check> checks = Arrays.asList(new NoSlowA(), new NoSlowB());

    @Override
    public String getName() {
        return "No slow";
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