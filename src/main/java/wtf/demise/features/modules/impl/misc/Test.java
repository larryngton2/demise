package wtf.demise.features.modules.impl.misc;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Test", description = "test 12345", category = ModuleCategory.Misc)
public class Test extends Module {
    public final BoolValue fuckFontRenderer = new BoolValue("fuck the font renderer", false, this);
    public final SliderValue a = new SliderValue("a", 0, 0, 2, 0.01f, this);
}