package wtf.demise.features.modules.impl.visual;

import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "FreeLook", category = ModuleCategory.Visual)
public class FreeLook extends Module {
    private boolean released;

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (event.isPost()) {
            if (Mouse.isButtonDown(2)) {
                mc.gameSettings.thirdPersonView = 1;
                released = false;
            } else {
                if (!released) {
                    mc.gameSettings.thirdPersonView = 0;
                    released = true;
                }
            }
        }
    }
}
