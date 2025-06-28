package wtf.demise.events.impl.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import wtf.demise.events.impl.CancellableEvent;

@Getter
@Setter
@AllArgsConstructor
public final class RenderItemEvent extends CancellableEvent {
    private EnumAction enumAction;
    private boolean useItem;
    private float progress, swingProgress;
    private ItemStack itemToRender;
}
