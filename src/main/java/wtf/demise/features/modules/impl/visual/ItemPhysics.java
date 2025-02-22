package wtf.demise.features.modules.impl.visual;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Item Physics", category = ModuleCategory.Visual)
public class ItemPhysics extends Module {
    public final SliderValue rotationSpeed = new SliderValue("Rotation speed", 2, 0, 10, 1, this);
}
