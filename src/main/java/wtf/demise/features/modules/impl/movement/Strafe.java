package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "Strafe", category = ModuleCategory.Movement)
public class Strafe extends Module {
    public final BoolValue ground = new BoolValue("Ground", true, this);
    public final SliderValue groundSpeed = new SliderValue("Ground speed", 1, 0.01f, 1, 0.01f, this, ground::get);
    public final BoolValue air = new BoolValue("Air", true, this);
    public final SliderValue airSpeed = new SliderValue("Air speed", 1, 0.01f, 1, 0.01f, this, air::get);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.onGround && ground.get()) MoveUtil.strafe(groundSpeed.get());
        if (!mc.thePlayer.onGround && air.get()) MoveUtil.strafe(airSpeed.get());
    }
}