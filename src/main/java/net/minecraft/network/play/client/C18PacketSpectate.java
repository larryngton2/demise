package net.minecraft.network.play.client;

import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.world.WorldServer;

import java.util.UUID;

public class C18PacketSpectate implements Packet<INetHandlerPlayServer> {
    private UUID id;

    public C18PacketSpectate() {
    }

    public C18PacketSpectate(UUID id) {
        this.id = id;
    }

    public void readPacketData(PacketBuffer buf) {
        this.id = buf.readUuid();
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeUuid(this.id);
    }

    public void processPacket(INetHandlerPlayServer handler) {
        handler.handleSpectate(this);
    }

    public Entity getEntity(WorldServer worldIn) {
        return worldIn.getEntityFromUuid(this.id);
    }
}
