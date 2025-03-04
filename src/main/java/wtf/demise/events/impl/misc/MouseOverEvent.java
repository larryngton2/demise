package wtf.demise.events.impl.misc;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.impl.Event;

@Getter
@Setter
public class MouseOverEvent implements Event {
    public MouseOverEvent(double range, float expand, double blockRange) {
        this.range = range;
        this.expand = expand;
        this.blockRange = blockRange;
    }

    private double range;
    private float expand;
    private MovingObjectPosition movingObjectPosition;
    private double blockRange;
}
