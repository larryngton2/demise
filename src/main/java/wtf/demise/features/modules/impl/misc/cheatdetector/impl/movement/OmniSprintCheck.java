package wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OmniSprintCheck extends Check {
    private final Map<UUID, Integer> sprintTicksMap = new HashMap<>();

    @Override
    public String getName() {
        return "Omni sprint";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        int sprintTicks = sprintTicksMap.getOrDefault(uuid, 0);

        if ((player.moveForward < 0.0f || player.moveForward == 0.0f && player.moveStrafing != 0.0f) && player.isSprinting()) {
            sprintTicks++;
        } else {
            sprintTicks = 0;
        }

        if (sprintTicks > 2) {
            flag(player, "Sprinting while moving backwards");
        }

        sprintTicksMap.put(uuid, sprintTicks);
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        sprintTicksMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
    }
}