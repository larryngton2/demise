package wtf.demise.features.modules.impl.misc.cheatdetector.impl.player;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.BlockPos;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LegitScaffoldCheck extends Check {
    private final Map<UUID, Boolean> shouldSneakMap = new HashMap<>();
    private final Map<UUID, Integer> sneakTicksMap = new HashMap<>();
    private final Map<UUID, Integer> bufferMap = new HashMap<>();

    @Override
    public String getName() {
        return "Legit scaffold";
    }

    @Override
    public void onPacket(PacketEvent e, EntityPlayer player) {
        boolean shouldSneak = shouldSneakMap.getOrDefault(player.getUniqueID(), false);
        int sneakTicks = sneakTicksMap.getOrDefault(player.getUniqueID(), 0);
        int buffer = bufferMap.getOrDefault(player.getUniqueID(), 0);

        if (e.getPacket() instanceof S14PacketEntity s14 && s14.getEntityId() == player.getEntityId()) {
            if (s14.getOnGround()) {
                final Block block = mc.theWorld.getBlockState(new BlockPos(player.posX, player.posY - 1, player.posZ)).getBlock();

                if (block instanceof BlockAir) {
                    shouldSneak = true;
                    sneakTicks++;
                } else {
                    shouldSneak = false;
                    sneakTicks = 0;
                }
            } else {
                shouldSneak = false;
                sneakTicks = 0;
            }
        }

        if (e.getPacket() instanceof S1CPacketEntityMetadata wrapper) {
            if (wrapper.func_149376_c() != null && wrapper.getEntityId() == player.getEntityId()) {
                for (final DataWatcher.WatchableObject object : wrapper.func_149376_c()) {
                    if (object.getObject() instanceof Byte && (byte) object.getObject() == 2) {
                        if (shouldSneak && sneakTicks <= 2) {
                            buffer = Math.min(10000, buffer + 1);

                            if (buffer > 10) {
                                flag(player, "Sneaking too fast");
                                shouldSneak = false;
                                sneakTicks = 0;
                            }
                        } else {
                            buffer = Math.max(0, buffer - 5);
                        }
                    }
                }
            }
        }

        shouldSneakMap.put(player.getUniqueID(), shouldSneak);
        sneakTicksMap.put(player.getUniqueID(), sneakTicks);
        bufferMap.put(player.getUniqueID(), buffer);
    }
}