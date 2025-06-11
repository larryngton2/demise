package wtf.demise.features.modules.impl.misc;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Test", category = ModuleCategory.Misc)
public class Test extends Module {
    public final BoolValue hitboxes = new BoolValue("Hitboxes", false, this);
    public final SliderValue scale = new SliderValue("Scale", 1, 1, 1, 0.1f, this);
}