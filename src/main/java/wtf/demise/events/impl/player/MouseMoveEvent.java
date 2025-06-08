package wtf.demise.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wtf.demise.events.impl.Event;

@Getter
@Setter
@AllArgsConstructor
public class MouseMoveEvent implements Event {
    private int deltaX;
    private int deltaY;
    private State state;

    public enum State {
        PRE, UPDATE
    }
}
