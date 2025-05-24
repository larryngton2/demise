package wtf.demise.utils.math;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.utils.InstanceAccess;

import java.security.SecureRandom;

public class MathUtils implements InstanceAccess {
    private static final TimerUtils lerpUpdateTimer = new TimerUtils();
    private static boolean updateLerp;

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        if (lerpUpdateTimer.hasTimeElapsed(10)) {
            updateLerp = true;
            lerpUpdateTimer.reset();
            return;
        }

        updateLerp = false;
    }

    public static double roundToHalf(double d) {
        return Math.round(d * 2) / 2.0;
    }

    public static double incValue(double val, double inc) {
        double one = 1.0 / inc;
        return Math.round(val * one) / one;
    }

    public static float interpolate(float current, float target) {
        return interpolate(current, target, mc.timer.partialTicks);
    }

    public static float interpolate(float current, float target, float multiple) {
        if (multiple == mc.timer.partialTicks) {
            return current + (target - current) * multiple;
        }

        if (updateLerp) {
            return current + (target - current) * multiple;
        }
        
        return current;
    }

    public static double interpolate(double current, double target) {
        return interpolate(current, target, mc.timer.partialTicks);
    }

    public static double interpolate(double current, double target, float multiple) {
        return interpolate((float) current, (float) target, multiple);
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target) {
        return interpolate(current, target, mc.timer.partialTicks);
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target, float multiple) {
        if (multiple == mc.timer.partialTicks) {
            return new Vec3(
                    interpolate(current.xCoord, target.xCoord, multiple),
                    interpolate(current.yCoord, target.yCoord, multiple),
                    interpolate(current.zCoord, target.zCoord, multiple));
        }

        if (updateLerp) {
            return new Vec3(
                    interpolate(current.xCoord, target.xCoord, multiple),
                    interpolate(current.yCoord, target.yCoord, multiple),
                    interpolate(current.zCoord, target.zCoord, multiple));
        }
        
        return current;
    }

    public static AxisAlignedBB interpolate(AxisAlignedBB current, AxisAlignedBB target) {
        return interpolate(current, target, mc.timer.partialTicks);
    }

    public static AxisAlignedBB interpolate(AxisAlignedBB current, AxisAlignedBB target, float multiple) {
        if (multiple == mc.timer.partialTicks) {
            return new AxisAlignedBB(
                    interpolate(current.minX, target.minX, multiple),
                    interpolate(current.minY, target.minY, multiple),
                    interpolate(current.minZ, target.minZ, multiple),
                    interpolate(current.maxX, target.maxX, multiple),
                    interpolate(current.maxY, target.maxY, multiple),
                    interpolate(current.maxZ, target.maxZ, multiple)
            );
        }

        if (updateLerp) {
            return new AxisAlignedBB(
                    interpolate(current.minX, target.minX, multiple),
                    interpolate(current.minY, target.minY, multiple),
                    interpolate(current.minZ, target.minZ, multiple),
                    interpolate(current.maxX, target.maxX, multiple),
                    interpolate(current.maxY, target.maxY, multiple),
                    interpolate(current.maxZ, target.maxZ, multiple)
            );
        }

        return current;
    }

    public static float interpolateNoUpdateCheck(float current, float target, float multiple) {
        return current + (target - current) * multiple;
    }

    public static Vec3 interpolateNoUpdateCheck(Vec3 current, Vec3 target, float multiple) {
        return new Vec3(
                interpolate(current.xCoord, target.xCoord, multiple),
                interpolate(current.yCoord, target.yCoord, multiple),
                interpolate(current.zCoord, target.zCoord, multiple));
    }

    public static float nextSecureFloat(final double origin, final double bound) {
        if (origin == bound) {
            return (float) origin;
        }
        final SecureRandom secureRandom = new SecureRandom();
        final float difference = (float) (bound - origin);
        return (float) (origin + secureRandom.nextFloat() * difference);
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double PI = Math.PI;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public static int nextInt(int min, int max) {
        if (min == max || max - min <= 0D)
            return min;

        return (int) (min + ((max - min) * Math.random()));
    }

    public static double nextDouble(double min, double max) {
        if (min == max || max - min <= 0D)
            return min;

        return min + ((max - min) * Math.random());
    }

    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    public static float nextFloat(final float startInclusive, final float endInclusive) {
        if (startInclusive == endInclusive || endInclusive - startInclusive <= 0F)
            return startInclusive;

        return (float) (startInclusive + ((endInclusive - startInclusive) * Math.random()));
    }

    public static int randomizeInt(double min, double max) {
        return (int) randomizeDouble(min, max);
    }

    public static double randomizeDouble(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    public static float randomizeFloat(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    public static boolean inBetween(double min, double max, double value) {
        return value >= min && value <= max;
    }

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
    }

    public static float getAdvancedRandom(float min, float max) {
        SecureRandom random = new SecureRandom();

        long finalSeed = System.nanoTime();

        for (int i = 0; i < 3; ++i) {
            long seed = (long) (Math.random() * 1_000_000_000);

            seed ^= (seed << 13);
            seed ^= (seed >>> 17);
            seed ^= (seed << 15);

            finalSeed += seed;
        }

        random.setSeed(finalSeed);

        return random.nextFloat() * (max - min) + min;
    }

    public static Vec3 closestPointOnFace(AxisAlignedBB aabb, EnumFacing face, double x, double y, double z) {
        double closestX, closestY, closestZ;

        switch (face) {
            case DOWN, UP -> {
                closestX = Math.max(aabb.minX, Math.min(x, aabb.maxX));
                closestY = face == EnumFacing.DOWN ? aabb.minY : aabb.maxY;
                closestZ = Math.max(aabb.minZ, Math.min(z, aabb.maxZ));
            }
            case NORTH, SOUTH -> {
                closestX = Math.max(aabb.minX, Math.min(x, aabb.maxX));
                closestY = Math.max(aabb.minY, Math.min(y, aabb.maxY));
                closestZ = face == EnumFacing.NORTH ? aabb.minZ : aabb.maxZ;
            }
            case WEST, EAST -> {
                closestX = face == EnumFacing.WEST ? aabb.minX : aabb.maxX;
                closestY = Math.max(aabb.minY, Math.min(y, aabb.maxY));
                closestZ = Math.max(aabb.minZ, Math.min(z, aabb.maxZ));
            }
            default -> throw new IllegalArgumentException("Invalid face: " + face);
        }

        return new Vec3(closestX, closestY, closestZ);
    }

    public static Vec3 closestPointOnFace(AxisAlignedBB aabb, EnumFacing face, Vec3 vec) {
        return closestPointOnFace(aabb, face, vec.xCoord, vec.yCoord, vec.zCoord);
    }
}