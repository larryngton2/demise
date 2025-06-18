package net.minecraft.network.play.server;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;

public class S46PacketSetCompressionLevel implements Packet<INetHandlerPlayClient> {
    private int threshold;

    public void readPacketData(PacketBuffer buf) {
        this.threshold = buf.readVarIntFromBuffer();
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeVarIntToBuffer(this.threshold);
    }

    public void processPacket(INetHandlerPlayClient handler) {
        handler.handleSetCompressionLevel(this);
    }

    public int getThreshold() {
        return this.threshold;
    }
}
