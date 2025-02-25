package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.DebugUtils;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.MovementUtils;
import wtf.demise.utils.player.RotationUtils;

@ModuleInfo(name = "Speed", category = ModuleCategory.Movement)
public class Speed extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Strafe Hop", "NCP", "Verus", "Legit", "Intave"}, "Strafe Hop", this);
    private final ModeValue yawOffsetMode = new ModeValue("Yaw offset", new String[]{"None", "Ground", "Air", "Constant"}, "Air", this);
    private final BoolValue smooth = new BoolValue("Smooth", false, this, () -> mode.is("Strafe Hop"));
    private final BoolValue ground = new BoolValue("Ground", true, this, () -> mode.is("Strafe Hop"));
    private final BoolValue air = new BoolValue("Air", true, this, () -> mode.is("Strafe Hop"));
    private final ModeValue ncpMode = new ModeValue("NCP mode", new String[]{"On tick 4", "On tick 5", "Old BHop"}, "On tick 5", this, () -> mode.is("NCP"));
    private final ModeValue verusMode = new ModeValue("Verus mode", new String[]{"Low"}, "Low", this, () -> mode.is("Verus"));
    private final SliderValue speedMulti = new SliderValue("Extra speed multiplier", 0.4f, 0f, 1f, 0.01f, this, () -> ncpMode.is("Old BHop") && ncpMode.canDisplay());
    private final SliderValue iBoostMulti = new SliderValue("Boost multiplier", 1, 0f, 1, 0.1f, this, () -> mode.is("Intave"));
    private final BoolValue minSpeedLimiter = new BoolValue("Min speed limiter", false, this);
    private final SliderValue minSpeed = new SliderValue("Min speed", 0.25f, 0, 1, 0.05f, this, minSpeedLimiter::get);
    private final SliderValue minMoveTicks = new SliderValue("Move ticks for limit", 15, 0, 40, 1, this, minSpeedLimiter::get);
    private final BoolValue printAirTicks = new BoolValue("Print airTicks", false);

    private int movingTicks, stoppedTicks, ticks;

    @Override
    public void onEnable() {
        ticks = 0;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get().replace(" ", ""));

        if (!MovementUtils.isMoving()) {
            movingTicks = 0;
            stoppedTicks++;
            return;
        } else {
            if (mc.thePlayer.onGround && !mode.is("Legit")) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            }

            movingTicks++;
            stoppedTicks = 0;
        }

        if (printAirTicks.get()) {
            DebugUtils.sendMessage("Air Ticks: " + mc.thePlayer.offGroundTicks);
        }

        if (MovementUtils.isMoving() && MovementUtils.getSpeed() < minSpeed.get() && movingTicks > minMoveTicks.get() && minSpeedLimiter.get()) {
            MovementUtils.strafe(minSpeed.get());
        }

        switch (yawOffsetMode.get()) {
            case "Ground":
                if (mc.thePlayer.onGround) {
                    RotationUtils.setRotation(new float[]{MovementUtils.getYawFromKeybind(), mc.thePlayer.rotationPitch}, MovementCorrection.SILENT, 180, 180);
                }
                break;
            case "Air":
                if (!mc.thePlayer.onGround) {
                    RotationUtils.setRotation(new float[]{mc.thePlayer.rotationYaw + 45, mc.thePlayer.rotationPitch}, MovementCorrection.SILENT, 180, 180);
                }
                break;
            case "Constant":
                RotationUtils.setRotation(new float[]{MovementUtils.getYawFromKeybind(), mc.thePlayer.rotationPitch}, MovementCorrection.SILENT, 180, 180);
                break;
        }

        switch (mode.get()) {
            case "NCP":
                if (ncpMode.is("On tick 4")) {
                    switch (mc.thePlayer.offGroundTicks) {
                        case 4:
                            mc.thePlayer.motionY = -0.09800000190734863;
                            break;

                        case 6:
                            if (MovementUtils.isMoving()) MovementUtils.strafe();
                            break;
                    }

                    if (mc.thePlayer.hurtTime >= 1 && mc.thePlayer.motionY > 0) {
                        mc.thePlayer.motionY -= 0.15;
                    }

                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();

                        if (MovementUtils.getSpeed() < 0.281) {
                            MovementUtils.strafe(0.281);
                        } else {
                            MovementUtils.strafe();
                        }
                    }
                }
                break;
            case "Intave":
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                }

                if (mc.thePlayer.motionY > 0.003 && mc.thePlayer.isSprinting()) {
                    mc.thePlayer.motionX *= 1f + (0.003 * iBoostMulti.get());
                    mc.thePlayer.motionX *= 1f + (0.003 * iBoostMulti.get());
                }
                break;
        }
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (!e.isPre() || !MovementUtils.isMoving()) {
            return;
        }

        ticks++;

        switch (mode.get()) {
            case "Strafe Hop":
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                }

                if ((mc.thePlayer.onGround && ground.get()) || (!mc.thePlayer.onGround && air.get())) {
                    if (smooth.get()) {
                        MovementUtils.smoothStrafe(e);
                    } else {
                        MovementUtils.strafe();
                    }
                }
                break;

            case "NCP":
                switch (ncpMode.get()) {
                    case "On tick 5":
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                            MovementUtils.strafe();
                        }

                        if (mc.thePlayer.offGroundTicks == 5) {
                            mc.thePlayer.motionY -= 0.1523351824467155;
                        }

                        if (mc.thePlayer.hurtTime >= 5 && mc.thePlayer.motionY >= 0) {
                            mc.thePlayer.motionY -= 0.1;
                        }

                        if (MovementUtils.isMoving()) {
                            mc.thePlayer.motionX *= 1.00718;
                            mc.thePlayer.motionZ *= 1.00718;
                        }

                        mc.timer.timerSpeed = 1f;
                        break;
                    case "Old BHop":
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                        }

                        if (MovementUtils.isMoving() && !mc.thePlayer.isInWater()) {
                            double spd = 0.0025 * speedMulti.get();
                            double m = (float) (Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ) + spd);
                            MovementUtils.bop(m);
                        }

                        if (mc.thePlayer.offGroundTicks == 4) {
                            mc.thePlayer.motionY -= 0.09800000190734863;
                        }

                        if (MovementUtils.getSpeed() < 0.312866806998394775 && movingTicks > 15) {
                            MovementUtils.strafe(0.312866806998394775);
                        }

                        if (MovementUtils.isMoving()) {
                            float timerSpeed = (float) (1.337 - MovementUtils.getSpeed());

                            if (timerSpeed > 1.5) timerSpeed = 1.5f;
                            if (timerSpeed < 0.6) timerSpeed = 0.6f;
                            mc.timer.timerSpeed = timerSpeed;
                        }
                        break;
                }
                break;
            case "Legit":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), mc.thePlayer.onGround && MovementUtils.isMoving());
                break;
        }
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        if (!MovementUtils.isMoving()) {
            return;
        }

        if (mode.get().equals("Verus")) {
            if (verusMode.get().equals("Low")) {
                if (ticks % 12 == 0 && mc.thePlayer.onGround) {
                    MovementUtils.strafe(0.69);
                    e.setY(0.42F);
                    mc.thePlayer.motionY = -(mc.thePlayer.posY - roundToOnGround(mc.thePlayer.posY));
                } else {
                    if (mc.thePlayer.onGround) {
                        MovementUtils.strafe(1.01);
                    } else {
                        MovementUtils.strafe(0.41);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        if (mode.is("Intave")) {
            e.setMotionY(0.42f - 1.7E-14f);
        }
    }

    public static double roundToOnGround(final double posY) {
        return posY - (posY % 0.015625);
    }
}