package wtf.demise.features.modules.impl.combat;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "KeepSprint", category = ModuleCategory.Combat)
public class KeepSprint extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Normal"}, "Normal", this);
    public final SliderValue motion = new SliderValue("Motion", 0.5f, 0, 1, 0.01f, this);
    public final BoolValue sprint = new BoolValue("Sprint", true, this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());
    }
}
