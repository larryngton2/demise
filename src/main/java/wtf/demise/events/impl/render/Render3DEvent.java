package wtf.demise.events.impl.render;

import net.minecraft.client.gui.ScaledResolution;
import wtf.demise.events.impl.Event;

public record Render3DEvent(float partialTicks, ScaledResolution scaledResolution) implements Event {
}