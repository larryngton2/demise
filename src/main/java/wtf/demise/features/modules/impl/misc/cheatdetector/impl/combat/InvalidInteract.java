package wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S0BPacketAnimation;
import wtf.demise.events.impl.packet.PacketEvent;
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
    public void onPacket(PacketEvent e, EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        int useTime = useTimeMap.getOrDefault(uuid, 0);

        if (player.isUsingItem()) {
            useTime++;
        } else {
            useTime = 0;
        }

        useTimeMap.put(uuid, useTime);

        if (useTime > 2 && e.getPacket() instanceof S0BPacketAnimation s0b && s0b.getEntityID() == player.getEntityId()) {
            flag(player, "Swinging while using an item");
        }
    }

    @Override
    public void cleanup(Set<UUID> onlineUUIDs) {
        useTimeMap.keySet().removeIf(uuid -> !onlineUUIDs.contains(uuid));
    }
}