package net.minecraft.network.play.client;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;

public class C01PacketChatMessage implements Packet<INetHandlerPlayServer> {
    @Setter
    @Getter
    private String message;

    public C01PacketChatMessage() {
    }

    public C01PacketChatMessage(String messageIn) {
        if (messageIn.length() > 100) {
            messageIn = messageIn.substring(0, 100);
        }

        this.message = messageIn;
    }

    public void readPacketData(PacketBuffer buf) {
        this.message = buf.readStringFromBuffer(100);
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeString(this.message);
    }

    public void processPacket(INetHandlerPlayServer handler) {
        handler.processChatMessage(this);
    }
}
