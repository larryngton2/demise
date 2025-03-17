package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "Cape", category = ModuleCategory.Visual)
public class Cape extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Rise"}, "Normal", this);
    public final ModeValue riseMode = new ModeValue("Rise mode", new String[]{"Normal", "Red", "Green", "Blue", "Dogshit"}, "Normal", this, () -> mode.is("Rise"));
    public final BoolValue enchanted = new BoolValue("Enchanted", false, this);
}
