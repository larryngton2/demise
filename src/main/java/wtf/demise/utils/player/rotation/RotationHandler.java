package wtf.demise.utils.player.rotation;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.Range;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.modules.impl.player.AutoClutch;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.SmoothMode;

import static wtf.demise.utils.player.rotation.RotationUtils.rotDiffBuildUp;

@Getter
public class RotationHandler implements InstanceAccess {
    final ModeValue smoothMode;
    final BoolValue accelerate;
    final BoolValue imperfectCorrelation;
    final SliderValue yawRotationSpeedMin;
    final SliderValue yawRotationSpeedMax;
    final SliderValue pitchRotationSpeedMin;
    final SliderValue pitchRotationSpeedMax;
    final BoolValue distanceBasedRotationSpeed;
    final SliderValue minRange;
    final SliderValue maxRange;
    final SliderValue decrementPerCycle;
    final SliderValue midpoint;
    final ModeValue movementFix;
    final BoolValue shortStop;
    final SliderValue shortStopDuration;
    final SliderValue rotationDiffBuildUpToStop;
    final SliderValue maxThresholdAttemptsToStop;
    private EntityLivingBase target;
    private final Module module;

    public RotationHandler(Module module) {
        this.module = module;

        smoothMode = new ModeValue("Smooth mode", new String[]{"Linear", "Relative", "Bezier", "None"}, "Linear", module);
        accelerate = new BoolValue("Accelerate", false, module, () -> !smoothMode.is("None"));
        imperfectCorrelation = new BoolValue("Imperfect correlation", false, module, () -> !smoothMode.is("None"));
        yawRotationSpeedMin = new SliderValue("Yaw rotation speed (min)", 180, 0.01f, 180, 0.01f, module, () -> !smoothMode.is("None"));
        yawRotationSpeedMax = new SliderValue("Yaw rotation speed (max)", 180, 0.01f, 180, 0.01f, module, () -> !smoothMode.is("None"));
        pitchRotationSpeedMin = new SliderValue("Pitch rotation speed (min)", 180, 0.01f, 180, 0.01f, module, () -> !smoothMode.is("None"));
        pitchRotationSpeedMax = new SliderValue("Pitch rotation speed (max)", 180, 0.01f, 180, 0.01f, module, () -> !smoothMode.is("None"));
        midpoint = new SliderValue("Midpoint", 0.8f, 0.01f, 1, 0.01f, module, () -> smoothMode.is("Bezier"));

        if (module.getClass() == KillAura.class) {
            target = KillAura.currentTarget;
        }

        distanceBasedRotationSpeed = new BoolValue("Distance based rotation speed", false, module, () -> !smoothMode.is("None") && module.getClass() == KillAura.class);
        minRange = new SliderValue("Min range", 0, 0, 8, 0.1f, module, () -> !smoothMode.is("None") && distanceBasedRotationSpeed.get() && module.getClass() == KillAura.class);
        maxRange = new SliderValue("Max range", 8, 0, 8, 0.1f, module, () -> !smoothMode.is("None") && distanceBasedRotationSpeed.get() && module.getClass() == KillAura.class);
        decrementPerCycle = new SliderValue("Decrement per cycle", 0.5f, 0.1f, 2, 0.1f, module, () -> !smoothMode.is("None") && distanceBasedRotationSpeed.get() && module.getClass() == KillAura.class);

        // who the fuck will use strict strafe on scaffold anyway
        if (module.getClass() == Scaffold.class || module.getClass() == AutoClutch.class) {
            movementFix = new ModeValue("Movement fix", new String[]{"None", "Silent"}, "None", module);
        } else {
            movementFix = new ModeValue("Movement fix", new String[]{"None", "Silent", "Strict"}, "None", module);
        }

        shortStop = new BoolValue("Short stop", false, module);
        shortStopDuration = new SliderValue("Duration", 50, 25, 1000, 25, module, shortStop::get);
        rotationDiffBuildUpToStop = new SliderValue("Rotation diff buildup to stop", 180, 50, 720, 1, module, shortStop::get);
        maxThresholdAttemptsToStop = new SliderValue("Max threshold attempts to stop", 1, 0, 5, 1, module, shortStop::get);
    }

