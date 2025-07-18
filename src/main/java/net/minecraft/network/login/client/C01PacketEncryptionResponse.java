package net.minecraft.network.login.client;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.login.INetHandlerLoginServer;
import net.minecraft.util.CryptManager;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

public class C01PacketEncryptionResponse implements Packet<INetHandlerLoginServer> {
    private byte[] secretKeyEncrypted = new byte[0];
    private byte[] verifyTokenEncrypted = new byte[0];

    public C01PacketEncryptionResponse() {
    }

    public C01PacketEncryptionResponse(SecretKey secretKey, PublicKey publicKey, byte[] verifyToken) {
        this.secretKeyEncrypted = CryptManager.encryptData(publicKey, secretKey.getEncoded());
        this.verifyTokenEncrypted = CryptManager.encryptData(publicKey, verifyToken);
    }

    public void readPacketData(PacketBuffer buf) {
        this.secretKeyEncrypted = buf.readByteArray();
        this.verifyTokenEncrypted = buf.readByteArray();
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeByteArray(this.secretKeyEncrypted);
        buf.writeByteArray(this.verifyTokenEncrypted);
    }

    public void processPacket(INetHandlerLoginServer handler) {
        handler.processEncryptionResponse(this);
    }

    public SecretKey getSecretKey(PrivateKey key) {
        return CryptManager.decryptSharedKey(key, this.secretKeyEncrypted);
    }

    public byte[] getVerifyToken(PrivateKey key) {
        return key == null ? this.verifyTokenEncrypted : CryptManager.decryptData(key, this.verifyTokenEncrypted);
    }
}
