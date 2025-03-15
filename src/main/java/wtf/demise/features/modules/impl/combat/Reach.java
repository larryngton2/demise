package wtf.demise.features.modules.impl.combat;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.killaura.KillAura;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;

@ModuleInfo(name = "Reach", category = ModuleCategory.Combat)
public class Reach extends Module {

    public final SliderValue min = new SliderValue("Min Range", 3.0F, 3, 6F, .1f, this);
    public final SliderValue max = new SliderValue("Max Range", 3.3F, 3, 6F, .1f, this);

    @EventTarget
    public void onMouseOver(MouseOverEvent event) {
        if (!getModule(KillAura.class).isEnabled()) {
            event.setRange(MathUtils.nextDouble(min.getMin(), max.getMax()));
        }
    }
}
