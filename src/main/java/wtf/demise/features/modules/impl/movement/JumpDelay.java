package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "JumpDelay", category = ModuleCategory.Movement)
public class JumpDelay extends Module {
    private final SliderValue jumpDelay = new SliderValue("Jump delay", 0, 0, 20, 1, this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(jumpDelay.get()));

        mc.thePlayer.jumpTicks = (int) jumpDelay.get();
    }
}
