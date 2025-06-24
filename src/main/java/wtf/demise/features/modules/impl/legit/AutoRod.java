package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.Range;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.AntiBot;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;

import java.util.Arrays;

@ModuleInfo(name = "AutoRod", description = "Automatically rods. Just read man.", category = ModuleCategory.Legit)
public class AutoRod extends Module {
    private final SliderValue minRange = new SliderValue("Min range", 3, 1, 8, 0.1f, this);
    private final SliderValue maxRange = new SliderValue("Max range", 4.5f, 1, 8, 0.1f, this);
    private final SliderValue maxDelay = new SliderValue("Max delay", 100, 0, 1000, 5, this);
    private final SliderValue maxRecastDelay = new SliderValue("Max recast delay", 100, 0, 1000, 5, this);
    private final SliderValue fov = new SliderValue("Fov", 90, 0, 360, 1, this);
    private final BoolValue rotate = new BoolValue("Rotate", true, this);
    private final SliderValue predictSize = new SliderValue("Predict Size", 2, 0.1f, 10, 0.1f, this, rotate::get);
    private final RotationHandler rotationHandler = new RotationHandler(this);
    private final BoolValue onlyOnKillAura = new BoolValue("Only on KillAura", false, this);

    private final MultiBoolValue allowedTargets = new MultiBoolValue("Allowed targets", Arrays.asList(
            new BoolValue("Players", true),
            new BoolValue("Non players", true),
            new BoolValue("Teams", true),
            new BoolValue("Bots", false),
            new BoolValue("Invisibles", false),
            new BoolValue("Dead", false)
    ), this);

    private final TimerUtils recastTimer = new TimerUtils();
    private final TimerUtils delayTimer = new TimerUtils();
    private boolean usingRod;
    private int oldSlot;
    private static EntityLivingBase currentTarget;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        rotationHandler.updateRotSpeed(e);

        currentTarget = findTarget();

        if (currentTarget == null || !mc.thePlayer.canEntityBeSeen(currentTarget) || mc.thePlayer.isUsingItem() || (!getModule(KillAura.class).isEnabled() && onlyOnKillAura.get())) {
            if (!getModule(Scaffold.class).isEnabled()) {
                reset();
            }
            return;
        }

        float range = (float) PlayerUtils.getDistanceToEntityBox(currentTarget);

        if (Range.between(minRange.get(), maxRange.get()).contains(range)) {
            if (RotationUtils.getRotationDifference(currentTarget) <= fov.get()) {
                if (!usingRod) {
                    if (delayTimer.hasTimeElapsed(maxDelay.get()) || currentTarget.hurtTime <= 3) {
                        int rod = findRod();

                        if (rod == -1) {
                            return;
                        }

                        useRod();
                        resetInUsingRod();
                    }
                } else {
                    if (recastTimer.hasTimeElapsed(maxRecastDelay.get()) || currentTarget.hurtTime >= 9) {
                        reset();
                    }
                }
            }
        } else {
            reset();
        }
    }

    public EntityLivingBase findTarget() {
        EntityLivingBase target = null;
        double closestDistance = maxRange.get() + 0.4;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            double distanceToEntity = PlayerUtils.getDistanceToEntityBox(entity);

            if (entity != mc.thePlayer && distanceToEntity <= maxRange.get()) {
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

    private void resetInUsingRod() {
        recastTimer.reset();
        usingRod = true;
    }

    private void resetOutsideOfUsingRod() {
        recastTimer.reset();
        delayTimer.reset();
        oldSlot = -1;
        usingRod = false;
    }

    private void reset() {
        if (oldSlot != -1) {
            mc.thePlayer.inventory.currentItem = oldSlot;
        }
        SpoofSlotUtils.stopSpoofing();
        mc.playerController.updateController();
        resetOutsideOfUsingRod();
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if ((!getModule(KillAura.class).isEnabled() && onlyOnKillAura.get())) {
            return;
        }

        double range = PlayerUtils.getDistanceToEntityBox(currentTarget);

        if (rotate.get() && KillAura.currentTarget == null && range > minRange.get() && range <= maxRange.get()) {
            float[] finalRotation = RotationUtils.faceTrajectory(currentTarget, true, predictSize.get(), 0.03f, 2f);

            rotationHandler.setRotation(finalRotation);
        }
    }

    private int findRod() {
        for (int i = 36; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null) {
                if (stack.getItem() instanceof ItemFishingRod) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void useRod() {
        int rod = findRod();

        SpoofSlotUtils.startSpoofing(mc.thePlayer.inventory.currentItem);
        oldSlot = mc.thePlayer.inventory.currentItem;

        mc.thePlayer.inventory.currentItem = rod - 36;

        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventoryContainer.getSlot(rod).getStack());
    }
}
