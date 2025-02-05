package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MovementUtils;

@ModuleInfo(name = "Strafe", category = ModuleCategory.Movement)
public class Strafe extends Module {

    public final BoolValue ground = new BoolValue("Ground", true, this);
    public final SliderValue groundSpeed = new SliderValue("Ground Speed", 1, 0.01f, 10f, 0.1f, this);
    public final BoolValue air = new BoolValue("Air", true, this);
    public final SliderValue airSpeed = new SliderValue("Air Speed", 1, 0.01f, 10f, 0.1f, this);

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.thePlayer.onGround && ground.get()) MovementUtils.strafe(groundSpeed.get());
        if (!mc.thePlayer.onGround && air.get()) MovementUtils.strafe(airSpeed.get());
    }
}