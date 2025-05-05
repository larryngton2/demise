package wtf.demise.features.modules.impl.legit;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "HitBox", description = "Expands your target's hitbox.", category = ModuleCategory.Legit)
public class HitBox extends Module {
    private final SliderValue expand = new SliderValue("Expand", 0.5f, -0.5f, 1, 0.1f, this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(expand.get()));
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent e) {
        e.setExpand(expand.get());
    }
}