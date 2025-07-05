package wtf.demise.features.modules.impl.visual;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.render.ViewBobbingEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "ViewBobbing", description = "Modifies your bobbing animation.")
public class ViewBobbing extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Smooth", "Funny", "None"}, "Smooth", this);

    @EventTarget
    public void onViewBobbing(ViewBobbingEvent e) {
        if (mode.is("Smooth")) {
            if (e.getState() == ViewBobbingEvent.State.CameraTransform || e.getState() == ViewBobbingEvent.State.Hand2) {
                e.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (!e.isPost()) return;

        mc.gameSettings.viewBobbing = true;

        switch (mode.get()) {
            case "Funny":
                mc.thePlayer.cameraYaw = 0.5F;
                break;
            case "None":
                mc.thePlayer.distanceWalkedModified = 0.0F;
                break;

        }
    }
}