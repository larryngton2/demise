package wtf.demise.events.impl.player;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wtf.demise.events.impl.Event;

@Setter
@Getter
@AllArgsConstructor
public final class SneakSlowDownEvent implements Event {
    private double strafe;
    private double forward;
}
