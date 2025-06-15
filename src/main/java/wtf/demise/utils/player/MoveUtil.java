package wtf.demise.utils.player;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import wtf.demise.Demise;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.impl.movement.TargetStrafe;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.rotation.RotationUtils;

import java.util.Arrays;
import java.util.Objects;

import static java.lang.Math.toRadians;

public class MoveUtil implements InstanceAccess {
    public static final double WALK_SPEED = 0.221;
    public static final double BUNNY_SLOPE = 0.66;
    public static final double MOD_SPRINTING = 1.3F;
    public static final double MOD_SNEAK = 0.3F;
    public static final double MOD_ICE = 2.5F;
    public static final double MOD_WEB = 0.105 / WALK_SPEED;
    public static final double JUMP_HEIGHT = 0.42F;
    public static final double BUNNY_FRICTION = 159.9F;
    public static final double Y_ON_GROUND_MIN = 0.00001;
    public static final double Y_ON_GROUND_MAX = 0.0626;
    public static final double MOD_SWIM = 0.115F / WALK_SPEED;
    public static final double[] MOD_DEPTH_STRIDER = {
            1.0F,
            0.1645F / MOD_SWIM / WALK_SPEED,
            0.1995F / MOD_SWIM / WALK_SPEED,
            1.0F / MOD_SWIM,
    };

    public static final double BASE_JUMP_HEIGHT = 0.41999998688698;

    public static final double UNLOADED_CHUNK_MOTION = -0.09800000190735147;
    public static final double HEAD_HITTER_MOTION = -0.0784000015258789;

    public static float lastYaw2;

    public static boolean isMoving() {
        return isMoving(mc.thePlayer);
    }

    public static boolean isMoving(EntityLivingBase player) {
        return player != null && (player.moveForward != 0F || player.moveStrafing != 0F);
    }

    public static double getSpeed(EntityPlayer player) {
        return Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
    }

