package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.rotation.RotationUtils;

@ModuleInfo(name = "AutoRod", description = "Automatically rods. Just read man.")
public class AutoRod extends Module {
    private final SliderValue minRange = new SliderValue("Min range", 3, 1, 8, 0.1f, this);
    private final SliderValue maxRange = new SliderValue("Max range", 4.5f, 1, 8, 0.1f, this);
    private final SliderValue maxDelay = new SliderValue("Max delay", 100, 0, 1000, 5, this);
    private final SliderValue maxRecastDelay = new SliderValue("Max recast delay", 100, 0, 1000, 5, this);
    private final SliderValue fov = new SliderValue("Fov", 90, 0, 360, 1, this);
    private final BoolValue rotate = new BoolValue("Rotate", true, this);
    private final SliderValue predictSize = new SliderValue("Predict size", 2, 0.1f, 10, 0.1f, this, rotate::get);
    private final RotationManager rotationManager = new RotationManager(this);
    private final BoolValue onlyOnKillAura = new BoolValue("Only on kill aura", false, this);
    public final BoolValue overrideAuraRots = new BoolValue("Override kill aura rots", false, this);

    private final TimerUtils recastTimer = new TimerUtils();
    private final TimerUtils delayTimer = new TimerUtils();
    private boolean usingRod;
    private int oldSlot;
    private static EntityLivingBase currentTarget;
    public boolean rotating;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        rotationManager.updateRotSpeed(e);

        currentTarget = PlayerUtils.getTarget(maxRange.get());

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
        if (!getModule(KillAura.class).isEnabled() && onlyOnKillAura.get()) {
            rotating = false;
            return;
        }

        double range = PlayerUtils.getDistanceToEntityBox(currentTarget);

        rotating = rotate.get() && KillAura.currentTarget == null && range > minRange.get() && range <= maxRange.get() && mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemFishingRod;

        if (rotating) {
            float[] finalRotation = RotationUtils.faceTrajectory(currentTarget, true, predictSize.get(), 0.03f, 2f);

            rotationManager.setRotation(finalRotation);
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
