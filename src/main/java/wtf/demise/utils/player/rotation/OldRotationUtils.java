package wtf.demise.utils.player.rotation;

import net.minecraft.util.MathHelper;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.features.modules.impl.visual.Rotation;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.SmoothMode;

import java.util.Objects;

import static java.lang.Math.*;
import static wtf.demise.utils.player.rotation.RotationManager.*;
import static wtf.demise.utils.player.rotation.RotationUtils.getAngleDifference;
import static wtf.demise.utils.player.rotation.RotationUtils.getRotationDifference;

public class OldRotationUtils implements InstanceAccess {
    private static float lastDelta;
    public static float[] currRotRequireNonNullElse;
    public static float[] prevRotRequireNonNullElse;
    public static MovementCorrection currentCorrection = MovementCorrection.None;
    public static boolean enabled;
    private static float cachedHSpeed;
    private static float cachedVSpeed;
    private static float cachedMidpoint;
    private static boolean cachedAccel;
    private static SmoothMode smoothMode;
    private static final Rotation moduleRotation = Demise.INSTANCE.getModuleManager().getModule(Rotation.class);
    private boolean angleCalled;
    private static final TimerUtils tickTimer = new TimerUtils();

    public static void setRotation(float[] rotation, final MovementCorrection correction, float hSpeed, float vSpeed, float midpoint, boolean accel, SmoothMode smoothMode) {
        prevRotRequireNonNullElse = Objects.requireNonNullElse(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});

        if (tickTimer.hasTimeElapsed(50)) {
            if (currentRotation != null && previousRotation != null) {
                lastDelta = getRotationDifference(currentRotation, previousRotation);
            } else {
                lastDelta = getRotationDifference(new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, new float[]{mc.thePlayer.prevRotationYaw, mc.thePlayer.prevRotationPitch});
            }

            tickTimer.reset();
        }

        if (moduleRotation.silent.get()) {
            currentRotation = limitRotations(serverRotation, rotation, hSpeed, vSpeed, midpoint, accel, smoothMode);
        } else {
            mc.thePlayer.rotationYaw = limitRotations(serverRotation, rotation, hSpeed, vSpeed, midpoint, accel, smoothMode)[0];
            mc.thePlayer.rotationPitch = limitRotations(serverRotation, rotation, hSpeed, vSpeed, midpoint, accel, smoothMode)[1];
        }

