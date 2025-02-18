package wtf.demise.events.impl.misc;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.impl.Event;

@Getter
@Setter
public class MouseOverEvent implements Event {

    public MouseOverEvent(double range, float expand) {
        this.range = range;
        this.expand = expand;
    }

    public MouseOverEvent(double range) {
        this.range = range;
        this.expand = 1f;
    }

    private double range;
    private float expand;
    private MovingObjectPosition movingObjectPosition;

}
