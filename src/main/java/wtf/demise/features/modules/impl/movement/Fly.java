package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "Fly", description = "Allows you to fly.", category = ModuleCategory.Movement)
public class Fly extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Intave"}, "Vanilla", this);
    private final SliderValue speed = new SliderValue("Speed", 2, 1, 5, 0.1f, this, () -> mode.is("Vanilla"));

    private final TimerUtils flagTimer = new TimerUtils();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        switch (mode.get()) {
            case "Vanilla":
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806D * speed.get()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
            case "Intave":
                Freeze freeze = getModule(Freeze.class);

                freeze.setEnabled(flagTimer.hasTimeElapsed(200));

                if (mc.thePlayer.hurtTime >= 5) {
                    flagTimer.reset();
                }
                break;
        }
    }

    @Override
    public void onDisable() {
        switch (mode.get()) {
            case "Vanilla":
                if (mc.thePlayer == null)
                    return;

                if (mc.thePlayer.capabilities.isFlying) {
                    mc.thePlayer.capabilities.isFlying = false;
                }

                mc.thePlayer.capabilities.setFlySpeed(0.05F);
                break;
            case "Intave":
                getModule(Freeze.class).setEnabled(false);
                break;
        }
    }
}