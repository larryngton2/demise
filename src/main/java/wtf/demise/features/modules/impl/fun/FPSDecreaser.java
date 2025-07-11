package wtf.demise.features.modules.impl.fun;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;

// devious
@ModuleInfo(name = "FPSDecreaser", description = "FPSBooster but on cocaine.")
public class FPSDecreaser extends Module {
    @EventTarget
    public void onGameEvent(GameEvent e) {
        for (long i = Long.MIN_VALUE; i < Long.MAX_VALUE; i++) {
            // so the loop isn't skipped
            mc.thePlayer.rotationYaw = mc.thePlayer.rotationYaw - i + i;
        }
    }
}
