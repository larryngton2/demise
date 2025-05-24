package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "ThirdPersonDistance", category = ModuleCategory.Visual)
public class ThirdPersonDistance extends Module {
    public final SliderValue cameraDistance = new SliderValue("Distance", 4.0f, 1.0f, 8.0f, 1.0f, this);
}