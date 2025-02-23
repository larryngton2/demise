package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "JumpDelay", category = ModuleCategory.Movement)
public class JumpDelay extends Module {
    private final SliderValue jumpDelay = new SliderValue("Jump delay", 0, 0, 20, 1, this);
    private final TimerUtils delay = new TimerUtils();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(jumpDelay.get()));

        // if jumpTicks > 1 you will not be able to jump at all, so this is needed
        if (jumpDelay.get() > 1) {
            if (delay.hasTimeElapsed(jumpDelay.get() * 50)) {
                mc.thePlayer.jumpTicks = 0;
                delay.reset();
            } else {
                mc.thePlayer.jumpTicks = (int) jumpDelay.get();
            }
        } else {
            mc.thePlayer.jumpTicks = (int) jumpDelay.get();
        }
    }
}
