package net.minecraft.network.login.server;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.util.CryptManager;

import java.security.PublicKey;

public class S01PacketEncryptionRequest implements Packet<INetHandlerLoginClient> {
    private String hashedServerId;
    private PublicKey publicKey;
    private byte[] verifyToken;

    public S01PacketEncryptionRequest() {
    }

    public S01PacketEncryptionRequest(String serverId, PublicKey key, byte[] verifyToken) {
        this.hashedServerId = serverId;
        this.publicKey = key;
        this.verifyToken = verifyToken;
    }

    public void readPacketData(PacketBuffer buf) {
        this.hashedServerId = buf.readStringFromBuffer(20);
        this.publicKey = CryptManager.decodePublicKey(buf.readByteArray());
        this.verifyToken = buf.readByteArray();
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeString(this.hashedServerId);
        buf.writeByteArray(this.publicKey.getEncoded());
        buf.writeByteArray(this.verifyToken);
    }

    public void processPacket(INetHandlerLoginClient handler) {
        handler.handleEncryptionRequest(this);
    }

    public String getServerId() {
        return this.hashedServerId;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public byte[] getVerifyToken() {
        return this.verifyToken;
    }
}
