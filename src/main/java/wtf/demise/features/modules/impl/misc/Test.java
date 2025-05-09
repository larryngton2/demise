package wtf.demise.features.modules.impl.misc;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Test", category = ModuleCategory.Misc)
public class Test extends Module {
    public final SliderValue extray = new SliderValue("extray", 5, 1, 20, 1, this);
    public final SliderValue heiught = new SliderValue("heiught", 4, 1, 20, 1, this);
}
