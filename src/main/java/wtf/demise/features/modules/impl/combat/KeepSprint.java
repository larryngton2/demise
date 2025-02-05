package wtf.demise.features.modules.impl.combat;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "KeepSprint", category = ModuleCategory.Combat)
public class KeepSprint extends Module {

    public final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla"}, "Vanilla", this);
}