    public static double getSpeed() {
        return getSpeed(mc.thePlayer);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static void strafe(final double speed) {
        if (!isMoving())
            return;

        final double yaw = getDirection();
        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }

    public static void strafe(final double speed, double yaw) {
        if (!isMoving())
            return;

        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }

    public static void strafe(MoveEvent event, double speed) {
        float direction = (float) getDirection();

        if (isMoving()) {
            event.setX(mc.thePlayer.motionX = -Math.sin(direction) * speed);
            event.setZ(mc.thePlayer.motionZ = Math.cos(direction) * speed);
        } else {
            event.setX(mc.thePlayer.motionX = 0);
            event.setZ(mc.thePlayer.motionZ = 0);
        }
    }

    public static void bop(double s) {
        double forward = mc.thePlayer.movementInput.moveForward;
        double strafe = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;
        if (forward == 0.0D && strafe == 0.0D) {
            mc.thePlayer.motionX = 0.0D;
            mc.thePlayer.motionZ = 0.0D;
        } else {
            if (forward != 0.0D) {
                if (strafe > 0.0D) {
                    yaw += (float) (forward > 0.0D ? -45 : 45);
                } else if (strafe < 0.0D) {
                    yaw += (float) (forward > 0.0D ? 45 : -45);
                }

                strafe = 0.0D;
                if (forward > 0.0D) {
                    forward = 1.0D;
                } else if (forward < 0.0D) {
                    forward = -1.0D;
                }
            }

            double rad = Math.toRadians(yaw + 90.0F);
            double sin = Math.sin(rad);
            double cos = Math.cos(rad);
            mc.thePlayer.motionX = forward * s * cos + strafe * s * sin;
            mc.thePlayer.motionZ = forward * s * sin - strafe * s * cos;
        }
    }

    public static float getRawDirectionRotation(float yaw, float pStrafe, float pForward) {
        float rotationYaw = yaw;

        if (pForward < 0F)
            rotationYaw += 180F;

        float forward = 1F;
        if (pForward < 0F)
            forward = -0.5F;
        else if (pForward > 0F)
            forward = 0.5F;

        if (pStrafe > 0F)
            rotationYaw -= 90F * forward;

        if (pStrafe < 0F)
            rotationYaw += 90F * forward;

        return rotationYaw;
    }

    public static float getRawDirection() {
        return getRawDirectionRotation(mc.thePlayer.rotationYaw, mc.thePlayer.moveStrafing, mc.thePlayer.moveForward);
    }

    public static int getSpeedEffect(EntityPlayer player) {
        return player.isPotionActive(Potion.moveSpeed) ? player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 : 0;
    }

    public static int getSpeedEffect() {
        return getSpeedEffect(mc.thePlayer);
    }

    public static void stopXZ() {
        mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
    }

    public static void stop() {
        mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
    }

    public static double getBPS() {
        return getBPS(mc.thePlayer);
    }

    public static double getBPS(EntityPlayer player) {
        if (player == null || player.ticksExisted < 1) {
            return 0.0;
        }
        return getDistance(player.lastTickPosX, player.lastTickPosZ) * (20.0f * mc.timer.timerSpeed);
    }

    public static double getDistance(final double x, final double z) {
        final double xSpeed = mc.thePlayer.posX - x;
        final double zSpeed = mc.thePlayer.posZ - z;
        return MathHelper.sqrt_double(xSpeed * xSpeed + zSpeed * zSpeed);
    }

    public static boolean isMovingStraight() {
        float direction = getYawFromKeybind() + 180;
        float movingYaw = Math.round(direction / 45) * 45;
        return movingYaw % 90 == 0f;
    }

    public static boolean canSprint(final boolean legit) {
        return (legit ? mc.thePlayer.moveForward >= 0.8F
                && !mc.thePlayer.isCollidedHorizontally
                && (mc.thePlayer.getFoodStats().getFoodLevel() > 6 || mc.thePlayer.capabilities.allowFlying)
                && !mc.thePlayer.isPotionActive(Potion.blindness)
                //&& !mc.thePlayer.isUsingItem()
                && !mc.thePlayer.isSneaking()
                : enoughMovementForSprinting());
    }

    public static boolean enoughMovementForSprinting() {
        return Math.abs(mc.thePlayer.moveForward) >= 0.8F || Math.abs(mc.thePlayer.moveStrafing) >= 0.8F;
    }

    public static boolean isGoingDiagonally(double amount) {
        return Math.abs(mc.thePlayer.motionX) > amount && Math.abs(mc.thePlayer.motionZ) > amount;
    }

    public static double getBaseMoveSpeed(EntityPlayer player) {
        double baseSpeed = 0.2873;
        if (player.isPotionActive(Potion.moveSpeed)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        return baseSpeed;
    }

    public static double getBaseMoveSpeed() {
        return getBaseMoveSpeed(mc.thePlayer);
    }

    public static double getJumpHeight() {
        double jumpY = BASE_JUMP_HEIGHT;

        if (mc.thePlayer.isPotionActive(Potion.jump)) {
            jumpY += (float) (mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;
        }

        return jumpY;
    }

    public static void jump(MoveEvent event) {
        event.setY(mc.thePlayer.motionY = getJumpHeight());
    }

    public static int depthStriderLevel() {
        if (mc.thePlayer == null)
            return 0;
        return EnchantmentHelper.getDepthStriderModifier(mc.thePlayer);
    }

    public static double getAllowedHorizontalDistance() {
        double horizontalDistance;
        boolean useBaseModifiers = false;

        if (mc.thePlayer.isInWeb) {
            horizontalDistance = MOD_WEB * WALK_SPEED;
        } else if (PlayerUtils.inLiquid()) {
            horizontalDistance = MOD_SWIM * WALK_SPEED;

            final int depthStriderLevel = depthStriderLevel();
            if (depthStriderLevel > 0) {
                horizontalDistance *= MOD_DEPTH_STRIDER[depthStriderLevel];
                useBaseModifiers = true;
            }

        } else if (mc.thePlayer.isSneaking()) {
            horizontalDistance = MOD_SNEAK * WALK_SPEED;
        } else {
            horizontalDistance = WALK_SPEED;
            useBaseModifiers = true;
        }

        if (useBaseModifiers) {
            if (canSprint(false)) {
                horizontalDistance *= MOD_SPRINTING;
            }

            if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration()
                    > 0) {
                horizontalDistance *= 1 + (0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1));
            }

            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
                horizontalDistance = 0.29;
            }
        }

        return horizontalDistance;
    }

