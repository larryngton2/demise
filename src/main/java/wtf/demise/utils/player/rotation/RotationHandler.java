package wtf.demise.utils.player.rotation;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;

import static java.lang.Math.*;
import static wtf.demise.utils.player.rotation.RotationUtils.getAngleDifference;
import static wtf.demise.utils.player.rotation.RotationUtils.getRotationDifference;

public class RotationHandler implements InstanceAccess {
    public static float[] currentRotation = new float[]{}, serverRotation = new float[]{}, previousRotation = null;
    public static float lastDelta;
    public static boolean enabled;
    public static float[] targetRotation;
    private static boolean silent;
    private static float hSpeed;
    private static float vSpeed;
    public static MovementCorrectionMode correction;
    public static float rotDiffBuildUp;
    public static boolean reset;
    private static final TimerUtils tickTimer = new TimerUtils();
    private static final TimerUtils tickTimer1 = new TimerUtils();
    private long lastFrameTime = System.nanoTime();
    private float timeScale;
    private static int previousDeltaYaw = 0;
    private static int previousDeltaPitch = 0;
    private static boolean accel;
    private static float accelFactorYaw;
    private static float accelFactorPitch;
    private static int interpolatedAccelDeltaYaw;
    private static int interpolatedAccelDeltaPitch;
    private static float smoothingFactor;

    public RotationHandler() {
        enabled = true;
        correction = MovementCorrectionMode.Silent;
        hSpeed = 180;
        vSpeed = 180;
        targetRotation = currentRotation = new float[]{0, 0};
        reset = true;
        silent = true;
        accel = false;
        accelFactorYaw = 0;
        accelFactorPitch = 0;
        smoothingFactor = 1;
    }

    public static void setBasicRotation(float[] rotation, boolean correction, float hSpeed, float vSpeed) {
        RotationHandler.targetRotation = rotation;
        RotationHandler.silent = true;
        RotationHandler.hSpeed = hSpeed;
        RotationHandler.vSpeed = vSpeed;
        RotationHandler.correction = correction ? MovementCorrectionMode.Silent : MovementCorrectionMode.None;
        RotationHandler.accel = false;
        RotationHandler.accelFactorYaw = 0;
        RotationHandler.accelFactorPitch = 0;
        smoothingFactor = 1;
        enabled = true;
    }

    public static void setRotation(float[] rotation, MovementCorrectionMode correction, float[] speed, boolean accel, float[] accelFactor, boolean silent, float smoothingFactor) {
        if (tickTimer.hasTimeElapsed(50)) {
            lastDelta = getRotationDifference(currentRotation, previousRotation);

            tickTimer.reset();
        }

        RotationHandler.targetRotation = rotation;
        RotationHandler.silent = silent;
        RotationHandler.hSpeed = speed[0];
        RotationHandler.vSpeed = speed[1];
        RotationHandler.correction = correction;
        RotationHandler.accel = accel;
        RotationHandler.accelFactorYaw = accelFactor[0];
        RotationHandler.accelFactorPitch = accelFactor[1];
        RotationHandler.smoothingFactor = smoothingFactor;
        enabled = true;
    }

