package wtf.demise.events.impl.player;

import lombok.AllArgsConstructor;
import wtf.demise.events.impl.Event;

@AllArgsConstructor
public class LookEvent implements Event {

    public float[] rotation;
}
