package wtf.demise.events.impl.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wtf.demise.events.impl.CancellableEvent;

@Getter
@Setter
@AllArgsConstructor
public class ViewBobbingEvent extends CancellableEvent {
    private State state;

    public enum State {
        CameraTransform,
        Hand1,
        Hand2
    }
}
