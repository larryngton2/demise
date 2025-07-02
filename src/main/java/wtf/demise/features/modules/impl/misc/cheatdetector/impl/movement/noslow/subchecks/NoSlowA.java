package wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.noslow.subchecks;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NoSlowA extends Check {
    private final Map<UUID, Integer> sprintBufferMap = new HashMap<>();

    @Override
    public String getName() {
        return "NoSlow A";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        int sprintBuffer = sprintBufferMap.getOrDefault(player.getUniqueID(), 0);

        if (player.isUsingItem() && player.hurtTime == 0) {
            if (player.isSprinting()) {
                sprintBuffer++;

                if (sprintBuffer > 5) {
                    flag(player, "Sprinting while using an item");
                    sprintBuffer = 0;
                }
            }

            sprintBufferMap.put(uuid, sprintBuffer);
        }
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        sprintBufferMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
    }
}