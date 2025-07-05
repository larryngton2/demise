package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;

@ModuleInfo(name = "Rotation", description = "Renders server-sided rotations.")
public class Rotation extends Module {
    public final BoolValue body = new BoolValue("Render Body", true, this);
    public final BoolValue realistic = new BoolValue("Realistic", true, this, body::get);
}