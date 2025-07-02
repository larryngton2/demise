package wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.scaffold.subchecks;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.utils.player.MoveUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.scaffold.ScaffoldCheck.blocksPlacedMap;

public class ScaffoldB extends Check {

    @Override
    public String getName() {
        return "Scaffold B";
    }

    private final Map<UUID, Float> cachedPitchMap = new HashMap<>();
    private final Map<UUID, Double> pitchSnapBufferMap = new HashMap<>();

    @Override
    public void onUpdate(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        float cachedPitch = cachedPitchMap.getOrDefault(uuid, 0.0f);
        int blocksPlaced = blocksPlacedMap.getOrDefault(uuid, 0);
        double buffer = pitchSnapBufferMap.getOrDefault(uuid, 0.0);

        boolean bridgingCheck = player.rotationPitchHead > 70 && player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemBlock;

        if (bridgingCheck && player.isSwingInProgress && !player.isJumping && player.onGround && MoveUtil.getSpeed(player) > 0.1) {
            double pitchDelta = Math.abs(player.rotationPitchHead - cachedPitch);

            if (pitchDelta > 2) {
                buffer += 1.4;

                if (buffer > 4) {
                    flag(player, "Suspicious pitch change");
                    buffer = 0;
                }
            } else {
                buffer = Math.max(0, buffer - 0.8);
            }
        }

        if (blocksPlaced > 7) {
            buffer = 0;
        }

        cachedPitch = player.rotationPitchHead;

        cachedPitchMap.put(uuid, cachedPitch);
        pitchSnapBufferMap.put(uuid, buffer);
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        cachedPitchMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
        pitchSnapBufferMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
    }
}