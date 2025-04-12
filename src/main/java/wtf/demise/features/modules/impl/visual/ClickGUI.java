package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.gui.GuiScreen;
import org.lwjglx.input.Keyboard;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ColorValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.gui.click.skeet.SkeetUI;

import java.awt.*;

@ModuleInfo(name = "ClickGUI", category = ModuleCategory.Visual, key = Keyboard.KEY_RSHIFT)
public class ClickGUI extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"DropDown", "Exhi"}, "DropDown", this);
    public final ColorValue color = new ColorValue("Color", new Color(80, 80, 80), this);
    public final BoolValue rainbow = new BoolValue("Rainbow", true, this, () -> mode.is("Exhi"));

    @Override
    public void onEnable() {
        GuiScreen guiScreen = switch (mode.get()) {
            case "DropDown" -> INSTANCE.getDropdownGUI();
            case "Exhi" -> INSTANCE.getSkeetGUI();
            default -> null;
        };
        mc.displayGuiScreen(guiScreen);

        if (mode.is("Exhi")) {
            SkeetUI.alpha = 0.0;
            INSTANCE.getSkeetGUI().targetAlpha = 255.0;
            SkeetUI.open = true;
            INSTANCE.getSkeetGUI().closed = false;
        }
        toggle();
        super.onEnable();
    }
}
