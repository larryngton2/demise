package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ColorValue;
import wtf.demise.features.values.impl.SliderValue;

import java.awt.*;

@ModuleInfo(name = "CustomBlockHover", category = ModuleCategory.Visual)
public class CustomBlockHover extends Module {
    public final BoolValue outline = new BoolValue("Outline", true, this);
    public final BoolValue filled = new BoolValue("Filled", false, this);
    public final BoolValue syncColor = new BoolValue("Sync Color", false, this);
    public final ColorValue color = new ColorValue("Color", new Color(255, 255, 255), this, () -> !syncColor.get());
    public final BoolValue interpolate = new BoolValue("Interpolate", false, this);
    public final SliderValue interpolationAmount = new SliderValue("Interpolation Amount", 0.5f, 0, 1, 0.01f, this, interpolate::get);
}