    public static double predictedMotionY(final double motion, final int ticks) {
        if (ticks == 0) return motion;
        double predicted = motion;

        for (int i = 0; i < ticks; i++) {
            predicted = (predicted - 0.08) * 0.98F;
        }

        return predicted;
    }

    public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public static void fixMovement(MoveInputEvent event, float yaw) {
        fixMovement(event, yaw, mc.thePlayer.rotationYaw);
    }

    public static void fixMovement(MoveInputEvent event, float yaw, float playerYaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();

        if (forward == 0 && strafe == 0) return;

        double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(playerYaw, forward, strafe)));

        float closestForward = 0, closestStrafe = 0;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedForward == 0 && predictedStrafe == 0) continue;

                double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(yaw, predictedForward, predictedStrafe)));
                double difference = Math.abs(MathHelper.wrapAngleTo180_double(angle - predictedAngle));

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }

    public static void moveFlying(double increase) {
        if (!MoveUtil.isMoving()) return;
        final double yaw = getDirection();
        mc.thePlayer.motionX += -MathHelper.sin((float) yaw) * increase;
        mc.thePlayer.motionZ += MathHelper.cos((float) yaw) * increase;
    }

    public static void preventDiagonalSpeed() {
        KeyBinding[] gameSettings = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft};

        final int[] down = {0};

        Arrays.stream(gameSettings).forEach(keyBinding -> down[0] = down[0] + (keyBinding.isKeyDown() ? 1 : 0));

        boolean active = down[0] == 1;

        if (active) return;

        final double groundIncrease = (0.1299999676734952 - 0.12739998266255503) + 1E-7 - 1E-8;
        final double airIncrease = (0.025999999334873708 - 0.025479999685988748) - 1E-8;
        final double increase = mc.thePlayer.onGround ? groundIncrease : airIncrease;

        moveFlying(-increase);
    }

    public static void useDiagonalSpeed() {
        KeyBinding[] gameSettings = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft};

        final int[] down = {0};

        Arrays.stream(gameSettings).forEach(keyBinding -> down[0] = down[0] + (keyBinding.isKeyDown() ? 1 : 0));

        boolean active = down[0] == 1;

        if (!active) return;

        final double groundIncrease = (0.1299999676734952 - 0.12739998266255503) + 1E-7 - 1E-8;
        final double airIncrease = (0.025999999334873708 - 0.025479999685988748) - 1E-8;
        final double increase = mc.thePlayer.onGround ? groundIncrease : airIncrease;

        moveFlying(increase);
    }

    public static void setSpeed(double speed) {
        double forward = mc.gameSettings.keyBindForward.isKeyDown()
                ? 1.0
                : (mc.gameSettings.keyBindBack.isKeyDown() ? -1.0 : 0.0);
        double strafe = mc.gameSettings.keyBindLeft.isKeyDown()
                ? 1.0
                : (mc.gameSettings.keyBindRight.isKeyDown() ? -1.0 : 0.0);
        float yaw = mc.thePlayer.rotationYaw;

        if (isMoving()) {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += (float) (forward > 0.0 ? -45 : 45);
                } else if (strafe < 0.0) {
                    yaw += (float) (forward > 0.0 ? 45 : -45);
                }

                strafe = 0.0;
                if (forward > 0.0) {
                    forward = 1.0;
                } else if (forward < 0.0) {
                    forward = -1.0;
                }
            }

            double cos = Math.cos(Math.toRadians(yaw + 89.5F));
            double sin = Math.sin(Math.toRadians(yaw + 89.5F));
            mc.thePlayer.motionX = forward * speed * cos + strafe * speed * sin;
            mc.thePlayer.motionZ = forward * speed * sin - strafe * speed * cos;
        } else {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    public static void smoothStrafe(MotionEvent e) {
        double deltaYaw = MathHelper.wrapAngleTo180_double(Math.toDegrees(getDirection()) - lastYaw2);
        double maxAngle = 20;
        double angle = lastYaw2 + (deltaYaw > 0 ? maxAngle : -maxAngle);

        if (Math.abs(deltaYaw) < maxAngle) angle = Math.toDegrees(getDirection());

        if (isMoving()) {
            if (mc.thePlayer.onGround) {
                angle = lastYaw2 = (float) Math.toDegrees(getDirection());
            }

            MoveUtil.strafe(MoveUtil.getSpeed(), Math.toRadians(angle));
        }

        lastYaw2 = MathHelper.wrapAngleTo180_float((float) angle);
        e.setYaw((float) angle);
    }

    private static float yaw = 0;

    public static float getDir() {
        if (mc.gameSettings.keyBindForward.isKeyDown() && mc.gameSettings.keyBindLeft.isKeyDown()) {
            yaw = 45f;
        } else if (mc.gameSettings.keyBindForward.isKeyDown() && mc.gameSettings.keyBindRight.isKeyDown()) {
            yaw = -45f;
        } else if (mc.gameSettings.keyBindBack.isKeyDown() && mc.gameSettings.keyBindLeft.isKeyDown()) {
            yaw = 135f;
        } else if (mc.gameSettings.keyBindBack.isKeyDown() && mc.gameSettings.keyBindRight.isKeyDown()) {
            yaw = -135f;
        } else if (mc.gameSettings.keyBindBack.isKeyDown()) {
            yaw = 180f;
        } else if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            yaw = 90f;
        } else if (mc.gameSettings.keyBindRight.isKeyDown()) {
            yaw = -90f;
        } else if (mc.gameSettings.keyBindForward.isKeyDown()) {
            yaw = 0f;
        }

        return yaw;
    }

    public static float getYawFromKeybind() {
        return mc.thePlayer.rotationYaw - getDir();
    }

    public static float getDirection() {
        float dir;

        TargetStrafe targetStrafe = Demise.INSTANCE.getModuleManager().getModule(TargetStrafe.class);

        if (targetStrafe.isEnabled() && targetStrafe.active && targetStrafe.target != null) {
            dir = targetStrafe.yaw;
        } else {
            dir = mc.thePlayer.rotationYaw - getDir();
        }

        return (float) toRadians(dir);
    }

    public static double getDirection(float moveForward, float moveStrafing, float rotationYaw) {
        if (moveForward < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (moveForward < 0) {
            forward = -0.5F;
        } else if (moveForward > 0) {
            forward = 0.5F;
        }

        if (moveStrafing > 0) {
            rotationYaw -= 70 * forward;
        }

        if (moveStrafing < 0) {
            rotationYaw += 70 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static void holdS(MoveInputEvent e) {
        final float forward = e.getForward();
        final float strafe = e.getStrafe();

        final double angle = MathHelper.wrapAngleTo180_double(RotationManager.currentRotation[0] - 180);
        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(MoveUtil.getDirection(predictedForward, predictedStrafe, mc.thePlayer.rotationYaw)));
                final double difference = MathUtils.wrappedDifference(angle, predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        e.setForward(closestForward);
        e.setStrafe(closestStrafe);
    }
}