    private final TimerUtils shortStopTimer = new TimerUtils();
    private float randPitchSpeed;
    private float randYawSpeed;
    private int maxThresholdReachAttempts;

    /**
     * best to call on AngleEvent
     */
    public void setRotation(float[] targetRotation) {
        SmoothMode mode = SmoothMode.valueOf(smoothMode.get());
        MovementCorrection correction = MovementCorrection.valueOf(movementFix.get());

        float hSpeed = randYawSpeed;
        float vSpeed = randPitchSpeed;

        if (shortStop.get() && shouldShortStop()) {
            hSpeed = MathUtils.randomizeFloat(0, 0.1f);
            vSpeed = MathUtils.randomizeFloat(0, 0.1f);
        }

        if (imperfectCorrelation.get()) {
            hSpeed *= MathUtils.randomizeFloat(0.9F, 1.1F);
            vSpeed *= MathUtils.randomizeFloat(0.9F, 1.1F);
        }

        if (module.getClass() == KillAura.class) {
            target = KillAura.currentTarget;

            if (distanceBasedRotationSpeed.get() && target != null) {
                float distance = (float) PlayerUtils.getDistanceToEntityBox(target);
                if (Range.between(minRange.get(), maxRange.get()).contains(distance)) {
                    float decreaseAmount = ((distance - minRange.get()) / 0.01f) * decrementPerCycle.get();
                    hSpeed -= decreaseAmount;
                    vSpeed -= decreaseAmount;
                }
            }
        }

        hSpeed = MathHelper.clamp_float(hSpeed, 0, 180);
        vSpeed = MathHelper.clamp_float(vSpeed, 0, 180);

        RotationUtils.setRotation(targetRotation, correction, hSpeed, vSpeed, midpoint.get(), accelerate.get(), mode);
    }

    public float[] getSimpleRotationsToEntity(Entity entity) {
        float yaw;
        float pitch;
        Vec3 currentVec;

        Vec3 playerPos = mc.thePlayer.getPositionEyes(1);

        AxisAlignedBB bb = entity.getHitbox();

        Vec3 boxCenter = bb.getCenter();
        Vec3 entityPos = new Vec3(boxCenter.xCoord, bb.minY, boxCenter.zCoord);

        currentVec = entityPos.add(0.0, entity.getEyeHeight(), 0.0);

        double deltaX = currentVec.xCoord - playerPos.xCoord;
        double deltaY = currentVec.yCoord - playerPos.yCoord;
        double deltaZ = currentVec.zCoord - playerPos.zCoord;

        yaw = (float) -(Math.atan2(deltaX, deltaZ) * (180.0 / Math.PI));
        pitch = (float) (-Math.toDegrees(Math.atan2(deltaY, Math.hypot(deltaX, deltaZ))));

        pitch = MathHelper.clamp_float(pitch, -90, 90);

        return new float[]{yaw, pitch};
    }

    private boolean shouldShortStop() {
        if (!shortStopTimer.hasTimeElapsed(shortStopDuration.get())) {
            return true;
        }

        if (Math.abs(rotDiffBuildUp) < rotationDiffBuildUpToStop.get()) return false;

        if (maxThresholdReachAttempts < maxThresholdAttemptsToStop.get()) {
            maxThresholdReachAttempts++;
            return false;
        }

        shortStopTimer.reset();
        return true;
    }

    public void updateRotSpeed(UpdateEvent e) {
        randYawSpeed = MathUtils.randomizeFloat(yawRotationSpeedMin.get(), yawRotationSpeedMax.get());
        randPitchSpeed = MathUtils.randomizeFloat(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get());
    }
}