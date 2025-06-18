package wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InvalidInteract extends Check {
    private final Map<UUID, Integer> useTimeMap = new HashMap<>();

    @Override
    public String getName() {
        return "Invalid interact";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        int useTime = useTimeMap.getOrDefault(uuid, 0);

        if (player.isUsingItem()) {
            useTime++;
        } else {
            useTime = 0;
        }

        useTimeMap.put(uuid, useTime);

        if (useTime > 2 && player.swingProgressInt == 0 && player.isSwingInProgress) {
            flag(player, "Swinging while using an item");
        }
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        useTimeMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
    }
}