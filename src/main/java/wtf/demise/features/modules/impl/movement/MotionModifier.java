package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "MotionModifier", description = "Testing module for speeds. Good luck trying to configure this.", category = ModuleCategory.Movement)
public class MotionModifier extends Module {
    private final BoolValue motionMulti = new BoolValue("Motion multi", false, this);

    private final BoolValue air = new BoolValue("On air", true, this, motionMulti::get);
    private final SliderValue airXZ1 = new SliderValue("XZ multi (air tick 1)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ2 = new SliderValue("XZ multi (air tick 2)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ3 = new SliderValue("XZ multi (air tick 3)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ4 = new SliderValue("XZ multi (air tick 4)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ5 = new SliderValue("XZ multi (air tick 5)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ6 = new SliderValue("XZ multi (air tick 6)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ7 = new SliderValue("XZ multi (air tick 7)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ8 = new SliderValue("XZ multi (air tick 8)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ9 = new SliderValue("XZ multi (air tick 9)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ10 = new SliderValue("XZ multi (air tick 10)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ11 = new SliderValue("XZ multi (air tick 11)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());
    private final SliderValue airXZ12 = new SliderValue("XZ multi (air tick 12)", 1, 0, 2, 0.01f, this, () -> air.get() && motionMulti.get());

    private final BoolValue ground = new BoolValue("On ground", false, this, motionMulti::get);
    private final SliderValue groundXZ1 = new SliderValue("XZ multi (ground tick 1)", 1, 0, 2, 0.01f, this, () -> ground.get() && motionMulti.get());
    private final SliderValue groundXZ2 = new SliderValue("XZ multi (ground tick 2)", 1, 0, 2, 0.01f, this, () -> ground.get() && motionMulti.get());
    private final SliderValue groundXZ3 = new SliderValue("XZ multi (ground tick 3)", 1, 0, 2, 0.01f, this, () -> ground.get() && motionMulti.get());
    private final SliderValue groundXZ4 = new SliderValue("XZ multi (ground tick 4)", 1, 0, 2, 0.01f, this, () -> ground.get() && motionMulti.get());
    private final SliderValue groundXZ5 = new SliderValue("XZ multi (ground tick 5)", 1, 0, 2, 0.01f, this, () -> ground.get() && motionMulti.get());

    private final BoolValue pulldownOnAir = new BoolValue("Pulldown on air", false, this);
    private final SliderValue pulldown1 = new SliderValue("Pulldown (tick 1)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown2 = new SliderValue("Pulldown (tick 2)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown3 = new SliderValue("Pulldown (tick 3)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown4 = new SliderValue("Pulldown (tick 4)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown5 = new SliderValue("Pulldown (tick 5)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown6 = new SliderValue("Pulldown (tick 6)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown7 = new SliderValue("Pulldown (tick 7)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown8 = new SliderValue("Pulldown (tick 8)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown9 = new SliderValue("Pulldown (tick 9)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown10 = new SliderValue("Pulldown (tick 10)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown11 = new SliderValue("Pulldown (tick 11)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);
    private final SliderValue pulldown12 = new SliderValue("Pulldown (tick 12)", 0, 0, 1, 0.01f, this, pulldownOnAir::get);

    private final BoolValue strafe = new BoolValue("Strafe", false, this);
    private final SliderValue strafe1 = new SliderValue("Strafe (tick 1)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe2 = new SliderValue("Strafe (tick 2)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe3 = new SliderValue("Strafe (tick 3)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe4 = new SliderValue("Strafe (tick 4)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe5 = new SliderValue("Strafe (tick 5)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe6 = new SliderValue("Strafe (tick 6)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe7 = new SliderValue("Strafe (tick 7)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe8 = new SliderValue("Strafe (tick 8)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe9 = new SliderValue("Strafe (tick 9)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe10 = new SliderValue("Strafe (tick 10)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe11 = new SliderValue("Strafe (tick 11)", 0, -0.05f, 1, 0.01f, this, strafe::get);
    private final SliderValue strafe12 = new SliderValue("Strafe (tick 12)", 0, -0.05f, 1, 0.01f, this, strafe::get);

    private final BoolValue autoJump = new BoolValue("Auto jump", true, this);
    private final SliderValue jumpHeight = new SliderValue("Jump height", 0.42f, 0, 1, 0.01f, this, autoJump::get);
    private final SliderValue jumpOff = new SliderValue("Jumpoff", 0.2f, 0, 1, 0.01f, this, autoJump::get);

    private final SliderValue timer = new SliderValue("Timer", 1, 0.1f, 2, 0.01f, this);

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        mc.timer.timerSpeed = timer.get();

        if (motionMulti.get()) {
            if (air.get()) {
                switch (mc.thePlayer.offGroundTicks) {
                    case 1:
                        multiXZ(airXZ1.get());
                        break;
                    case 2:
                        multiXZ(airXZ2.get());
                        break;
                    case 3:
                        multiXZ(airXZ3.get());
                        break;
                    case 4:
                        multiXZ(airXZ4.get());
                        break;
                    case 5:
                        multiXZ(airXZ5.get());
                        break;
                    case 6:
                        multiXZ(airXZ6.get());
                        break;
                    case 7:
                        multiXZ(airXZ7.get());
                        break;
                    case 8:
                        multiXZ(airXZ8.get());
                        break;
                    case 9:
                        multiXZ(airXZ9.get());
                        break;
                    case 10:
                        multiXZ(airXZ10.get());
                        break;
                    case 11:
                        multiXZ(airXZ11.get());
                        break;
                    case 12:
                        multiXZ(airXZ12.get());
                        break;
                }
            }

            if (ground.get()) {
                switch (mc.thePlayer.onGroundTicks) {
                    case 1:
                        multiXZ(groundXZ1.get());
                        break;
                    case 2:
                        multiXZ(groundXZ2.get());
                        break;
                    case 3:
                        multiXZ(groundXZ3.get());
                        break;
                    case 4:
                        multiXZ(groundXZ4.get());
                        break;
                    case 5:
                        multiXZ(groundXZ5.get());
                        break;
                }
            }
        }

        if (pulldownOnAir.get()) {
            switch (mc.thePlayer.offGroundTicks) {
                case 1:
                    mc.thePlayer.motionY -= pulldown1.get();
                    break;
                case 2:
                    mc.thePlayer.motionY -= pulldown2.get();
                    break;
                case 3:
                    mc.thePlayer.motionY -= pulldown3.get();
                    break;
                case 4:
                    mc.thePlayer.motionY -= pulldown4.get();
                    break;
                case 5:
                    mc.thePlayer.motionY -= pulldown5.get();
                    break;
                case 6:
                    mc.thePlayer.motionY -= pulldown6.get();
                    break;
                case 7:
                    mc.thePlayer.motionY -= pulldown7.get();
                    break;
                case 8:
                    mc.thePlayer.motionY -= pulldown8.get();
                    break;
                case 9:
                    mc.thePlayer.motionY -= pulldown9.get();
                    break;
                case 10:
                    mc.thePlayer.motionY -= pulldown10.get();
                    break;
                case 11:
                    mc.thePlayer.motionY -= pulldown11.get();
                    break;
                case 12:
                    mc.thePlayer.motionY -= pulldown12.get();
                    break;
            }
        }

        if (strafe.get()) {
            switch (mc.thePlayer.offGroundTicks) {
                case 1:
                    realStrafe(strafe1.get());
                    break;
                case 2:
                    realStrafe(strafe2.get());
                    break;
                case 3:
                    realStrafe(strafe3.get());
                    break;
                case 4:
                    realStrafe(strafe4.get());
                    break;
                case 5:
                    realStrafe(strafe5.get());
                    break;
                case 6:
                    realStrafe(strafe6.get());
                    break;
                case 7:
                    realStrafe(strafe7.get());
                    break;
                case 8:
                    realStrafe(strafe8.get());
                    break;
                case 9:
                    realStrafe(strafe9.get());
                    break;
                case 10:
                    realStrafe(strafe10.get());
                    break;
                case 11:
                    realStrafe(strafe11.get());
                    break;
                case 12:
                    realStrafe(strafe12.get());
                    break;
            }
        }

        if (autoJump.get()) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
        }
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        if (autoJump.get()) {
            e.setMotionY(jumpHeight.get());
            e.setJumpoff(jumpOff.get());
        }
    }

    private void multiXZ(float amount) {
        mc.thePlayer.motionX *= amount;
        mc.thePlayer.motionZ *= amount;
    }

    private void realStrafe(float amount) {
        if (amount > 0) {
            MoveUtil.strafe(amount);
        } else if (amount == 0) {
            MoveUtil.strafe();
        }
    }
}
