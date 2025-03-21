package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.killaura.KillAura;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.RotationUtils;
import wtf.demise.utils.player.SmoothMode;

@ModuleInfo(name = "AutoRod", category = ModuleCategory.Combat)
public class AutoRod extends Module {
    private final SliderValue minRange = new SliderValue("Min range", 3, 1, 8, 0.1f, this);
    private final SliderValue maxRange = new SliderValue("Max range", 4.5f, 1, 8, 0.1f, this);
    private final SliderValue delay = new SliderValue("Delay", 100, 0, 1000, 5, this);
    private final SliderValue recastDelay = new SliderValue("Recast delay", 100, 0, 1000, 5, this);
    private final SliderValue fov = new SliderValue("Fov", 90, 0, 360, 1, this);
    private final BoolValue rotate = new BoolValue("Rotate", true, this);
    private final SliderValue predictSize = new SliderValue("Predict Size", 2, 0.1f, 5, 0.1f, this, rotate::get);
    private final ModeValue smoothMode = new ModeValue("Smooth mode", new String[]{"Linear", "Lerp", "Bezier"}, "Linear", this, rotate::get);
    private final SliderValue yawRotationSpeedMin = new SliderValue("Yaw rotation speed (min)", 180, 0.01f, 180, 0.01f, this, rotate::get);
    private final SliderValue yawRotationSpeedMax = new SliderValue("Yaw rotation speed (max)", 180, 0.01f, 180, 0.01f, this, rotate::get);
    private final SliderValue pitchRotationSpeedMin = new SliderValue("Pitch rotation speed (min)", 180, 0.01f, 180, 0.01f, this, rotate::get);
    private final SliderValue pitchRotationSpeedMax = new SliderValue("Pitch rotation speed (max)", 180, 0.01f, 180, 0.01f, this, rotate::get);
    private final SliderValue midpoint = new SliderValue("Midpoint", 0.3f, 0.01f, 1, 0.01f, this, () -> rotate.get() && smoothMode.is("Bezier"));
    private final BoolValue movementFix = new BoolValue("Movement fix", true, this, rotate::get);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);

    private final TimerUtils recastTimer = new TimerUtils();
    private final TimerUtils delayTimer = new TimerUtils();
    private boolean usingRod;
    private int oldSlot;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        EntityLivingBase currentTarget = PlayerUtils.getTarget(maxRange.get() + 5, teamCheck.get());

        if (currentTarget == null || !mc.thePlayer.canEntityBeSeen(currentTarget) || mc.thePlayer.isUsingItem()) {
            return;
        }

        double range = PlayerUtils.getDistanceToEntityBox(currentTarget);

        if (range > minRange.get() && range <= maxRange.get()) {
            if (rotate.get() && KillAura.currentTarget == null) {
                float[] finalRotation = RotationUtils.faceTrajectory(currentTarget, true, predictSize.get(), 0.03f, 2f);

                SmoothMode mode = SmoothMode.valueOf(smoothMode.get());
                MovementCorrection correction = movementFix.get() ? MovementCorrection.Silent : MovementCorrection.None;

                switch (mode) {
                    case Linear:
                        RotationUtils.setRotation(finalRotation, correction,
                                MathUtils.randomizeInt((int) yawRotationSpeedMin.get(), (int) yawRotationSpeedMax.get()),
                                MathUtils.randomizeInt((int) pitchRotationSpeedMin.get(), (int) pitchRotationSpeedMax.get()),
                                SmoothMode.Linear
                        );
                        break;
                    case Lerp:
                        RotationUtils.setRotation(finalRotation, correction,
                                MathUtils.randomizeInt((int) yawRotationSpeedMin.get(), (int) yawRotationSpeedMax.get()),
                                MathUtils.randomizeInt((int) pitchRotationSpeedMin.get(), (int) pitchRotationSpeedMax.get()),
                                SmoothMode.Lerp
                        );
                        break;
                    case Bezier:
                        RotationUtils.setRotation(finalRotation, correction,
                                MathUtils.randomizeInt((int) yawRotationSpeedMin.get(), (int) yawRotationSpeedMax.get()),
                                MathUtils.randomizeInt((int) pitchRotationSpeedMin.get(), (int) pitchRotationSpeedMax.get()),
                                SmoothMode.Bezier, midpoint.get()
                        );
                        break;
                }
            }

            if (RotationUtils.getRotationDifference(currentTarget) <= fov.get()) {
                if (!usingRod && delayTimer.hasTimeElapsed((long) delay.get())) {
                    int rod = findRod();

                    if (rod == -1) {
                        return;
                    }

                    useRod();
                    delayTimer.reset();
                    recastTimer.reset();
                } else if (recastTimer.hasTimeElapsed((long) recastDelay.get())) {
                    mc.thePlayer.inventory.currentItem = oldSlot;
                    SpoofSlotUtils.stopSpoofing();
                    mc.playerController.updateController();
                    usingRod = false;
                }
            }
        } else if (usingRod) {
            mc.thePlayer.inventory.currentItem = oldSlot;
            SpoofSlotUtils.stopSpoofing();
            mc.playerController.updateController();
            usingRod = false;
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

        usingRod = true;
    }
}
