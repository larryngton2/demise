package wtf.demise.features.modules.impl.misc;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Test", category = ModuleCategory.Misc)
public class Test extends Module {
    public final SliderValue anglex = new SliderValue("Angle X", 0.5f, 0, 10, 0.01f, this);
    public final BoolValue doAngleY = new BoolValue("Render angle Y", false, this);
    public final SliderValue angley = new SliderValue("Angle Y", -0.5235988F, -1, 1, 0.01f, this, doAngleY::get);
    public final BoolValue forceBlock = new BoolValue("Force block", false, this);

    public final SliderValue x1 = new SliderValue("X1", -0.15f, -1, 1, 0.01f, this);
    public final SliderValue y1 = new SliderValue("Y1", -0.2f, -1, 1, 0.01f, this);
    public final SliderValue z1 = new SliderValue("Z1", 0, -1, 1, 0.01f, this);
    public final SliderValue deg = new SliderValue("Deg", 70, 0, 180, 1, this);
    public final SliderValue deg2 = new SliderValue("Deg2", 0, -180, 180, 1, this);
    public final SliderValue x2 = new SliderValue("X2", 0.119f, -1, 1, 0.01f, this);
    public final SliderValue y2 = new SliderValue("Y2", 0.2f, -1, 1, 0.01f, this);
    public final SliderValue z2 = new SliderValue("Z2", -0.024f, -1, 1, 0.01f, this);
}
