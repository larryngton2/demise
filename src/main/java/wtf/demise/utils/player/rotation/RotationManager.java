package wtf.demise.utils.player.rotation;

import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.SmoothMode;

import static java.lang.Math.*;
import static wtf.demise.utils.player.rotation.RotationUtils.getAngleDifference;
import static wtf.demise.utils.player.rotation.RotationUtils.getRotationDifference;

public class RotationManager implements InstanceAccess {
    public static float[] currentRotation = new float[]{}, serverRotation = new float[]{}, previousRotation = null;
    public static float lastDelta;
    public static boolean enabled;
    private static float[] targetRotation;
    private static boolean silent;
    private static float cachedHSpeed;
    private static float cachedVSpeed;
    private static SmoothMode smoothMode;
    public static boolean cachedCorrection;
    public static float rotDiffBuildUp;
    public static boolean reset;
    private static final TimerUtils tickTimer = new TimerUtils();
    private static final TimerUtils tickTimer1 = new TimerUtils();
    private long lastFrameTime = System.nanoTime();
    private float timeScale;
    private int previousDeltaYaw = 0;
    private int previousDeltaPitch = 0;
    private static boolean cachedAccel;
    private static float cachedAccelFactorYaw;
    private static float cachedAccelFactorPitch;
    private int interpolatedAccelDeltaYaw;
    private int interpolatedAccelDeltaPitch;

    public RotationManager() {
        enabled = true;
        smoothMode = SmoothMode.None;
        cachedCorrection = true;
        cachedHSpeed = 180;
        cachedVSpeed = 180;
        targetRotation = new float[]{0, 0};
        silent = true;
    }

    public static void setRotation(float[] rotation, boolean correction, float[] speed, boolean accel, float[] accelFactor, SmoothMode smoothMode, boolean silent) {
        if (tickTimer.hasTimeElapsed(50)) {
            lastDelta = getRotationDifference(currentRotation, previousRotation);

            tickTimer.reset();
        }

        RotationManager.targetRotation = rotation;
        RotationManager.silent = silent;
        RotationManager.cachedHSpeed = speed[0];
        RotationManager.cachedVSpeed = speed[1];
        RotationManager.smoothMode = smoothMode;
        RotationManager.cachedCorrection = correction;
        RotationManager.cachedAccel = accel;
        RotationManager.cachedAccelFactorYaw = accelFactor[0];
        RotationManager.cachedAccelFactorPitch = accelFactor[1];
        enabled = true;
    }

    private boolean shouldCorrect() {
        return shouldRotate() && cachedCorrection;
    }

    @EventTarget
    private void onMove(MoveInputEvent e) {
        if (shouldCorrect()) {
            MoveUtil.fixMovement(e, currentRotation[0]);
        }
    }

    @EventTarget
    private void onStrafe(StrafeEvent e) {
        if (shouldCorrect()) {
            e.setYaw(currentRotation[0]);
        }
    }

    @EventTarget
    private void onJump(JumpEvent event) {
        if (shouldCorrect()) {
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
        currentRotation = mc.thePlayer.getRotation();
        targetRotation = mc.thePlayer.getRotation();
    }

    @EventTarget
    @EventPriority(-100)
    public void onPacket(final PacketEvent e) {
        if (!(e.getPacket() instanceof C03PacketPlayer packetPlayer)) return;

        if (!packetPlayer.rotating) {
            rotDiffBuildUp = 0;
            return;
        }

        if (shouldRotate()) {
            packetPlayer.yaw = currentRotation[0];
            packetPlayer.pitch = currentRotation[1];
        }

        if (serverRotation != null && shouldRotate()) {
            float diff = getAngleDifference(packetPlayer.getYaw(), serverRotation[0]);
            rotDiffBuildUp += diff;
        }

        serverRotation = new float[]{packetPlayer.yaw, packetPlayer.pitch};
    }

    @EventTarget
    public void onMouseMove(MouseMoveEvent e) {
        if (currentRotation == null) {
            currentRotation = mc.thePlayer.getRotation();
        }

        if (e.getState() == MouseMoveEvent.State.PRE) {
            if (lastFrameTime == 0L) {
                lastFrameTime = System.nanoTime();
            }

            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
            lastFrameTime = currentTime;

            timeScale = deltaTime * 120;
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
        return currentRotation != null;
    }

    private void handleRotation(MouseMoveEvent e, float[] target) {
        float[] delta = toFloats(limitRotations(currentRotation, target));
        int[] scaledDelta = new int[]{(int) (delta[0] * timeScale), (int) (delta[1] * timeScale)};

        if (!silent) {
            e.setDeltaX(scaledDelta[0]);
            e.setDeltaY(scaledDelta[1]);

            currentRotation = mc.thePlayer.getRotation();
        } else {
            float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;

            float yawStep = scaledDelta[0] * f1;
            float pitchStep = scaledDelta[1] * f1;

            currentRotation[0] += yawStep * 0.15f;
            currentRotation[1] -= pitchStep * 0.15f;

            currentRotation[1] = MathHelper.clamp_float(currentRotation[1], -90, 90);
        }

        reset = false;
    }

    private int[] limitRotations(float[] current, float[] target) {
        float yawDifference = getAngleDifference(target[0], current[0]);
        float pitchDifference = getAngleDifference(target[1], current[1]);

        double rotationDifference = hypot(abs(yawDifference), abs(pitchDifference));

        float straightLineYaw = (float) (abs(yawDifference / rotationDifference) * cachedHSpeed);
        float straightLinePitch = (float) (abs(pitchDifference / rotationDifference) * cachedVSpeed);

        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;

        int[] finalTargetRotation = new int[]{
                (int) (max(-straightLineYaw, min(straightLineYaw, yawDifference)) / f1),
                (int) -(max(-straightLinePitch, min(straightLinePitch, pitchDifference)) / f1)
        };

        int[] delta;

        switch (smoothMode) {
            case Relative -> {
                float factorH = (float) max(min(rotationDifference / 180 * cachedHSpeed, 180), MathUtils.randomizeFloat(4, 6));
                float factorV = (float) max(min(rotationDifference / 180 * cachedVSpeed, 180), MathUtils.randomizeFloat(4, 6));

                float[] factor = new float[]{factorH, factorV};

                straightLineYaw = straightLineYaw / cachedHSpeed * factor[0];
                straightLinePitch = straightLinePitch / cachedVSpeed * factor[1];

                delta = new int[]{
                        (int) (max(-straightLineYaw, min(straightLineYaw, yawDifference)) / f1),
                        (int) -(max(-straightLinePitch, min(straightLinePitch, pitchDifference)) / f1)
                };
            }

            case Polar -> {
                // the method
                straightLineYaw = (float) (abs(yawDifference / rotationDifference) * 78);
                straightLinePitch = (float) (abs(pitchDifference / rotationDifference) * 24);

                delta = new int[]{
                        (int) (max(-straightLineYaw, min(straightLineYaw, yawDifference)) / f1),
                        (int) -(max(-straightLinePitch, min(straightLinePitch, pitchDifference)) / f1)
                };
            }

            default -> delta = finalTargetRotation;
        }

        if (cachedAccel) {
            float[] factors = new float[]{cachedAccelFactorYaw, cachedAccelFactorPitch};

            // nothing else worked ok
            // 16.67 is roughly the frame delay of 60 fps
            // if you don't get > 60 fps, please don't consider using this client
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