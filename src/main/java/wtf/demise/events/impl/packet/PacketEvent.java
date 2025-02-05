package wtf.demise.events.impl.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;
import wtf.demise.events.impl.CancellableEvent;

@Getter
@AllArgsConstructor
public class PacketEvent extends CancellableEvent {
    @Setter
    private Packet<?> packet;
    private final State state;

    public enum State {
        INCOMING,
        OUTGOING
    }
}
