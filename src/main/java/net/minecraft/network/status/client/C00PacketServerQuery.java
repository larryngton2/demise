package net.minecraft.network.status.client;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.status.INetHandlerStatusServer;

public class C00PacketServerQuery implements Packet<INetHandlerStatusServer> {
    public void readPacketData(PacketBuffer buf) {
    }

    public void writePacketData(PacketBuffer buf) {
    }

    public void processPacket(INetHandlerStatusServer handler) {
        handler.processServerQuery(this);
    }
}
