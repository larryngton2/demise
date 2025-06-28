package wtf.demise.utils.packet;

import lombok.Getter;
import net.minecraft.network.Packet;
import wtf.demise.utils.math.TimerUtils;

@Getter
public class TimedPacket {
    private final Packet packet;
    private final TimerUtils time;
    private final long millis;

    public TimedPacket(Packet packet) {
        this.packet = packet;
        this.time = new TimerUtils();
        this.millis = System.currentTimeMillis();
    }
}
