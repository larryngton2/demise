package wtf.demise.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import wtf.demise.Demise;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.impl.movement.TargetStrafe;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.rotation.RotationHandler;

import java.util.Arrays;

@UtilityClass
public class MoveUtil implements InstanceAccess {
    public double BASE_JUMP_HEIGHT = 0.41999998688698;

    public boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.moveForward != 0F || mc.thePlayer.moveStrafing != 0F);
    }

    public boolean isMovingMotion(EntityLivingBase player) {
        return player != null && (player.motionX != 0 || player.motionZ != 0);
    }

    public double getSpeed(EntityPlayer player) {
        return Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
    }

    public double getSpeed() {
        return getSpeed(mc.thePlayer);
    }

    public void strafe() {
        strafe(getSpeed(), 1);
    }

    public void strafe(double speed) {
        strafe(speed, 1);
    }

    public void strafe(double speed, double strength) {
        double motionX = mc.thePlayer.motionX;
        double motionZ = mc.thePlayer.motionZ;

        if (!isMoving()) return;

        double yaw = getDirection();
        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;

        mc.thePlayer.motionX = motionX + (mc.thePlayer.motionX - motionX) * strength;
        mc.thePlayer.motionZ = motionZ + (mc.thePlayer.motionZ - motionZ) * strength;
    }

    public void bop(double s) {
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

    public float getRawDirectionRotation(float yaw, float pStrafe, float pForward) {
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

    public float getRawDirection() {
        return getRawDirectionRotation(mc.thePlayer.rotationYaw, mc.thePlayer.moveStrafing, mc.thePlayer.moveForward);
    }

    public int getSpeedEffect(EntityPlayer player) {
        return player.isPotionActive(Potion.moveSpeed) ? player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 : 0;
    }

    public int getSpeedEffect() {
        return getSpeedEffect(mc.thePlayer);
    }

    public void stopXZ() {
        mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
    }

    public void stop() {
        mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
    }

    public double getBPS() {
        return getBPS(mc.thePlayer);
    }

    public double getBPS(EntityPlayer player) {
        if (player == null || player.ticksExisted < 1) {
            return 0.0;
        }
        return getDistance(player.lastTickPosX, player.lastTickPosZ) * (20.0f * mc.timer.timerSpeed);
    }

    public double getDistance(double x, double z) {
        double xSpeed = mc.thePlayer.posX - x;
        double zSpeed = mc.thePlayer.posZ - z;
        return MathHelper.sqrt_double(xSpeed * xSpeed + zSpeed * zSpeed);
    }

    public boolean isMovingStraight() {
        float direction = getYawFromKeybind() + 180;
        float movingYaw = Math.round(direction / 45) * 45;
        return movingYaw % 90 == 0f;
    }

    public boolean canSprint(boolean legit) {
        return (legit ? mc.thePlayer.moveForward >= 0.8F
                && !mc.thePlayer.isCollidedHorizontally
                && (mc.thePlayer.getFoodStats().getFoodLevel() > 6 || mc.thePlayer.capabilities.allowFlying)
                && !mc.thePlayer.isPotionActive(Potion.blindness)
                //&& !mc.thePlayer.isUsingItem()
                && !mc.thePlayer.isSneaking()
                : enoughMovementForSprinting());
    }

    public boolean enoughMovementForSprinting() {
        return Math.abs(mc.thePlayer.moveForward) >= 0.8F || Math.abs(mc.thePlayer.moveStrafing) >= 0.8F;
    }

    public boolean isGoingDiagonally(double amount) {
        return Math.abs(mc.thePlayer.motionX) > amount && Math.abs(mc.thePlayer.motionZ) > amount;
    }

    public double getBaseMoveSpeed(EntityPlayer player) {
        double baseSpeed = 0.2873;
        if (player.isPotionActive(Potion.moveSpeed)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        return baseSpeed;
    }

    public double getBaseMoveSpeed() {
        return getBaseMoveSpeed(mc.thePlayer);
    }

    public double getJumpHeight() {
        double jumpY = BASE_JUMP_HEIGHT;

        if (mc.thePlayer.isPotionActive(Potion.jump)) {
            jumpY += (float) (mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;
        }

        return jumpY;
    }

    public void jump(MoveEvent event) {
        event.setY(mc.thePlayer.motionY = getJumpHeight());
    }

    public double predictedMotionY(double motion, int ticks) {
        if (ticks == 0) return motion;
        double predicted = motion;

        for (int i = 0; i < ticks; i++) {
            predicted = (predicted - 0.08) * 0.98F;
        }

        return predicted;
    }

    public double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public void fixMovement(MoveInputEvent event, float yaw) {
        fixMovement(event, yaw, mc.thePlayer.rotationYaw);
    }

    public void fixMovement(MoveInputEvent event, float yaw, float playerYaw) {
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

    public void moveFlying(double increase) {
        if (!MoveUtil.isMoving()) return;
        double yaw = getDirection();
        mc.thePlayer.motionX += -MathHelper.sin((float) yaw) * increase;
        mc.thePlayer.motionZ += MathHelper.cos((float) yaw) * increase;
    }

    public void useDiagonalSpeed() {
        KeyBinding[] gameSettings = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft};

        int[] down = {0};

        Arrays.stream(gameSettings).forEach(keyBinding -> down[0] = down[0] + (keyBinding.isKeyDown() ? 1 : 0));

        boolean active = down[0] == 1;

        if (!active) return;

        double groundIncrease = (0.1299999676734952 - 0.12739998266255503) + 1E-7 - 1E-8;
        double airIncrease = (0.025999999334873708 - 0.025479999685988748) - 1E-8;
        double increase = mc.thePlayer.onGround ? groundIncrease : airIncrease;

        moveFlying(increase);
    }

    private float yaw = 0;

    public float getDir() {
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

    public float getYawFromKeybind() {
        return mc.thePlayer.rotationYaw - getDir();
    }

    public float getDirection() {
        float dir;

        TargetStrafe targetStrafe = Demise.INSTANCE.getModuleManager().getModule(TargetStrafe.class);

        if (targetStrafe.isEnabled() && targetStrafe.active && targetStrafe.target != null) {
            dir = targetStrafe.yaw;
        } else {
            dir = getYawFromKeybind();
        }

        return (float) Math.toRadians(dir);
    }

    public double getDirection(float moveForward, float moveStrafing, float rotationYaw) {
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

    public void holdS(MoveInputEvent e) {
        float forward = e.getForward();
        float strafe = e.getStrafe();

        double angle = MathHelper.wrapAngleTo180_double(RotationHandler.currentRotation[0] - 180);
        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(MoveUtil.getDirection(predictedForward, predictedStrafe, mc.thePlayer.rotationYaw)));
                double difference = MathUtils.wrappedDifference(angle, predictedAngle);

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