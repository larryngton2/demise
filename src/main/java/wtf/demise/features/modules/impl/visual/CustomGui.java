package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "MenuGui", category = ModuleCategory.Visual)
public class CustomGui extends Module {
    public final ModeValue buttonStyle = new ModeValue("Button style", new String[]{"Custom", "Vanilla"}, "Custom", this);
    public final BoolValue shaderMenu = new BoolValue("Shader main menu", true, this);
}
