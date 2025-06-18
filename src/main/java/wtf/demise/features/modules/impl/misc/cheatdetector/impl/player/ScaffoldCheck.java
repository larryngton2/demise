package wtf.demise.features.modules.impl.misc.cheatdetector.impl.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.scaffold.ScaffoldA;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.scaffold.ScaffoldB;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.scaffold.ScaffoldC;
import wtf.demise.utils.player.MoveUtil;

import java.util.*;

public class ScaffoldCheck extends Check {
    @Override
    public String getName() {
        return "Scaffold";
    }

    private final List<Check> checks = Arrays.asList(new ScaffoldA(), new ScaffoldB(), new ScaffoldC());
    public static final Map<UUID, Integer> blocksPlacedMap = new HashMap<>();
    private final Map<UUID, Boolean> bridgingMap = new HashMap<>();

    @Override
    public void onUpdate(EntityPlayer player) {
        checks.forEach(check -> check.onUpdate(player));

        UUID uuid = player.getUniqueID();
        int blocksPlaced = blocksPlacedMap.getOrDefault(uuid, 0);
        boolean bridgingCheck = bridgingMap.getOrDefault(uuid, false);

        if (bridgingCheck && player.swingProgressInt == 0 && player.isSwingInProgress) {
            blocksPlaced++;
        }

        bridgingCheck = player.rotationPitchHead > 70 && player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemBlock;

        if (!MoveUtil.isMoving(player) || player.isJumping || player.isSneaking() || !player.onGround || MoveUtil.getSpeed(player) < 0.1) blocksPlaced = 0;

        if (blocksPlaced > 8) {
            blocksPlaced = 0;
        }

        bridgingMap.put(uuid, bridgingCheck);
        blocksPlacedMap.put(uuid, blocksPlaced);
    }

    @Override
    public void onPacket(PacketEvent e, EntityPlayer player) {
        if (e.getPacket() instanceof S25PacketBlockBreakAnim s25 && s25.getBreakerId() == player.getEntityId()) {
            if (bridgingMap.getOrDefault(player.getUniqueID(), false)) {
                bridgingMap.put(player.getUniqueID(), false);
            }
        }
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        blocksPlacedMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
        checks.forEach(check -> check.cleanup(onlineUUIDs));
    }
}