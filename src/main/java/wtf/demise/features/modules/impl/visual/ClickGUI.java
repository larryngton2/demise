package wtf.demise.features.modules.impl.visual;

import org.lwjglx.input.Keyboard;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "ClickGUI", category = ModuleCategory.Visual, key = Keyboard.KEY_RSHIFT)
public class ClickGUI extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Dropdown", "Panel"}, "Dropdown", this);

    @Override
    public void onEnable() {
        switch (mode.get()) {
            case "Dropdown" -> mc.displayGuiScreen(INSTANCE.getDropdownGUI());
            case "Panel" -> mc.displayGuiScreen(INSTANCE.getPanelGui());
        }

        toggle();
        super.onEnable();
    }
}