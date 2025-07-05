package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;

@ModuleInfo(name = "CustomWidgets", description = "Edits the vanilla HUD widgets.", category = ModuleCategory.Visual)
public class CustomWidgetsModule extends Module {
    public final BoolValue hotbar = new BoolValue("Hotbar", true, this);
    public final BoolValue scoreboard = new BoolValue("Scoreboard", true, this);
    public final BoolValue chat = new BoolValue("Chat", true, this);
    public final BoolValue tabList = new BoolValue("TabList", true, this);
}