        currentCorrection = correction;
        cachedHSpeed = hSpeed;
        cachedVSpeed = vSpeed;
        cachedMidpoint = midpoint;
        cachedAccel = accel;
        OldRotationUtils.smoothMode = smoothMode;
        if (smoothMode != SmoothMode.None) {
            cachedCorrection = true;
        }
        enabled = true;
        RotationManager.enabled = true;
        currRotRequireNonNullElse = Objects.requireNonNullElse(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});
    }

    @EventPriority(Integer.MIN_VALUE)
    @EventTarget
    public void onLook(LookEvent e) {
        if (enabled) {
            e.rotation = currentRotation;
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        currentRotation = mc.thePlayer.getRotation();
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        angleCalled = true;
    }

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        double distanceToPlayerRotation = getRotationDifference(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});

        if (!enabled && angleCalled && RotationManager.reset) {
            if (distanceToPlayerRotation < 1) {
                resetRotation();
                return;
            }

            if (distanceToPlayerRotation > 0) {
                float finalHSpeed = (cachedHSpeed / 2) * mc.timer.partialTicks;
                float finalVSpeed = (cachedVSpeed / 2) * mc.timer.partialTicks;

                currentRotation = limitRotations(Objects.requireNonNullElse(currentRotation, serverRotation), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, finalHSpeed, finalVSpeed, cachedMidpoint, cachedAccel, smoothMode);
            }
        }

        enabled = false;
        angleCalled = false;
    }

    private static void resetRotation() {
        enabled = false;
        currentRotation = null;
        currentCorrection = MovementCorrection.None;
    }

    public static float[] limitRotations(float[] currentRotation, float[] targetRotation, float hSpeed, float vSpeed, float midpoint, boolean accel, SmoothMode smoothMode) {
        float[] finalRotation;

        float yawDifference = getAngleDifference(targetRotation[0], currentRotation[0]);
        float pitchDifference = getAngleDifference(targetRotation[1], currentRotation[1]);

        double rotationDifference = hypot(abs(yawDifference), abs(pitchDifference));

        float straightLineYaw = (float) (abs(yawDifference / rotationDifference) * hSpeed);
        float straightLinePitch = (float) (abs(pitchDifference / rotationDifference) * vSpeed);

        if (accel) {
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
                rangeYaw[0] = rangePitch[0] = 0.3f;
                rangeYaw[1] = rangePitch[1] = 0.7f;
            }

            float[] newRot = new float[]{MathUtils.interpolateNoUpdateCheck(lastDelta, straightLineYaw, MathUtils.randomizeFloat(rangeYaw[0], rangeYaw[1])), MathUtils.interpolateNoUpdateCheck(lastDelta, straightLinePitch, MathUtils.randomizeFloat(rangePitch[0], rangePitch[1]))};

            straightLineYaw = newRot[0];
            straightLinePitch = newRot[1];
        }

        float[] finalTargetRotation = new float[]{
                currentRotation[0] + max(-straightLineYaw, min(straightLineYaw, yawDifference)),
                currentRotation[1] + max(-straightLinePitch, min(straightLinePitch, pitchDifference))
        };

        switch (smoothMode) {
            case Linear -> finalRotation = applyGCDFix(currentRotation, finalTargetRotation);

            case Relative -> {
                float factorH = (float) max(min(rotationDifference / 180 * hSpeed, 180), MathUtils.randomizeFloat(4, 6));
                float factorV = (float) max(min(rotationDifference / 180 * vSpeed, 180), MathUtils.randomizeFloat(4, 6));

                float[] factor = new float[]{factorH, factorV};

                float straightLineYaw1 = (float) (abs(yawDifference / rotationDifference) * factor[0]);
                float straightLinePitch1 = (float) (abs(pitchDifference / rotationDifference) * factor[1]);

                float[] smoothedRotation = new float[]{
                        currentRotation[0] + max(-straightLineYaw1, min(straightLineYaw1, yawDifference)),
                        currentRotation[1] + max(-straightLinePitch1, min(straightLinePitch1, pitchDifference))
                };

                finalRotation = applyGCDFix(currentRotation, smoothedRotation);
            }

            case Bezier -> {
                float yawDirection = yawDifference / (float) rotationDifference;
                float pitchDirection = pitchDifference / (float) rotationDifference;

                float controlYaw = currentRotation[0] + yawDirection * midpoint * (float) rotationDifference;
                float controlPitch = currentRotation[1] + pitchDirection * midpoint * (float) rotationDifference;

                float[] t = new float[]{hSpeed / 180, vSpeed / 180};

                float finalYaw = (1 - t[0]) * (1 - t[0]) * currentRotation[0] + 2 * (1 - t[0]) * t[0] * controlYaw + t[0] * t[0] * finalTargetRotation[0];
                float finalPitch = (1 - t[1]) * (1 - t[1]) * currentRotation[1] + 2 * (1 - t[1]) * t[1] * controlPitch + t[1] * t[1] * finalTargetRotation[1];

                float[] smoothedRotation = new float[]{finalYaw, finalPitch};

                finalRotation = applyGCDFix(currentRotation, smoothedRotation);
            }

            default -> finalRotation = targetRotation;
        }

        return finalRotation;
    }

    public static float[] applyGCDFix(float[] prevRotation, float[] currentRotation) {
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;
        float yawDelta = currentRotation[0] - prevRotation[0];
        float pitchDelta = currentRotation[1] - prevRotation[1];

        float f1 = round(yawDelta / gcd) * gcd;
        float f2 = round(pitchDelta / gcd) * gcd;

        float yaw = prevRotation[0] + f1;
        float pitch = prevRotation[1] + f2;

        return new float[]{yaw, pitch};
    }
}