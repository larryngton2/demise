package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.TextValue;

@ModuleInfo(name = "CustomSkin", category = ModuleCategory.Visual)
public class CustomSkin extends Module {
    public final BoolValue slimSkin = new BoolValue("Slim", true, this);
    public final BoolValue customUrl = new BoolValue("Custom URL", false, this);
    public final ModeValue skinMode = new ModeValue("Skin", new String[]{"larryngton", "Esound", "izuna", "GommeHD"}, "larryngton", this, () -> !customUrl.get());
    public final TextValue customSkinUrl = new TextValue("URL", "https://s.namemc.com/i/afd98273bb9a8b69.png", this, customUrl::get);

    public String getURL() {
        if (!customUrl.get()) {
            return switch (skinMode.get()) {
                case "larryngton" -> "https://s.namemc.com/i/afd98273bb9a8b69.png";
                case "Esound" -> "https://s.namemc.com/i/c6e0ab215c046d63.png";
                case "izuna" -> "https://s.namemc.com/i/b95cabd61250a94d.png";
                case "GommeHD" -> "https://s.namemc.com/i/0f1ee89922626929.png";
                default -> "";
            };
        } else {
            return customSkinUrl.get();
        }
    }
}
