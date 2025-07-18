package net.minecraft.network.play.client;

import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayServer;

public class C13PacketPlayerAbilities implements Packet<INetHandlerPlayServer> {
    private boolean invulnerable;
    private boolean flying;
    private boolean allowFlying;
    private boolean creativeMode;
    private float flySpeed;
    private float walkSpeed;

    public C13PacketPlayerAbilities() {
    }

    public C13PacketPlayerAbilities(PlayerCapabilities capabilities) {
        this.setInvulnerable(capabilities.disableDamage);
        this.setFlying(capabilities.isFlying);
        this.setAllowFlying(capabilities.allowFlying);
        this.setCreativeMode(capabilities.isCreativeMode);
        this.setFlySpeed(capabilities.getFlySpeed());
        this.setWalkSpeed(capabilities.getWalkSpeed());
    }

    public void readPacketData(PacketBuffer buf) {
        byte b0 = buf.readByte();
        this.setInvulnerable((b0 & 1) > 0);
        this.setFlying((b0 & 2) > 0);
        this.setAllowFlying((b0 & 4) > 0);
        this.setCreativeMode((b0 & 8) > 0);
        this.setFlySpeed(buf.readFloat());
        this.setWalkSpeed(buf.readFloat());
    }

    public void writePacketData(PacketBuffer buf) {
        byte b0 = 0;

        if (this.isInvulnerable()) {
            b0 = (byte) (b0 | 1);
        }

        if (this.isFlying()) {
            b0 = (byte) (b0 | 2);
        }

        if (this.isAllowFlying()) {
            b0 = (byte) (b0 | 4);
        }

        if (this.isCreativeMode()) {
            b0 = (byte) (b0 | 8);
        }

        buf.writeByte(b0);
        buf.writeFloat(this.flySpeed);
        buf.writeFloat(this.walkSpeed);
    }

    public void processPacket(INetHandlerPlayServer handler) {
        handler.processPlayerAbilities(this);
    }

    public boolean isInvulnerable() {
        return this.invulnerable;
    }

    public void setInvulnerable(boolean isInvulnerable) {
        this.invulnerable = isInvulnerable;
    }

    public boolean isFlying() {
        return this.flying;
    }

    public void setFlying(boolean isFlying) {
        this.flying = isFlying;
    }

    public boolean isAllowFlying() {
        return this.allowFlying;
    }

    public void setAllowFlying(boolean isAllowFlying) {
        this.allowFlying = isAllowFlying;
    }

    public boolean isCreativeMode() {
        return this.creativeMode;
    }

    public void setCreativeMode(boolean isCreativeMode) {
        this.creativeMode = isCreativeMode;
    }

    public void setFlySpeed(float flySpeedIn) {
        this.flySpeed = flySpeedIn;
    }

    public void setWalkSpeed(float walkSpeedIn) {
        this.walkSpeed = walkSpeedIn;
    }
}