    @EventTarget
    private void onMove(MoveInputEvent e) {
        if (shouldRotate()) {
            if (correction == MovementCorrectionMode.Silent) {
                MoveUtil.fixMovement(e, currentRotation[0]);
            } else if (abs(getAngleDifference((float) toDegrees(MoveUtil.getDirection()), currentRotation[0])) > 90 && !mc.thePlayer.omniSprint && !Demise.INSTANCE.getModuleManager().getModule(Scaffold.class).isEnabled()) {
                if (correction == MovementCorrectionMode.None) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
                    mc.thePlayer.setSprinting(false);
                }
            }
        }
    }

    @EventTarget
    private void onStrafe(StrafeEvent e) {
        if (correction != MovementCorrectionMode.None && shouldRotate()) {
            e.setYaw(currentRotation[0]);
        }
    }

    @EventTarget
    private void onJump(JumpEvent event) {
        if (correction != MovementCorrectionMode.None && shouldRotate()) {
            event.setYaw(currentRotation[0]);
        }
    }

    @EventTarget
    public void onLook(LookEvent e) {
        if (shouldRotate()) {
            e.rotation = currentRotation;
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        currentRotation = targetRotation = mc.thePlayer.getRotation();
        reset = true;
    }

    @EventTarget
    @EventPriority(-100)
    public void onPacket(final PacketEvent e) {
        if (!(e.getPacket() instanceof C03PacketPlayer packetPlayer)) return;
        serverRotation = new float[]{packetPlayer.yaw, packetPlayer.pitch};

        if (!packetPlayer.rotating) {
            rotDiffBuildUp = 0;
            return;
        }

        if (shouldRotate()) {
            packetPlayer.yaw = currentRotation[0];
            packetPlayer.pitch = currentRotation[1];

            float diff = getAngleDifference(packetPlayer.getYaw(), serverRotation[0]);
            rotDiffBuildUp += diff;
        }
    }

    @EventTarget
    public void onMouseMove(MouseMoveEvent e) {
        if (currentRotation == null) {
            currentRotation = mc.thePlayer.getRotation();
        }

        // normalise rotation deltas to 60 fps
        // in other words, makes the rotation speed be equivalent to having 60 fps
        if (e.getState() == MouseMoveEvent.State.PRE) {
            if (lastFrameTime == 0L) {
                lastFrameTime = System.nanoTime();
            }

            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
            lastFrameTime = currentTime;

            // 45 is the min fps you can get without the rotation breaking
            deltaTime = Math.min(deltaTime, 1.0f / 45);
            timeScale = deltaTime * 60;
            return;
        }

        if (enabled) {
            handleRotation(e, targetRotation);
        } else {
            if (abs(RotationUtils.getRotationDifference(currentRotation, mc.thePlayer.getRotation())) < 1 || reset) {
                currentRotation = mc.thePlayer.getRotation();
                targetRotation = null;
                reset = true;
                previousDeltaYaw = previousDeltaPitch = 0;
            } else {
                handleRotation(e, mc.thePlayer.getRotation());
            }
        }

        enabled = false;
    }

    public static boolean shouldRotate() {
        return currentRotation != null && targetRotation != null;
    }

    private void handleRotation(MouseMoveEvent e, float[] target) {
        float[] delta = toFloats(limitRotations(target));

        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;

        delta[0] = Math.max(Math.abs(delta[0]), f1) * Math.signum(delta[0]);
        delta[1] = Math.max(Math.abs(delta[1]), f1) * Math.signum(delta[1]);

        int[] scaledDelta = new int[]{(int) (delta[0] * timeScale), (int) (delta[1] * timeScale)};

        float yawStep = scaledDelta[0] * f1;
        float pitchStep = scaledDelta[1] * f1;

        if (!silent) {
            e.setDeltaX(0);
            e.setDeltaY(0);
            mc.thePlayer.rotationYaw += yawStep * smoothingFactor;
            mc.thePlayer.rotationPitch -= pitchStep * smoothingFactor;
            mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90f, 90f);
            currentRotation = mc.thePlayer.getRotation();
        } else {
            currentRotation[0] += yawStep * smoothingFactor;
            currentRotation[1] -= pitchStep * smoothingFactor;
            currentRotation[1] = MathHelper.clamp_float(currentRotation[1], -90f, 90f);
        }

        reset = false;
    }

    private int[] limitRotations(float[] target) {
        float yawDifference = getAngleDifference(target[0], currentRotation[0]);
        float pitchDifference = getAngleDifference(target[1], currentRotation[1]);

        double rotationDifference = hypot(abs(yawDifference), abs(pitchDifference));

        float straightLineYaw = (float) (abs(yawDifference / rotationDifference) * hSpeed);
        float straightLinePitch = (float) (abs(pitchDifference / rotationDifference) * vSpeed);

        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;

        int[] delta = new int[]{
                (int) (max(-straightLineYaw, min(straightLineYaw, yawDifference)) / f1),
                (int) -(max(-straightLinePitch, min(straightLinePitch, pitchDifference)) / f1)
        };

        if (accel) {
            float[] factors = new float[]{accelFactorYaw, accelFactorPitch};

            // nothing else worked ok
            // 16.67 is roughly the frame delay of 60 fps
            if (tickTimer1.hasTimeElapsed((long) 16.67)) {
                interpolatedAccelDeltaYaw = (int) (delta[0] * (1 - factors[0]) + previousDeltaYaw * factors[0]);
                interpolatedAccelDeltaPitch = (int) (delta[1] * (1 - factors[1]) + previousDeltaPitch * factors[1]);
                tickTimer1.reset();
            }

            delta[0] = interpolatedAccelDeltaYaw;
            delta[1] = interpolatedAccelDeltaPitch;
        }

        previousDeltaYaw = delta[0];
        previousDeltaPitch = delta[1];

        return delta;
    }

    private float[] toFloats(int[] ints) {
        return new float[]{ints[0], ints[1]};
    }
}