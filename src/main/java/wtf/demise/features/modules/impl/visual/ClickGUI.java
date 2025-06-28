package wtf.demise.features.modules.impl.visual;

import org.lwjglx.input.Keyboard;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "ClickGUI", description = "Just guess.", category = ModuleCategory.Visual, key = Keyboard.KEY_RSHIFT)
public class ClickGUI extends Module {

    @Override
    public void onEnable() {
        mc.displayGuiScreen(INSTANCE.getPanelGui());
        toggle();
    }
}