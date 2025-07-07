package wtf.demise.utils.player.rotation;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import org.jetbrains.annotations.NotNull;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.utils.InstanceAccess;

import static java.lang.Math.*;
import static wtf.demise.utils.player.rotation.RotationHandler.*;

public class RotationUtils implements InstanceAccess {
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

    public static MovingObjectPosition rayTraceSafe(float[] rot, double reach, float partialTicks) {
        Vec3 from = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 direction = mc.thePlayer.getLookCustom(rot[0], rot[1]);
        Vec3 to = from.addVector(direction.xCoord * reach, direction.yCoord * reach, direction.zCoord * reach);

        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(from, to, false, true, true);

        if (result == null) {
            return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, to, EnumFacing.UP, new BlockPos(to));
        }

        return result;
    }

    public static MovingObjectPosition rayTraceSafe(double reach, float partialTicks) {
        Vec3 from = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 direction = mc.thePlayer.getLookCustom(currentRotation[0], currentRotation[1]);
        Vec3 to = from.addVector(direction.xCoord * reach, direction.yCoord * reach, direction.zCoord * reach);

        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(from, to, false, true, true);

        if (result == null) {
            return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, to, EnumFacing.UP, new BlockPos(to));
        }

        return result;
    }

    public static float[] getRotations(BlockPos blockPos, EnumFacing enumFacing) {
        return getRotations(blockPos, enumFacing, 0.25, 0.25);
    }

    public static float[] getRotations(BlockPos blockPos) {
        return getRotations(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, mc.thePlayer.posX, mc.thePlayer.posY + (double)mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
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
        double posX = target.posX + (predict ? (target.posX - target.prevPosX) * predictSize : 0.0) - (mc.thePlayer.posX + (predict ? mc.thePlayer.posX - mc.thePlayer.prevPosX : 0.0));
        double posY = target.getEntityBoundingBox().minY + (predict ? (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize : 0.0) + target.getEyeHeight() - 0.15 - (mc.thePlayer.getEntityBoundingBox().minY + (predict ? mc.thePlayer.posY - mc.thePlayer.prevPosY : 0.0)) - mc.thePlayer.getEyeHeight();
        double posZ = target.posZ + (predict ? (target.posZ - target.prevPosZ) * predictSize : 0.0) - (mc.thePlayer.posZ + (predict ? mc.thePlayer.posZ - mc.thePlayer.prevPosZ : 0.0));
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

    public static Vec3 getVec3(BlockPos pos, EnumFacing face) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        x += face.getFrontOffsetX() / 2.0D;
        z += face.getFrontOffsetZ() / 2.0D;
        y += face.getFrontOffsetY() / 2.0D;

        return new Vec3(x, y, z);
    }
}