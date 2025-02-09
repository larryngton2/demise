package wtf.demise.features.modules.impl.misc;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;

@ModuleInfo(name = "Options", category = ModuleCategory.Misc)
public class Options extends Module {
    public final BoolValue fixHealth = new BoolValue("Fix health", false, this);
    public final BoolValue cape = new BoolValue("Cape", true, this);
    public final BoolValue wavey = new BoolValue("Wavey cape", true, this);
    public final BoolValue enchanted = new BoolValue("Enchanted", true, this, () -> cape.get() && !wavey.get());

    @Override
    public void onEnable() {
        this.toggle();
    }
}