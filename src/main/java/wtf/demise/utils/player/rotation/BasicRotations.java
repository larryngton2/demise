package wtf.demise.utils.player.rotation;

import wtf.demise.Demise;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.LookEvent;
import wtf.demise.features.modules.impl.visual.Rotation;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.TimerUtils;

import java.util.Objects;

import static java.lang.Math.*;
import static wtf.demise.utils.player.rotation.RotationManager.*;
import static wtf.demise.utils.player.rotation.RotationUtils.getAngleDifference;
import static wtf.demise.utils.player.rotation.RotationUtils.getRotationDifference;

public class BasicRotations implements InstanceAccess {
    public static float[] currRotRequireNonNullElse;
    public static float[] prevRotRequireNonNullElse;
    public static boolean currentCorrection;
    public static boolean enabled;
    private static float cachedHSpeed;
    private static float cachedVSpeed;
    private static final Rotation moduleRotation = Demise.INSTANCE.getModuleManager().getModule(Rotation.class);
    private boolean angleCalled;
    private static final TimerUtils tickTimer = new TimerUtils();

    public static void setRotation(float[] rotation, boolean correction, float hSpeed, float vSpeed) {
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
            currentRotation = limitRotations(serverRotation, rotation, hSpeed, vSpeed);
        } else {
            mc.thePlayer.rotationYaw = limitRotations(serverRotation, rotation, hSpeed, vSpeed)[0];
            mc.thePlayer.rotationPitch = limitRotations(serverRotation, rotation, hSpeed, vSpeed)[1];
        }

        currentCorrection = correction;
        cachedHSpeed = hSpeed;
        cachedVSpeed = vSpeed;
        cachedCorrection = correction;
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

                currentRotation = limitRotations(Objects.requireNonNullElse(currentRotation, serverRotation), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, finalHSpeed, finalVSpeed);
            }
        }

        enabled = false;
        angleCalled = false;
    }

    private static void resetRotation() {
        enabled = false;
        currentRotation = null;
        currentCorrection = false;
    }

    public static float[] limitRotations(float[] currentRotation, float[] targetRotation, float hSpeed, float vSpeed) {
        float[] finalRotation;

        float yawDifference = getAngleDifference(targetRotation[0], currentRotation[0]);
        float pitchDifference = getAngleDifference(targetRotation[1], currentRotation[1]);

        double rotationDifference = hypot(abs(yawDifference), abs(pitchDifference));

        float straightLineYaw = (float) (abs(yawDifference / rotationDifference) * hSpeed);
        float straightLinePitch = (float) (abs(pitchDifference / rotationDifference) * vSpeed);

        float[] finalTargetRotation = new float[]{
                currentRotation[0] + max(-straightLineYaw, min(straightLineYaw, yawDifference)),
                currentRotation[1] + max(-straightLinePitch, min(straightLinePitch, pitchDifference))
        };

        finalRotation = applyGCDFix(currentRotation, finalTargetRotation);

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