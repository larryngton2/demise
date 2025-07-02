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

public class ScaffoldA extends Check {

    @Override
    public String getName() {
        return "Scaffold A";
    }

    private final Map<UUID, Float> cachedYawMap = new HashMap<>();
    private final Map<UUID, Integer> yawSnapCountMap = new HashMap<>();

    @Override
    public void onUpdate(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        float cachedYaw = cachedYawMap.getOrDefault(uuid, 0.0f);
        int blocksPlaced = blocksPlacedMap.getOrDefault(uuid, 0);
        int yawSnapCount = yawSnapCountMap.getOrDefault(uuid, 0);

        boolean bridgingCheck = player.rotationPitchHead > 70 && player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemBlock;

        if (bridgingCheck && player.isSwingInProgress && !player.isJumping && player.onGround && MoveUtil.getSpeed(player) > 0.1) {
            if (Math.abs(player.rotationYawHead - cachedYaw) > 45 && !player.isSneaking()) {
                yawSnapCount++;

                if (yawSnapCount > 2) {
                    flag(player, "Suspicious yaw change");
                    yawSnapCount = 0;
                }
            }
        }

        if (blocksPlaced > 7) {
            yawSnapCount = 0;
        }

        cachedYaw = player.rotationYawHead;

        cachedYawMap.put(uuid, cachedYaw);
        yawSnapCountMap.put(uuid, yawSnapCount);
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        cachedYawMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
        yawSnapCountMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
    }
}