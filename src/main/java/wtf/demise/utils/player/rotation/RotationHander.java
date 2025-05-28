package wtf.demise.utils.player.rotation;

import wtf.demise.features.modules.Module;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.InstanceAccess;

public class RotationHander implements InstanceAccess {

    public RotationHander(Module module) {
        final ModeValue rotationMode = new ModeValue("Rotation mode", new String[]{"Silent", "Derp"}, "Silent", module);
        final ModeValue smoothMode = new ModeValue("Smooth mode", new String[]{"Linear", "Lerp", "Bezier", "Exponential", "Relative", "LinearAcceleration", "None"}, "Linear", module);
        final BoolValue accelerate = new BoolValue("Accelerate", false, module, () -> !smoothMode.is("None"));
        final BoolValue imperfectCorrelation = new BoolValue("Imperfect correlation", false, module, () -> !smoothMode.is("None"));
        final SliderValue yawRotationSpeedMin = new SliderValue("Yaw rotation speed (min)", 1, 0.01f, 180, 0.01f, module, () -> !smoothMode.is("None"));
        final SliderValue yawRotationSpeedMax = new SliderValue("Yaw rotation speed (max)", 1, 0.01f, 180, 0.01f, module, () -> !smoothMode.is("None"));
        final SliderValue pitchRotationSpeedMin = new SliderValue("Pitch rotation speed (min)", 1, 0.01f, 180, 0.01f, module, () -> !smoothMode.is("None"));
        final SliderValue pitchRotationSpeedMax = new SliderValue("Pitch rotation speed (max)", 1, 0.01f, 180, 0.01f, module, () -> !smoothMode.is("None"));
        final SliderValue midpoint = new SliderValue("Midpoint", 0.3f, 0.01f, 1, 0.01f, module, () -> smoothMode.is("Bezier"));
        final ModeValue movementFix = new ModeValue("Movement fix", new String[]{"None", "Silent", "Strict"}, "None", module);
    }
}
