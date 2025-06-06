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
    private static boolean cachedPredictionFlick;
    private static SmoothMode smoothMode;
    public static boolean cachedCorrection;
    public static float rotDiffBuildUp;
    public static boolean reset;
    private static final TimerUtils tickTimer = new TimerUtils();

    public static void setRotation(float[] rotation, boolean correction, float hSpeed, float vSpeed, float midpoint, boolean predictionFlick, SmoothMode smoothMode, boolean silent) {
        if (tickTimer.hasTimeElapsed(50)) {
            if (currentRotation != null && previousRotation != null) {
                lastDelta = getRotationDifference(currentRotation, previousRotation);
            } else {
                lastDelta = getRotationDifference(new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, new float[]{mc.thePlayer.prevRotationYaw, mc.thePlayer.prevRotationPitch});
            }

            tickTimer.reset();
        }

        RotationManager.targetRotation = rotation;
        RotationManager.silent = silent;
        RotationManager.cachedHSpeed = hSpeed;
        RotationManager.cachedVSpeed = vSpeed;
        RotationManager.cachedMidpoint = midpoint;
        RotationManager.cachedPredictionFlick = predictionFlick;
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

    @EventPriority(Integer.MIN_VALUE)
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
        int[] delta = limitRotations(silent ? currentRotation : mc.thePlayer.getRotation(), target);

        if (!silent) {
            e.setDeltaX(delta[0]);
            e.setDeltaY(delta[1]);

            currentRotation = mc.thePlayer.getRotation();
        } else {
            float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;

            float yawStep = (delta[0] * f1) * 0.15f;
            float pitchStep = (delta[1] * f1) * 0.15f;

            currentRotation[0] += yawStep;
            currentRotation[1] -= pitchStep;

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

        if (cachedPredictionFlick) {
            float[] rangeYaw = {0, 0};
            float[] rangePitch = {0, 0};

            if (lastDelta == 0) {
                float incYaw = 0.2f * MathHelper.clamp_float(straightLineYaw / 50, 0, 1);
                rangeYaw[0] = 0.1f + incYaw;
                rangeYaw[1] = 0.5f + incYaw;

                float incPitch = 0.2f * MathHelper.clamp_float(straightLinePitch / 50, 0, 1);
                rangePitch[0] = 0.1f + incPitch;
                rangePitch[1] = 0.5f + incPitch;
            } else {
                rangeYaw[0] = rangePitch[0] = 0.9f;
                rangeYaw[1] = rangePitch[1] = 1f;
            }

            float[] newRot = new float[]{MathUtils.interpolateNoUpdateCheck(lastDelta, straightLineYaw, MathUtils.randomizeFloat(rangeYaw[0], rangeYaw[1])), MathUtils.interpolateNoUpdateCheck(lastDelta, straightLinePitch, MathUtils.randomizeFloat(rangePitch[0], rangePitch[1]))};

            straightLineYaw = newRot[0];
            straightLinePitch = newRot[1];
        }

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