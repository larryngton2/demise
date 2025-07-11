package net.minecraft.network.play.server;

import com.google.common.collect.Maps;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;

import java.util.Map;
import java.util.Map.Entry;

public class S37PacketStatistics implements Packet<INetHandlerPlayClient> {
    private Map<StatBase, Integer> field_148976_a;

    public S37PacketStatistics() {
    }

    public S37PacketStatistics(Map<StatBase, Integer> p_i45173_1_) {
        this.field_148976_a = p_i45173_1_;
    }

    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleStatistics(this);
    }

    public void readPacketData(PacketBuffer buf) {
        int i = buf.readVarIntFromBuffer();
        this.field_148976_a = Maps.newHashMap();

        for (int j = 0; j < i; ++j) {
            StatBase statbase = StatList.getOneShotStat(buf.readStringFromBuffer(32767));
            int k = buf.readVarIntFromBuffer();

            if (statbase != null) {
                this.field_148976_a.put(statbase, k);
            }
        }
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeVarIntToBuffer(this.field_148976_a.size());

        for (Entry<StatBase, Integer> entry : this.field_148976_a.entrySet()) {
            buf.writeString(entry.getKey().statId);
            buf.writeVarIntToBuffer(entry.getValue());
        }
    }

    public Map<StatBase, Integer> func_148974_c() {
        return this.field_148976_a;
    }
}
