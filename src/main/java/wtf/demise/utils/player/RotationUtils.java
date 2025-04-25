package wtf.demise.utils.player;

import com.google.common.base.Predicates;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.*;
import net.optifine.reflect.Reflector;
import org.jetbrains.annotations.NotNull;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.features.modules.impl.visual.Rotation;
import wtf.demise.utils.InstanceAccess;

import java.util.List;
import java.util.Objects;

import static java.lang.Math.*;

public class RotationUtils implements InstanceAccess {
    public static float[] currentRotation = null, serverRotation = new float[]{}, previousRotation = null;
    public static MovementCorrection currentCorrection = MovementCorrection.None;
    private static boolean enabled;
    public static float cachedHSpeed;
    public static float cachedVSpeed;
    public static float cachedMidpoint;
    public static SmoothMode smoothMode = SmoothMode.Linear;
    private static final Rotation moduleRotation = Demise.INSTANCE.getModuleManager().getModule(Rotation.class);
    private boolean angleCalled;

    public static boolean shouldRotate() {
        return currentRotation != null;
    }

    public static void setRotation(float[] rotation) {
        setRotation(rotation, MovementCorrection.None);
    }

    public static void setRotation(float[] rotation, final MovementCorrection correction) {
        RotationUtils.currentRotation = applyGCDFix(serverRotation, rotation);

        currentCorrection = correction;
        cachedHSpeed = 180;
        cachedVSpeed = 180;
        enabled = true;
    }

    public static void setRotation(float[] rotation, final MovementCorrection correction, float hSpeed, float vSpeed) {
        hSpeed /= mc.timer.partialTicks;
        vSpeed /= mc.timer.partialTicks;

        hSpeed = MathHelper.clamp_float(hSpeed, 1, 180);
        vSpeed = MathHelper.clamp_float(vSpeed, 1, 180);

        RotationUtils.currentRotation = smoothLinear(serverRotation, rotation, hSpeed, vSpeed);

        currentCorrection = correction;
        cachedHSpeed = hSpeed;
        cachedVSpeed = vSpeed;
        RotationUtils.smoothMode = SmoothMode.Linear;

        enabled = true;
    }

    public static void setRotation(float[] rotation, final MovementCorrection correction, float hSpeed, float vSpeed, SmoothMode smoothMode, float midpoint) {
        hSpeed /= mc.timer.partialTicks;
        vSpeed /= mc.timer.partialTicks;

        hSpeed = MathHelper.clamp_float(hSpeed, 1, 180);
        vSpeed = MathHelper.clamp_float(vSpeed, 1, 180);

            switch (smoothMode) {
                case Linear:
                    currentRotation = smoothLinear(serverRotation, rotation, hSpeed, vSpeed);
                    break;
                case Lerp:
                    currentRotation = smoothLerp(serverRotation, rotation, hSpeed, vSpeed);
                    break;
                case Bezier:
                    currentRotation = smoothBezier(serverRotation, rotation, hSpeed, vSpeed, midpoint);
                    break;
                case Exponential:
                    currentRotation = smoothExpo(serverRotation, rotation, hSpeed, vSpeed);
                    break;
                case Test:
                    currentRotation = smoothNatural(serverRotation, rotation, hSpeed, vSpeed);
                    break;
            }

        currentCorrection = correction;
        cachedHSpeed = hSpeed;
        cachedVSpeed = vSpeed;
        cachedMidpoint = midpoint;
        RotationUtils.smoothMode = smoothMode;

        enabled = true;
    }

