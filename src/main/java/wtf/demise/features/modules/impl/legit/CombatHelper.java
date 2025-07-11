package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.FakeLag;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;

@ModuleInfo(name = "CombatHelper", description = "Helps you with combat.")
public class CombatHelper extends Module {
    private final SliderValue publicSearchRange = new SliderValue("Public search range", 6, 0.1f, 12, 0.1f, this);

    private final BoolValue comboBreaker = new BoolValue("Combo breaker", true, this);
    private final SliderValue breakerAttackRange = new SliderValue("Breaker attack range", 3, 0.1f, 8, 0.1f, this, comboBreaker::get);

    private final BoolValue keepCombo = new BoolValue("Keep combo", true, this);
    private final SliderValue keepComboAttackRange = new SliderValue("Keep combo attack range", 3, 0.1f, 8, 0.1f, this, keepCombo::get);

    private final BoolValue smartBlocking = new BoolValue("Smart blocking", true, this);
    private final SliderValue blockRange = new SliderValue("Block range", 2, 0.1f, 8, 0.1f, this, smartBlocking::get);

    // works best with FakeLag
    private final BoolValue adaptiveStrafe = new BoolValue("Adaptive strafe", false, this);
    private final BoolValue forceStrafe = new BoolValue("Force strafe", false, this, adaptiveStrafe::get);
    private final SliderValue strafeDistance = new SliderValue("Target strafe distance", 3, 2.5f, 6, 0.1f, this, adaptiveStrafe::get);
    private final SliderValue maxStrafeDistance = new SliderValue("Max strafe distance", 8, 4.5f, 15, 0.1f, this, adaptiveStrafe::get);
    private final SliderValue predictionTicks = new SliderValue("Target Prediction Ticks", 5, 1, 20, 1, this, adaptiveStrafe::get);
    private final BoolValue lagCheck = new BoolValue("Lag check", false, this, adaptiveStrafe::get);

    private boolean isBlocking;
    private EntityLivingBase target;
    private Vec3 lastTargetPos;
    private double lastStrafeYaw;

    @EventTarget
    public void onGame(GameEvent e) {
        if (!getModule(KillAura.class).isEnabled()) {
            target = PlayerUtils.getTarget(publicSearchRange.get() * 2);
        } else {
            target = KillAura.currentTarget;
        }

        if (smartBlocking.get()) {
            if (target == null) {
                if (isBlocking) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    isBlocking = false;
                }
            } else {
                if (mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), target.hurtTime > 3 && PlayerUtils.getDistanceToEntityBox(target) <= blockRange.get());
                    isBlocking = target.hurtTime > 3;
                } else if (isBlocking) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    isBlocking = false;
                }
            }
        }
    }

    private float calculateStrafeYaw(EntityPlayer target) {
        if (!adaptiveStrafe.get() || target == null || (lagCheck.get() && !FakeLag.blinking) || mc.thePlayer.hurtTime != 0) return mc.thePlayer.rotationYaw;

        double playerX = mc.thePlayer.posX;
        double playerZ = mc.thePlayer.posZ;

        double targetMotionX = 0;
        double targetMotionZ = 0;
        if (lastTargetPos != null) {
            targetMotionX = target.posX - lastTargetPos.xCoord;
            targetMotionZ = target.posZ - lastTargetPos.zCoord;
        }
        lastTargetPos = new Vec3(target.posX, target.posY, target.posZ);

        double predictedTargetX = target.posX + targetMotionX * predictionTicks.get();
        double predictedTargetZ = target.posZ + targetMotionZ * predictionTicks.get();

        double relX = playerX - predictedTargetX;
        double relZ = playerZ - predictedTargetZ;
        double distanceToPredicted = Math.sqrt(relX * relX + relZ * relZ);

        double idealDistance = Math.max(strafeDistance.get(), 2.5);
        if (distanceToPredicted < 2.5) {
            idealDistance = 4.0;
        }

        double angleToPredicted = Math.atan2(relZ, relX);

        double angleOffset = (distanceToPredicted > idealDistance) ? -45 : 45;
        angleToPredicted += Math.toRadians(angleOffset);

        double strafeX = predictedTargetX + Math.cos(angleToPredicted) * idealDistance;
        double strafeZ = predictedTargetZ + Math.sin(angleToPredicted) * idealDistance;

        float targetYaw = (float) Math.toDegrees(Math.atan2(strafeZ - playerZ, strafeX - playerX));
        float deltaYaw = MathHelper.wrapAngleTo180_float(targetYaw - (float) lastStrafeYaw);

        targetYaw = (float) lastStrafeYaw + MathHelper.clamp_float(deltaYaw, -20f, 20f);
        lastStrafeYaw = targetYaw;

        return targetYaw - 90f;
    }

    private boolean attemptingToStrafe() {
        return mc.thePlayer.movementInput.moveStrafe != 0 || RotationUtils.getRotationDifferenceClientRot(target) > 25;
    }

    @EventPriority(-100)
    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (target != null) {
            if (comboBreaker.get() && PlayerUtils.getDistanceToEntityBox(target) >= breakerAttackRange.get() && !mc.thePlayer.onGround && mc.thePlayer.hurtTime != 0 && target.hurtTime == 0) {
                MoveUtil.holdS(e);
            }

            if (keepCombo.get() && PlayerUtils.getDistanceToEntityBox(target) < keepComboAttackRange.get() && !target.onGround && target.hurtTime != 0 && mc.thePlayer.hurtTime == 0) {
                MoveUtil.holdS(e);
            }

            if ((attemptingToStrafe() || forceStrafe.get()) && adaptiveStrafe.get() && PlayerUtils.getDistanceToEntityBox(target) < maxStrafeDistance.get()) {
                float strafeYaw = calculateStrafeYaw((EntityPlayer) target);
                MoveUtil.fixMovement(e, RotationHandler.shouldRotate() ? RotationHandler.currentRotation[0] : mc.thePlayer.rotationYaw, strafeYaw);
            }
        }
    }

    @Override
    public void onDisable() {
        lastTargetPos = null;
        lastStrafeYaw = 0;
    }
}