package wtf.demise.utils.packet;

import lombok.experimental.UtilityClass;
import net.minecraft.network.Packet;
import wtf.demise.utils.InstanceAccess;

import java.util.Arrays;

@UtilityClass
public class PacketUtils implements InstanceAccess {
    public void sendPacket(Packet<?> packet) {
        mc.getNetHandler().addToSendQueue(packet);
    }

    public void sendPacketNoEvent(Packet<?> packet) {
        mc.getNetHandler().sendPacketNoEvent(packet);
    }

    public void queue(final Packet packet) {
        if (packet == null) {
            System.out.println("Packet is null");
            return;
        }

        if (isClientPacket(packet)) {
            mc.getNetHandler().sendPacketNoEvent(packet);
        } else {
            packet.processPacket(mc.getNetHandler().getNetworkManager().getNetHandler());
        }
    }

    public boolean isClientPacket(final Packet<?> packet) {
        return Arrays.stream(NetworkAPI.serverbound).anyMatch(clazz -> clazz == packet.getClass());
    }
}