    @EventTarget
    private void onMove(MoveInputEvent e) {
        if (currentCorrection == MovementCorrection.Silent) {
            final float yaw = currentRotation[0];
            MoveUtil.fixMovement(e, yaw);
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
    public void onAngle(AngleEvent e) {
        angleCalled = true;
    }

    @EventTarget
    @EventPriority(-100)
    public void onPacket(final PacketEvent e) {
        final Packet<?> packet = e.getPacket();

        if (!(packet instanceof C03PacketPlayer packetPlayer)) return;

        if (!packetPlayer.rotating) return;

        if (shouldRotate()) {
            packetPlayer.yaw = currentRotation[0];
            packetPlayer.pitch = currentRotation[1];
        }

        serverRotation = new float[]{packetPlayer.yaw, packetPlayer.pitch};
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        resetRotation();
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

                    RotationUtils.currentRotation = switch (smoothMode) {
                        case Linear ->
                                smoothLinear(Objects.requireNonNullElse(currentRotation, serverRotation), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, finalHSpeed, finalVSpeed);
                        case Lerp ->
                                smoothLerp(Objects.requireNonNullElse(currentRotation, serverRotation), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, finalHSpeed, finalVSpeed);
                        case Bezier ->
                                smoothBezier(Objects.requireNonNullElse(currentRotation, serverRotation), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, finalHSpeed, finalVSpeed, cachedMidpoint);
                        case Exponential ->
                                smoothExpo(Objects.requireNonNullElse(currentRotation, serverRotation), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, finalHSpeed, finalVSpeed);
                        default ->
                                smoothLinear(Objects.requireNonNullElse(currentRotation, serverRotation), new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch}, finalHSpeed, finalVSpeed);
                    };
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

    public static float[] smoothLinear(final float[] currentRotation, final float[] targetRotation, float hSpeed, float vSpeed) {
        float yawDifference = getAngleDifference(targetRotation[0], currentRotation[0]);
        float pitchDifference = getAngleDifference(targetRotation[1], currentRotation[1]);

        double rotationDifference = hypot(abs(yawDifference), abs(pitchDifference));

        float straightLineYaw = (float) (abs(yawDifference / rotationDifference) * hSpeed);
        float straightLinePitch = (float) (abs(pitchDifference / rotationDifference) * vSpeed);

        float[] finalTargetRotation = new float[]{
                currentRotation[0] + Math.max(-straightLineYaw, Math.min(straightLineYaw, yawDifference)),
                currentRotation[1] + Math.max(-straightLinePitch, Math.min(straightLinePitch, pitchDifference))
        };

        return applyGCDFix(currentRotation, finalTargetRotation);
    }

    public static float[] smoothBezier(final float[] currentRotation, final float[] targetRotation, float hSpeed, float vSpeed, float midpoint) {
        float yawDifference = getAngleDifference(targetRotation[0], currentRotation[0]);
        float pitchDifference = getAngleDifference(targetRotation[1], currentRotation[1]);

        double rotationDifference = hypot(abs(yawDifference), abs(pitchDifference));

        float straightLineYaw = (float) (abs(yawDifference / rotationDifference) * hSpeed);
        float straightLinePitch = (float) (abs(pitchDifference / rotationDifference) * vSpeed);

        float[] finalTargetRotation = new float[]{
                currentRotation[0] + Math.max(-straightLineYaw, Math.min(straightLineYaw, yawDifference)),
                currentRotation[1] + Math.max(-straightLinePitch, Math.min(straightLinePitch, pitchDifference))
        };

        float yawDirection = yawDifference / (float) rotationDifference;
        float pitchDirection = pitchDifference / (float) rotationDifference;

        float controlYaw = currentRotation[0] + yawDirection * midpoint * (float) rotationDifference;
        float controlPitch = currentRotation[1] + pitchDirection * midpoint * (float) rotationDifference;

        float[] t = new float[]{hSpeed / 180, vSpeed / 180};

        float finalYaw = (1 - t[0]) * (1 - t[0]) * currentRotation[0] + 2 * (1 - t[0]) * t[0] * controlYaw + t[0] * t[0] * finalTargetRotation[0];
        float finalPitch = (1 - t[1]) * (1 - t[1]) * currentRotation[1] + 2 * (1 - t[1]) * t[1] * controlPitch + t[1] * t[1] * finalTargetRotation[1];

        float[] finalRotation = new float[]{finalYaw, finalPitch};

        return applyGCDFix(currentRotation, finalRotation);
    }

    public static float[] smoothLerp(final float[] currentRotation, final float[] targetRotation, float hSpeed, float vSpeed) {
        float yawDifference = getAngleDifference(targetRotation[0], currentRotation[0]);
        float pitchDifference = getAngleDifference(targetRotation[1], currentRotation[1]);

        float newYaw = currentRotation[0] + (yawDifference * hSpeed / 180);
        float newPitch = currentRotation[1] + (pitchDifference * vSpeed / 180);

        float[] finalTargetRotation = new float[]{newYaw, newPitch};

        return applyGCDFix(currentRotation, finalTargetRotation);
    }

    public static float[] smoothExpo(final float[] currentRotation, final float[] targetRotation, float hSpeed, float vSpeed) {
        float yawDifference = getAngleDifference(targetRotation[0], currentRotation[0]);
        float pitchDifference = getAngleDifference(targetRotation[1], currentRotation[1]);

        float smoothedYaw = currentRotation[0] + yawDifference * (1 - (float)Math.exp(-(hSpeed / 180)));
        float smoothedPitch = currentRotation[1] + pitchDifference * (1 - (float)Math.exp(-(vSpeed / 180)));

        float[] finalTargetRotation = new float[] {
                smoothedYaw,
                smoothedPitch
        };

        return applyGCDFix(currentRotation, finalTargetRotation);
    }

    public static float[] smoothNatural(final float[] currentRotation, final float[] targetRotation, float hSpeed, float vSpeed) {
        float normHSpeed = hSpeed;
        float normVSpeed = vSpeed;

        float yawDifference = getAngleDifference(targetRotation[0], currentRotation[0]);
        float pitchDifference = getAngleDifference(targetRotation[1], currentRotation[1]);

        double rotationDistance = hypot(abs(yawDifference), abs(pitchDifference));

        float distanceFactor = (float) Math.log1p(rotationDistance / 45.0) / 2.0f;
        distanceFactor = Math.min(distanceFactor, 1.0f);

        float yawSign = Math.signum(yawDifference);
        float pitchSign = Math.signum(pitchDifference);

        float dynamicYawSpeed = calculateDynamicSpeed(abs(yawDifference), normHSpeed, distanceFactor);
        float dynamicPitchSpeed = calculateDynamicSpeed(abs(pitchDifference), normVSpeed, distanceFactor);

        float newYaw = currentRotation[0] + yawSign * dynamicYawSpeed;
        float newPitch = currentRotation[1] + pitchSign * dynamicPitchSpeed;

        if (abs(yawDifference) < dynamicYawSpeed) {
            newYaw = targetRotation[0];
        }
        if (abs(pitchDifference) < dynamicPitchSpeed) {
            newPitch = targetRotation[1];
        }

        return applyGCDFix(currentRotation, new float[]{newYaw, newPitch});
    }

    private static float calculateDynamicSpeed(float difference, float baseSpeed, float distanceFactor) {
        float speed = baseSpeed * (0.7f + 0.3f * distanceFactor);

        if (difference < 5.0f) {
            return speed * (0.3f + 0.7f * (difference / 5.0f));
        } else if (difference < 15.0f) {
            return speed * (0.8f + 0.2f * (difference / 15.0f));
        } else {
            return speed;
        }
    }

    public static float[] applyGCDFix(float[] prevRotation, float[] currentRotation) {
        final float f = (float) (mc.gameSettings.mouseSensitivity * (1 + Math.random() / 100000) * 0.6F + 0.2F);
        final double gcd = f * f * f * 8.0F * 0.15D;
        final float yaw = prevRotation[0] + (float) (Math.round((currentRotation[0] - prevRotation[0]) / gcd) * gcd);
        final float pitch = prevRotation[1] + (float) (Math.round((currentRotation[1] - prevRotation[1]) / gcd) * gcd);

        return new float[]{yaw, pitch};
    }

    public static float getAngleDifference(float a, float b) {
        return MathHelper.wrapAngleTo180_float(a - b);
    }

    public static float[] getAngles(Entity entity) {
        if (entity == null) return null;
        final EntityPlayerSP player = mc.thePlayer;

        final double diffX = entity.posX - player.posX,
                diffY = entity.posY + (entity.getEyeHeight() / 5 * 3) - (player.posY + player.getEyeHeight()),
                diffZ = entity.posZ - player.posZ, dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        final float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F,
                pitch = (float) -(Math.atan2(diffY, dist) * 180.0D / Math.PI);

        return new float[]{player.rotationYaw + MathHelper.wrapAngleTo180_float(
                yaw - player.rotationYaw), player.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - player.rotationPitch)};
    }

    public static float i(final double n, final double n2) {
        return (float) (Math.atan2(n - mc.thePlayer.posX, n2 - mc.thePlayer.posZ) * 57.295780181884766 * -1.0);
    }

    public static double distanceFromYaw(final Entity entity) {
        return abs(MathHelper.wrapAngleTo180_double(i(entity.posX, entity.posZ) - mc.thePlayer.rotationYaw));
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
        return (float) hypot(abs(getAngleDifference(target[0], mc.thePlayer.rotationYaw)), abs(target[1] - mc.thePlayer.rotationPitch));
    }

    public static float getRotationDifference(final Entity entity, final Entity entity2) {
        float[] target = RotationUtils.getRotations(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        float[] target2 = RotationUtils.getRotations(entity2.posX, entity2.posY + entity2.getEyeHeight(), entity2.posZ);
        return (float) hypot(abs(getAngleDifference(target[0], target2[0])), abs(target[1] - target2[1]));
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
        float f = (float) (Math.atan2(d2, d) * 180.0 / Math.PI) - 90.0f;
        float f2 = (float) (Math.atan2(d3, d4) * 180.0 / Math.PI);
        return new float[]{MathHelper.wrapAngleTo180_float(f), f2};
    }

    public static float[] getRotations(double rotX, double rotY, double rotZ, double startX, double startY, double startZ) {
        double x = rotX - startX;
        double y = rotY - startY;
        double z = rotZ - startZ;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0 / Math.PI));
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

        double distanceXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, distanceXZ));

        return new float[]{yaw, pitch};
    }

    public static float clampTo90(final float n) {
        return MathHelper.clamp_float(n, -90, 90);
    }

    public static float calculateYawFromSrcToDst(final float yaw, final double srcX, final double srcZ, final double dstX, final double dstZ) {
        final double xDist = dstX - srcX;
        final double zDist = dstZ - srcZ;
        final float var1 = (float) (StrictMath.atan2(zDist, xDist) * 180.0 / Math.PI) - 90.0F;
        return yaw + MathHelper.wrapAngleTo180_float(var1 - yaw);
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
                        (float) Math.toDegrees(Math.atan2(pos.zCoord - from.posZ, pos.xCoord - from.posX)) - 90f - from.rotationYaw
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

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return from.rotationPitch + MathHelper.wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - from.rotationPitch);
    }

    public static float getPitch(@NotNull Vec3 pos) {
        return getPitch(mc.thePlayer, pos);
    }

    public static float angleDifference(float a, float b) {
        return MathHelper.wrapAngleTo180_float(a - b);
    }

    public static float[] faceTrajectory(Entity target, boolean predict, float predictSize, float gravity, float velocity) {
        EntityPlayerSP player = mc.thePlayer;

        double posX = target.posX + (predict ? (target.posX - target.prevPosX) * predictSize : 0.0) - (player.posX + (predict ? player.posX - player.prevPosX : 0.0));
        double posY = target.getEntityBoundingBox().minY + (predict ? (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize : 0.0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().minY + (predict ? player.posY - player.prevPosY : 0.0)) - player.getEyeHeight();
        double posZ = target.posZ + (predict ? (target.posZ - target.prevPosZ) * predictSize : 0.0) - (player.posZ + (predict ? player.posZ - player.prevPosZ : 0.0));
        double posSqrt = Math.sqrt(posX * posX + posZ * posZ);

        velocity = Math.min((velocity * velocity + velocity * 2) / 3, 1f);

        float gravityModifier = 0.12f * gravity;

        return new float[]{
                (float) Math.toDegrees(Math.atan2(posZ, posX)) - 90f,
                (float) -Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(
                        velocity * velocity * velocity * velocity - gravityModifier * (gravityModifier * posSqrt * posSqrt + 2 * posY * velocity * velocity)
                )) / (gravityModifier * posSqrt)))
        };
    }

    public static float[] faceTrajectory(Entity target, boolean predict, float predictSize) {
        float gravity = 0.03f;
        float velocity = 0;

        return faceTrajectory(target, predict, predictSize, gravity, velocity);
    }

    public static boolean isLookingAtEntity(final float[] rotation, final double range) {
        return isLookingAtEntity(rayCast(rotation, range, 1).entityHit, rotation, range);
    }

    public static boolean isLookingAtEntity(Entity target, float[] rotations, final double range) {
        Vec3 src = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 rotationVec = mc.thePlayer.getLookCustom(rotations[0], rotations[1]);
        Vec3 dest = src.addVector(rotationVec.xCoord * range, rotationVec.yCoord * range, rotationVec.zCoord * range);
        MovingObjectPosition obj = mc.theWorld.rayTraceBlocks(src, dest, false, false, true);
        if (obj == null) {
            return false;
        }
        return target.getEntityBoundingBox().expand(target.getCollisionBorderSize(), target.getCollisionBorderSize(), target.getCollisionBorderSize()).calculateIntercept(src, dest) != null;
    }

    public static MovingObjectPosition rayCast(final float[] rots, final double range, final float partialTicks) {
        MovingObjectPosition objectMouseOver = null;
        Entity entity = mc.getRenderViewEntity();

        if (entity != null && mc.theWorld != null) {
            Entity pointedEntity = null;
            double d0 = mc.playerController.getBlockReachDistance();
            objectMouseOver = entity.rayTraceCustom(d0, partialTicks, rots[0], rots[1]);
            double d1 = d0;
            Vec3 vec3 = entity.getPositionEyes(partialTicks);
            boolean flag = false;
            double i = range;

            MouseOverEvent mouseOverEvent = new MouseOverEvent(i, 0, 4.5);
            Demise.INSTANCE.getEventManager().call(mouseOverEvent);

            i = mouseOverEvent.getRange();

            if (mc.playerController.extendedReach()) {
                d0 = 6.0D;
                d1 = 6.0D;
            } else if (d0 > (ViaLoadingBase.getInstance().getTargetVersion().getVersion() <= 47 ? 3.0D : 2.9D)) {
                flag = true;
            }

            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }

            Vec3 vec31 = entity.getLookCustom(RotationUtils.serverRotation[0], RotationUtils.serverRotation[1]);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            Vec3 vec33 = null;
            float f = 1.0F;
            List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = d1;

            for (Entity entity1 : list) {
                float f1 = entity1.getCollisionBorderSize();
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        boolean flag1 = false;

                        if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                            flag1 = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract);
                        }

                        if (!flag1 && entity1 == entity.ridingEntity) {
                            if (d2 == 0.0D) {
                                pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                            }
                        } else {
                            pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > (ViaLoadingBase.getInstance().getTargetVersion().getVersion() <= 47 ? i : i - 0.1f)) {
                pointedEntity = null;
                objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null, new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);
            }
        }
        return objectMouseOver;
    }
}