package wtf.demise.features.modules.impl.movement;

import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.rotation.RotationHandler;

@ModuleInfo(name = "Fly", description = "Allows you to fly.")
public class Fly extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Intave", "Ground"}, "Vanilla", this);
    private final SliderValue speed = new SliderValue("Speed", 2, 1, 5, 0.1f, this, () -> mode.is("Vanilla"));
    //private final BoolValue autoJump = new BoolValue("Auto jump", false, this, () -> mode.is("Ground"));
    private final BoolValue vanillaKickBypass = new BoolValue("Vanilla kick bypass", false, this);

    private final TimerUtils timer = new TimerUtils();
    private boolean paused = false;
    private boolean jumped = false;
    private int ticks;
    private boolean down;

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
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            if (mode.is("Ground")) {
                if (ticks >= 8 && (!Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()) || mc.currentScreen != null)) {
                    mc.thePlayer.onGround = true;
                    e.setOnGround(true);
                    mc.thePlayer.motionY = 0;
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && mc.thePlayer.onGround && mc.currentScreen == null) {
                    ticks = 0;
                }

                ticks++;
            }

            if (mc.thePlayer.ticksExisted % 40 == 0 && vanillaKickBypass.get()) {
                if (!down) {
                    mc.thePlayer.motionY -= 0.04;
                    down = true;
                } else {
                    mc.thePlayer.motionY += 0.04;
                    down = false;
                }

                ChatUtils.sendMessageClient("sdffs");
            }
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (mode.is("Intave")) {
            RotationHandler.setBasicRotation(new float[]{mc.thePlayer.rotationYaw - 180, 50}, true, 80, 80);
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