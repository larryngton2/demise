package wtf.demise.features.modules.impl.misc;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Test", category = ModuleCategory.Misc)
public class Test extends Module {
    public final SliderValue xpad = new SliderValue("xpad", 7, 1, 20, 1, this);
    public final SliderValue ypad = new SliderValue("ypad", 5, 1, 20, 1, this);
}
