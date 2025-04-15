package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.TextValue;

@ModuleInfo(name = "CustomSkin", category = ModuleCategory.Visual)
public class CustomSkin extends Module {
    public final BoolValue slimSkin = new BoolValue("Slim", true, this);
    public final TextValue skinURL = new TextValue("URL", "https://textures.minecraft.net/texture/e1a7483e444322bb9e7f6f4341b80cb0d9063896c458fe45ea34899475ca6298", this);
}
