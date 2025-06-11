package wtf.demise.features.modules.impl.misc;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;

@ModuleInfo(name = "Test", category = ModuleCategory.Misc)
public class Test extends Module {
    public final BoolValue hitboxes = new BoolValue("Hitboxes", false, this);
}