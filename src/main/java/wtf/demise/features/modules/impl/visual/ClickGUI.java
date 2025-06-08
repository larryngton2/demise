package wtf.demise.features.modules.impl.visual;

import org.lwjglx.input.Keyboard;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ColorValue;

import java.awt.*;

@ModuleInfo(name = "ClickGUI", category = ModuleCategory.Visual, key = Keyboard.KEY_RSHIFT)
public class ClickGUI extends Module {
    public final ColorValue color = new ColorValue("Color", new Color(80, 80, 80), this);

    @Override
    public void onEnable() {
        mc.displayGuiScreen(INSTANCE.getDropdownGUI());
        toggle();
        super.onEnable();
    }
}