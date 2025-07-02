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
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.SmoothMode;

import static java.lang.Math.*;
import static wtf.demise.utils.player.rotation.RotationUtils.getAngleDifference;
import static wtf.demise.utils.player.rotation.RotationUtils.getRotationDifference;

public class RotationHandler implements InstanceAccess {
    public static float[] currentRotation = new float[]{}, serverRotation = new float[]{}, previousRotation = null;
    public static float lastDelta;
    public static boolean enabled;
    public static float[] targetRotation;
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
    private static int previousDeltaYaw = 0;
    private static int previousDeltaPitch = 0;
    private static boolean cachedAccel;
    private static float cachedAccelFactorYaw;
    private static float cachedAccelFactorPitch;
    private static int interpolatedAccelDeltaYaw;
    private static int interpolatedAccelDeltaPitch;
    private static boolean isSmooth;

    public RotationHandler() {
        enabled = true;
        smoothMode = SmoothMode.Linear;
        cachedCorrection = true;
        cachedHSpeed = 180;
        cachedVSpeed = 180;
        targetRotation = currentRotation = new float[]{0, 0};
        silent = true;
        cachedAccel = false;
        cachedAccelFactorYaw = 0;
        cachedAccelFactorPitch = 0;
        isSmooth = true;
    }

    public static void setBasicRotation(float[] rotation, boolean correction, float hSpeed, float vSpeed) {
        RotationHandler.targetRotation = rotation;
        RotationHandler.silent = true;
        RotationHandler.cachedHSpeed = hSpeed;
        RotationHandler.cachedVSpeed = vSpeed;
        RotationHandler.smoothMode = SmoothMode.Linear;
        RotationHandler.cachedCorrection = correction;
        RotationHandler.cachedAccel = false;
        RotationHandler.cachedAccelFactorYaw = 0;
        RotationHandler.cachedAccelFactorPitch = 0;
        RotationHandler.isSmooth = false;
        enabled = true;
    }

    public static void setRotation(float[] rotation, boolean correction, float[] speed, boolean accel, float[] accelFactor, SmoothMode smoothMode, boolean silent, boolean smooth) {
        if (tickTimer.hasTimeElapsed(50)) {
            lastDelta = getRotationDifference(currentRotation, previousRotation);

            tickTimer.reset();
        }

        RotationHandler.targetRotation = rotation;
        RotationHandler.silent = silent;
        RotationHandler.cachedHSpeed = speed[0];
        RotationHandler.cachedVSpeed = speed[1];
        RotationHandler.smoothMode = smoothMode;
        RotationHandler.cachedCorrection = correction;
        RotationHandler.cachedAccel = accel;
        RotationHandler.cachedAccelFactorYaw = accelFactor[0];
        RotationHandler.cachedAccelFactorPitch = accelFactor[1];
        RotationHandler.isSmooth = smooth;
        enabled = true;
    }

    private boolean shouldCorrect() {
        return shouldRotate() && cachedCorrection;
    }

    @EventTarget
    private void onMove(MoveInputEvent e) {
        if (shouldCorrect()) {
            MoveUtil.fixMovement(e, currentRotation[0]);
        } else if (shouldRotate() && abs(getAngleDifference((float) toDegrees(MoveUtil.getDirection()), currentRotation[0])) > 90 && !mc.thePlayer.omniSprint && !Demise.INSTANCE.getModuleManager().getModule(Scaffold.class).isEnabled()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
            mc.thePlayer.setSprinting(false);
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
        return currentRotation != null && targetRotation != null;
    }

    private void handleRotation(MouseMoveEvent e, float[] target) {
        float hSpeed = cachedHSpeed;
        float vSpeed = cachedVSpeed;

        if (!isSmooth) {
            // because rotations become too fucking fast
            hSpeed *= mc.timer.partialTicks / 2;
            vSpeed *= mc.timer.partialTicks / 2;

            hSpeed = MathHelper.clamp_float(hSpeed, 0, 180);
            vSpeed = MathHelper.clamp_float(vSpeed, 0, 180);
        }

        float[] delta = toFloats(limitRotations(currentRotation, target, hSpeed, vSpeed));
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;

        delta[0] = Math.max(Math.abs(delta[0]), f1) * Math.signum(delta[0]);
        delta[1] = Math.max(Math.abs(delta[1]), f1) * Math.signum(delta[1]);

        if (isSmooth) {
            int[] scaledDelta = new int[]{(int) (delta[0] * timeScale), (int) (delta[1] * timeScale)};

            if (!silent) {
                e.setDeltaX(scaledDelta[0]);
                e.setDeltaY(scaledDelta[1]);
                currentRotation = mc.thePlayer.getRotation();
            } else {
                float yawStep = scaledDelta[0] * f1;
                float pitchStep = scaledDelta[1] * f1;

                currentRotation[0] += yawStep * 0.15f;
                currentRotation[1] -= pitchStep * 0.15f;
                currentRotation[1] = MathHelper.clamp_float(currentRotation[1], -90, 90);
            }
        } else {
            if (!silent) {
                mc.thePlayer.rotationYaw += delta[0] * f1;
                mc.thePlayer.rotationPitch -= delta[1] * f1;
                currentRotation = mc.thePlayer.getRotation();
            } else {
                currentRotation[0] += delta[0] * f1;
                currentRotation[1] -= delta[1] * f1;
                currentRotation[1] = MathHelper.clamp_float(currentRotation[1], -90, 90);
            }
        }

        reset = false;
    }

    private int[] limitRotations(float[] current, float[] target, float hSpeed, float vSpeed) {
        float yawDifference = getAngleDifference(target[0], current[0]);
        float pitchDifference = getAngleDifference(target[1], current[1]);

        double rotationDifference = hypot(abs(yawDifference), abs(pitchDifference));

        float straightLineYaw = (float) (abs(yawDifference / rotationDifference) * hSpeed);
        float straightLinePitch = (float) (abs(pitchDifference / rotationDifference) * vSpeed);

        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float f1 = f * f * f * 8.0F;

        int[] finalTargetRotation = new int[]{
                (int) (max(-straightLineYaw, min(straightLineYaw, yawDifference)) / f1),
                (int) -(max(-straightLinePitch, min(straightLinePitch, pitchDifference)) / f1)
        };

        int[] delta;

        if (smoothMode.equals(SmoothMode.Relative)) {
            float factorH = (float) max(min(rotationDifference / 180 * hSpeed, 180), MathUtils.randomizeFloat(4, 6));
            float factorV = (float) max(min(rotationDifference / 180 * vSpeed, 180), MathUtils.randomizeFloat(4, 6));

            float[] factor = new float[]{factorH, factorV};

            straightLineYaw = straightLineYaw / hSpeed * factor[0];
            straightLinePitch = straightLinePitch / vSpeed * factor[1];

            delta = new int[]{
                    (int) (max(-straightLineYaw, min(straightLineYaw, yawDifference)) / f1),
                    (int) -(max(-straightLinePitch, min(straightLinePitch, pitchDifference)) / f1)
            };
        } else {
            delta = finalTargetRotation;
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