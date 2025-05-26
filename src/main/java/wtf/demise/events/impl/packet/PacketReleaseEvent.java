package wtf.demise.events.impl.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import wtf.demise.events.impl.CancellableEvent;
import wtf.demise.utils.packet.TimedPacket;

@Getter
@AllArgsConstructor
public class PacketReleaseEvent extends CancellableEvent {
    TimedPacket timedPacket;
}
