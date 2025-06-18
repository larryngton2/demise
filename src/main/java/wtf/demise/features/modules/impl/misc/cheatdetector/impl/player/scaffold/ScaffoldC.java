package wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.scaffold;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.utils.math.TimerUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.ScaffoldCheck.blocksPlacedMap;

public class ScaffoldC extends Check {
    private final Map<UUID, TimerUtils> bridgingTimerMap = new HashMap<>();
    private final TimerUtils flagTimer = new TimerUtils();

    @Override
    public String getName() {
        return "Scaffold C";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        int blocksPlaced = blocksPlacedMap.getOrDefault(uuid, 0);
        TimerUtils bridgingTimer = bridgingTimerMap.getOrDefault(uuid, new TimerUtils());

        boolean bridgingCheck = player.rotationPitchHead > 70 && player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemBlock;

        if (bridgingCheck) {
            bridgingTimer.reset();
        }

        // to make sure you don't false when not placing blocks
        if (bridgingTimer.hasTimeElapsed(1000L)) {
            blocksPlaced = 0;
            bridgingTimer.reset();
        }

        // might false for breezily and shit like that, but I don't care
        if (blocksPlaced > 7 && flagTimer.hasTimeElapsed(500)) {
            flag(player, "Suspicious block placement");
            flagTimer.reset();
        }
        bridgingTimerMap.put(uuid, bridgingTimer);
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        bridgingTimerMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
        bridgingTimerMap.values().removeIf(timerUtils -> timerUtils.hasTimeElapsed(1000L));
    }
}
