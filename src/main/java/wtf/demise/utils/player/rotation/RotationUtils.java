package wtf.demise.utils.player.rotation;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.*;
import org.jetbrains.annotations.NotNull;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
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

public class RotationUtils implements InstanceAccess {
    public static float[] currentRotation = null, serverRotation = new float[]{}, previousRotation = null;
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
    public static float rotDiffBuildUp;

    public static boolean shouldRotate() {
        return currentRotation != null;
    }

    public static void setRotation(float[] rotation) {
        setRotation(rotation, MovementCorrection.None);
    }

    public static void setRotation(float[] rotation, final MovementCorrection correction) {
        prevRotRequireNonNullElse = Objects.requireNonNullElse(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});

        if (moduleRotation.silent.get()) {
            RotationUtils.currentRotation = applyGCDFix(serverRotation, rotation);
        } else {
            mc.thePlayer.rotationYaw = applyGCDFix(serverRotation, rotation)[0];
            mc.thePlayer.rotationPitch = applyGCDFix(serverRotation, rotation)[1];
        }

        currentCorrection = correction;
        cachedHSpeed = 180;
        cachedVSpeed = 180;
        enabled = true;
        currRotRequireNonNullElse = Objects.requireNonNullElse(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});
    }

    public static void setRotation(float[] rotation, final MovementCorrection correction, float hSpeed, float vSpeed) {
        prevRotRequireNonNullElse = Objects.requireNonNullElse(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});

        if (moduleRotation.silent.get()) {
            currentRotation = limitRotations(serverRotation, rotation, hSpeed, vSpeed, 1, false, SmoothMode.Linear);
        } else {
            mc.thePlayer.rotationYaw = limitRotations(serverRotation, rotation, hSpeed, vSpeed, 1, false, SmoothMode.Linear)[0];
            mc.thePlayer.rotationPitch = limitRotations(serverRotation, rotation, hSpeed, vSpeed, 1, false, SmoothMode.Linear)[1];
        }

        currentCorrection = correction;
        cachedHSpeed = hSpeed;
        cachedVSpeed = vSpeed;
        RotationUtils.smoothMode = SmoothMode.Linear;
        enabled = true;
        currRotRequireNonNullElse = Objects.requireNonNullElse(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});
    }

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
        RotationUtils.smoothMode = smoothMode;
        enabled = true;
        currRotRequireNonNullElse = Objects.requireNonNullElse(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});
    }

    @EventTarget
    private void onMove(MoveInputEvent e) {
        if (shouldRotate()) {
            if (currentCorrection == MovementCorrection.Silent) {
                final float yaw = currentRotation[0];
                MoveUtil.fixMovement(e, yaw);
            }
        }
    }

    @EventTarget
    private void onStrafe(StrafeEvent e) {
        if (shouldRotate()) {
            if (currentCorrection != MovementCorrection.None) {
                e.setYaw(currentRotation[0]);
            }
        }
    }

    @EventTarget
    private void onJump(JumpEvent event) {
        if (shouldRotate()) {
            if (currentCorrection != MovementCorrection.None) {
                event.setYaw(currentRotation[0]);
            }
        }
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

        if (serverRotation != null && enabled) {
            float diff = getAngleDifference(packetPlayer.getYaw(), serverRotation[0]);
            rotDiffBuildUp += diff;
        }

        serverRotation = new float[]{packetPlayer.yaw, packetPlayer.pitch};
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        resetRotation();
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        angleCalled = true;
    }

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        if (currentRotation != null) {
            double distanceToPlayerRotation = getRotationDifference(currentRotation, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});

            if (!enabled && angleCalled) {
                if (distanceToPlayerRotation < 1) {
                    resetRotation();
                    return;
                }

                if (distanceToPlayerRotation > 0) {
                    float finalHSpeed = (cachedHSpeed / 2) * mc.timer.partialTicks;
                    float finalVSpeed = (cachedVSpeed / 2) * mc.timer.partialTicks;

                    RotationUtils.currentRotation = limitRotations(Objects.requireNonNullElse(currentRotation, serverRotation), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, finalHSpeed, finalVSpeed, cachedMidpoint, cachedAccel, smoothMode);
                }
            }

            enabled = false;
            angleCalled = false;
        }
    }

    @EventPriority(Integer.MIN_VALUE)
    @EventTarget
    public void onLook(LookEvent e) {
        if (shouldRotate()) {
            e.rotation = currentRotation;
        }
    }

    private static void resetRotation() {
        enabled = false;
        RotationUtils.currentRotation = null;
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

    public static float getAngleDifference(float a, float b) {
        return MathHelper.wrapAngleTo180_float(a - b);
    }

    public static double getRotationDifference(float[] e) {
        return getRotationDifference(serverRotation, e);
    }

    public static double getRotationDifference(Vec3 e) {
        float[] entityRotation = getRotations(e.xCoord, e.yCoord, e.zCoord);
        return getRotationDifference(entityRotation);
    }

    public static float getRotationDifference(final Entity entity) {
        float[] target = RotationUtils.getRotations(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        return (float) hypot(abs(getAngleDifference(target[0], shouldRotate() ? currentRotation[0] : mc.thePlayer.rotationYaw)), abs(target[1] - (shouldRotate() ? currentRotation[1] : mc.thePlayer.rotationYaw)));
    }

    public static float getRotationDifferenceClientRot(final Entity entity) {
        float[] target = RotationUtils.getRotations(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        return (float) hypot(abs(getAngleDifference(target[0], mc.thePlayer.rotationYaw)), abs(target[1] - mc.thePlayer.rotationYaw));
    }

    public static float getRotationDifference(final float[] a, final float[] b) {
        return (float) hypot(abs(getAngleDifference(a[0], b[0])), abs(a[1] - b[1]));
    }

    public static MovingObjectPosition rayTrace(float[] rot, double blockReachDistance, float partialTicks) {
        Vec3 vec3 = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 vec31 = mc.thePlayer.getLookCustom(rot[0], rot[1]);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);
        return mc.theWorld.rayTraceBlocks(vec3, vec32, false, true, true);
    }

    public static MovingObjectPosition rayTrace(double blockReachDistance, float partialTicks) {
        Vec3 vec3 = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 vec31 = mc.thePlayer.getLookCustom(currentRotation[0], currentRotation[1]);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);
        return mc.theWorld.rayTraceBlocks(vec3, vec32, false, true, true);
    }

    public static float[] getRotations(BlockPos blockPos, EnumFacing enumFacing) {
        return getRotations(blockPos, enumFacing, 0.25, 0.25);
    }

    public static float[] getRotations(BlockPos blockPos, EnumFacing enumFacing, double xz, double y) {
        double d = blockPos.getX() + 0.5 - mc.thePlayer.posX + enumFacing.getFrontOffsetX() * xz;
        double d2 = blockPos.getZ() + 0.5 - mc.thePlayer.posZ + enumFacing.getFrontOffsetZ() * xz;
        double d3 = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - blockPos.getY() - enumFacing.getFrontOffsetY() * y;
        double d4 = MathHelper.sqrt_double(d * d + d2 * d2);
        float f = (float) (atan2(d2, d) * 180.0 / PI) - 90.0f;
        float f2 = (float) (atan2(d3, d4) * 180.0 / PI);
        return new float[]{MathHelper.wrapAngleTo180_float(f), f2};
    }

    public static float[] getRotations(double rotX, double rotY, double rotZ, double startX, double startY, double startZ) {
        double x = rotX - startX;
        double y = rotY - startY;
        double z = rotZ - startZ;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (atan2(z, x) * 180.0 / PI) - 90.0F;
        float pitch = (float) (-(atan2(y, dist) * 180.0 / PI));
        return new float[]{yaw, pitch};
    }

    public static float[] getRotations(double posX, double posY, double posZ) {
        return getRotations(posX, posY, posZ, mc.thePlayer.posX, mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
    }

    public static float[] getRotations(Vec3 vec) {
        return getRotations(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public static float[] getRotationToBlock(BlockPos blockPos, EnumFacing direction) {
        double centerX = blockPos.getX() + 0.5 + direction.getFrontOffsetX() * 0.5;
        double centerY = blockPos.getY() + 0.5 + direction.getFrontOffsetY() * 0.5;
        double centerZ = blockPos.getZ() + 0.5 + direction.getFrontOffsetZ() * 0.5;

        double playerX = mc.thePlayer.posX;
        double playerY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double playerZ = mc.thePlayer.posZ;

        double deltaX = centerX - playerX;
        double deltaY = centerY - playerY;
        double deltaZ = centerZ - playerZ;

        double distanceXZ = sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (toDegrees(atan2(deltaZ, deltaX)) - 90.0F);
        float pitch = (float) -toDegrees(atan2(deltaY, distanceXZ));

        return new float[]{yaw, pitch};
    }

    public static Vec3 getBestHitVec(final Entity entity) {
        final Vec3 positionEyes = mc.thePlayer.getPositionEyes(1);
        final AxisAlignedBB entityBoundingBox = entity.getHitbox();
        final double ex = MathHelper.clamp_double(positionEyes.xCoord, entityBoundingBox.minX, entityBoundingBox.maxX);
        final double ey = MathHelper.clamp_double(positionEyes.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY);
        final double ez = MathHelper.clamp_double(positionEyes.zCoord, entityBoundingBox.minZ, entityBoundingBox.maxZ);
        return new Vec3(ex, ey, ez);
    }

    public static float getYaw(@NotNull BlockPos pos) {
        return getYaw(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public static float getYaw(@NotNull AbstractClientPlayer from, @NotNull Vec3 pos) {
        return from.rotationYaw +
                MathHelper.wrapAngleTo180_float(
                        (float) toDegrees(atan2(pos.zCoord - from.posZ, pos.xCoord - from.posX)) - 90f - from.rotationYaw
                );
    }

    public static float getYaw(@NotNull Vec3 pos) {
        return getYaw(mc.thePlayer, pos);
    }

    public static float getPitch(@NotNull BlockPos pos) {
        return getPitch(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public static float getPitch(@NotNull AbstractClientPlayer from, @NotNull Vec3 pos) {
        double diffX = pos.xCoord - from.posX;
        double diffY = pos.yCoord - (from.posY + from.getEyeHeight());
        double diffZ = pos.zCoord - from.posZ;

        double diffXZ = sqrt(diffX * diffX + diffZ * diffZ);

        return from.rotationPitch + MathHelper.wrapAngleTo180_float((float) -toDegrees(atan2(diffY, diffXZ)) - from.rotationPitch);
    }

    public static float getPitch(@NotNull Vec3 pos) {
        return getPitch(mc.thePlayer, pos);
    }

    public static float[] faceTrajectory(Entity target, boolean predict, float predictSize, float gravity, float velocity) {
        EntityPlayerSP player = mc.thePlayer;

        double posX = target.posX + (predict ? (target.posX - target.prevPosX) * predictSize : 0.0) - (player.posX + (predict ? player.posX - player.prevPosX : 0.0));
        double posY = target.getEntityBoundingBox().minY + (predict ? (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize : 0.0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().minY + (predict ? player.posY - player.prevPosY : 0.0)) - player.getEyeHeight();
        double posZ = target.posZ + (predict ? (target.posZ - target.prevPosZ) * predictSize : 0.0) - (player.posZ + (predict ? player.posZ - player.prevPosZ : 0.0));
        double posSqrt = sqrt(posX * posX + posZ * posZ);

        velocity = min((velocity * velocity + velocity * 2) / 3, 1f);

        float gravityModifier = 0.12f * gravity;

        return new float[]{
                (float) toDegrees(atan2(posZ, posX)) - 90f,
                (float) -toDegrees(atan((velocity * velocity - sqrt(
                        velocity * velocity * velocity * velocity - gravityModifier * (gravityModifier * posSqrt * posSqrt + 2 * posY * velocity * velocity)
                )) / (gravityModifier * posSqrt)))
        };
    }
}