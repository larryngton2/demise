package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Fly", category = ModuleCategory.Movement)
public class Fly extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Glide"}, "Vanilla", this);
    private final SliderValue speed = new SliderValue("Speed", 2, 1, 5, 0.1f, this);

    private boolean opf = false;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        switch (mode.get()) {
            case "Vanilla":
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806D * speed.get()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
            case "Glide":
                if (mc.thePlayer.movementInput.moveForward > 0.0F) {
                    if (!this.opf) {
                        this.opf = true;
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                        }
                    } else {
                        if (mc.thePlayer.onGround || mc.thePlayer.isCollidedHorizontally) {
                            this.setEnabled(false);
                            return;
                        }

                        double s = 1.94D * speed.get();
                        double r = Math.toRadians(mc.thePlayer.rotationYaw + 90.0F);
                        mc.thePlayer.motionX = s * Math.cos(r);
                        mc.thePlayer.motionZ = s * Math.sin(r);
                    }
                }
                break;
        }
    }

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
            case "Glide":
                this.opf = false;
                break;
        }
    }
}