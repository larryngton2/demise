package wtf.demise.events.impl.render;

import lombok.Getter;
import net.minecraft.entity.Entity;
import wtf.demise.events.impl.CancellableEvent;

@Getter
public class RenderNameTagEvent extends CancellableEvent {
    final Entity entity;

    public RenderNameTagEvent(Entity entity) {
        this.entity = entity;
    }
}