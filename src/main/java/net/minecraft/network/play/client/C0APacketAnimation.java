package net.minecraft.network.play.client;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;

public class C0APacketAnimation implements Packet<INetHandlerPlayServer> {
    public void readPacketData(PacketBuffer buf) {
    }

    public void writePacketData(PacketBuffer buf) {
    }

    public void processPacket(INetHandlerPlayServer handler) {
        handler.handleAnimation(this);
    }
}
