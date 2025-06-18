package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
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
import wtf.demise.utils.player.rotation.RotationHandler;

import java.util.Arrays;

@ModuleInfo(name = "AutoWalk", description = "w a l k", category = ModuleCategory.Movement)
public class AutoWalk extends Module {
    private final BoolValue target = new BoolValue("Target player", false, this);
    private final BoolValue rotate = new BoolValue("Rotate", true, this, target::get);
    private final RotationHandler rotationHandler = new RotationHandler(this);
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
                rotationHandler.updateRotSpeed(e);

                if (rotate.get()) {
                    rotationHandler.setRotation(rotationHandler.getSimpleRotationsToEntity(target));
                }

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
}
