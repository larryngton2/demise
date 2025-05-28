package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.AntiBot;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.PlayerUtils;

import java.util.Arrays;

/**
 * YES, I am using code from KillAura. FUCK YOU
 */
@ModuleInfo(name = "AutoWalk", description = "w a l k", category = ModuleCategory.Movement)
public class AutoWalk extends Module {
    private final BoolValue target = new BoolValue("Target player", false, this);
    private final BoolValue rotate = new BoolValue("Rotate", true, this, target::get);
    private final SliderValue minRange = new SliderValue("Min range", 1.5f, 0, 15, 0.1f, this, target::get);
    private final MultiBoolValue allowedTargets = new MultiBoolValue("Allowed targets", Arrays.asList(
            new BoolValue("Players", true),
            new BoolValue("Non players", true),
            new BoolValue("Teams", true),
            new BoolValue("Bots", false),
            new BoolValue("Invisibles", false),
            new BoolValue("Dead", false)
    ), this, target::get);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!target.get()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
        } else {
            EntityLivingBase target = findTarget();
            if (target != null) {

                //nah

                /*
                if (rotate.get()) {
                    mc.thePlayer.rotationYaw = RotationUtils.limitRotations(RotationUtils.serverRotation, calcToEntity(target), 25, 45, 1, 90, SmoothMode.Linear)[0];
                    mc.thePlayer.rotationPitch = RotationUtils.limitRotations(RotationUtils.serverRotation, calcToEntity(target), 25, 45, 1, 90, SmoothMode.Linear)[1];
                }

                 */

                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), PlayerUtils.getDistanceToEntityBox(target) > minRange.get());
            }
        }
    }

    public EntityLivingBase findTarget() {
        EntityLivingBase target = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            double distanceToEntity = PlayerUtils.getDistanceToEntityBox(entity);

            if (entity != mc.thePlayer) {
                if (!(entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager || entity instanceof EntityPlayer)) {
                    continue;
                }

                if (entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager) {
                    if (!allowedTargets.isEnabled("Non players")) continue;
                }

                if (entity.isInvisible() && !allowedTargets.isEnabled("Invisibles")) continue;
                if (entity.isDead && !allowedTargets.isEnabled("Dead")) continue;

                if (entity instanceof EntityPlayer) {
                    if (!allowedTargets.isEnabled("Players")) continue;
                    if (Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity)) continue;
                    if (!allowedTargets.isEnabled("Bots") && getModule(AntiBot.class).isBot((EntityPlayer) entity))
                        continue;
                    if (PlayerUtils.isInTeam(entity) && !allowedTargets.isEnabled("Teams")) continue;
                }

                if (distanceToEntity < closestDistance) {
                    target = (EntityLivingBase) entity;
                    closestDistance = distanceToEntity;
                }
            }
        }

        return target;
    }

    public float[] calcToEntity(EntityLivingBase entity) {
        float yaw;
        float pitch;
        Vec3 currentVec;

        Vec3 playerPos = mc.thePlayer.getPositionEyes(1);

        AxisAlignedBB entityBoundingBox = entity.getEntityBoundingBox();

        final double ex = (entityBoundingBox.maxX + entityBoundingBox.minX) / 2;
        final double ey = MathHelper.clamp_double(playerPos.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY);
        final double ez = (entityBoundingBox.maxZ + entityBoundingBox.minZ) / 2;

        currentVec = new Vec3(ex, ey, ez);

        double deltaX = currentVec.xCoord - playerPos.xCoord;
        double deltaY = currentVec.yCoord - playerPos.yCoord;
        double deltaZ = currentVec.zCoord - playerPos.zCoord;

        yaw = (float) -(Math.atan2(deltaX, deltaZ) * (180.0 / Math.PI));
        pitch = (float) (-Math.toDegrees(Math.atan2(deltaY, Math.hypot(deltaX, deltaZ))));

        pitch = MathHelper.clamp_float(pitch, -90, 90);

        return new float[]{yaw, pitch};
    }
}
