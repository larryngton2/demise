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
    private static float cachedMidpoint;
    private static SmoothMode smoothMode;
    public static boolean cachedCorrection;
    public static float rotDiffBuildUp;
    public static boolean reset;
    private static final TimerUtils tickTimer = new TimerUtils();
    private long lastFrameTime = System.nanoTime();
    private float timeScale;

    public static void setRotation(float[] rotation, boolean correction, float hSpeed, float vSpeed, float midpoint, SmoothMode smoothMode, boolean silent) {
        if (tickTimer.hasTimeElapsed(50)) {
            lastDelta = getRotationDifference(currentRotation, previousRotation);

            tickTimer.reset();
        }

        RotationManager.targetRotation = rotation;
        RotationManager.silent = silent;
        RotationManager.cachedHSpeed = hSpeed;
        RotationManager.cachedVSpeed = vSpeed;
        RotationManager.cachedMidpoint = midpoint;
        RotationManager.smoothMode = smoothMode;
        RotationManager.cachedCorrection = correction;
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
        int[] delta = limitRotations(currentRotation, target);

        if (!silent) {
            e.setDeltaX((int) (delta[0] * timeScale));
            e.setDeltaY((int) (delta[1] * timeScale));

            currentRotation = mc.thePlayer.getRotation();
        } else {
            float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;

            float yawStep = delta[0] * f1 * timeScale;
            float pitchStep = delta[1] * f1 * timeScale;

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

            case Bezier -> {
                float yawDirection = yawDifference / (float) rotationDifference;
                float pitchDirection = pitchDifference / (float) rotationDifference;

                float controlYaw = yawDirection * cachedMidpoint * (float) rotationDifference;
                float controlPitch = pitchDirection * cachedMidpoint * (float) rotationDifference;

                float[] t = new float[]{cachedHSpeed / 180, cachedVSpeed / 180};

                float finalYaw = (1 - t[0]) * (1 - t[0]) * current[0] + 2 * (1 - t[0]) * t[0] * controlYaw + t[0] * t[0] * finalTargetRotation[0];
                float finalPitch = (1 - t[1]) * (1 - t[1]) * current[1] + 2 * (1 - t[1]) * t[1] * controlPitch + t[1] * t[1] * finalTargetRotation[1];

                //todo fix bezier rots having down syndrome
                delta = new int[]{
                        (int) ((finalYaw - current[0]) / f1),
                        (int) -((finalPitch - current[1]) / f1)
                };
            }

            default -> delta = finalTargetRotation;
        }

        return delta;
    }
}