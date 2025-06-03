package wtf.demise.events.impl.player;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.events.impl.Event;

@Getter
@Setter
public class MouseMoveEvent implements Event {
    private int deltaX;
    private int deltaY;

    public MouseMoveEvent(int deltaX, int deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }
}
