package wtf.demise.events.impl.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;
import wtf.demise.events.impl.Event;

@Getter
@AllArgsConstructor
public class Render3DEvent implements Event {
    private final float partialTicks;
    private final ScaledResolution scaledResolution;
}
