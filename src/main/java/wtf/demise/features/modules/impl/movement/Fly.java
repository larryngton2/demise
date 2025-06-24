package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.rotation.OldRotationUtils;

@ModuleInfo(name = "Fly", description = "Allows you to fly.", category = ModuleCategory.Movement)
public class Fly extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Intave"}, "Vanilla", this);
    private final SliderValue speed = new SliderValue("Speed", 2, 1, 5, 0.1f, this, () -> mode.is("Vanilla"));

    private final TimerUtils timer = new TimerUtils();
    private boolean paused = false;
    private boolean jumped = false;

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

                if (!jumped && mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                    jumped = true;
                }

                if (jumped) {
                    if (mc.thePlayer.fallDistance > 1 && !paused) {
                        mc.rightClickMouse();
                        freeze.setEnabled(true);
                        timer.reset();
                        paused = true;
                    }

                    if (paused && timer.hasTimeElapsed(4000)) {
                        setEnabled(false);
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (mode.is("Intave")) {
            OldRotationUtils.setRotation(new float[]{mc.thePlayer.rotationYaw - 180, 50}, MovementCorrection.Silent, 80, 80);
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
                jumped = false;
                paused = false;
                break;
        }
    }
}