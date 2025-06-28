package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.DecelerateAnimation;

@ModuleInfo(name = "TargetHud", description = "Renders a widget with the current target's info.", category = ModuleCategory.Visual)
public class TargetHud extends Module {
    public final BoolValue targetHUDTracking = new BoolValue("TargetHUD tracking", false, this);
    public final BoolValue esp = new BoolValue("ESP", false, this, targetHUDTracking::get);
    public final SliderValue interpolation = new SliderValue("Interpolation", 0.5f, 0.01f, 1, 0.01f, this, () -> targetHUDTracking.get() && !esp.get());
    public final BoolValue centerX = new BoolValue("Center X", true, this, targetHUDTracking::get);
    public final SliderValue offsetX = new SliderValue("Offset X", 0, -100, 100, this, () -> targetHUDTracking.get() && !centerX.get());
    public final SliderValue offsetY = new SliderValue("Offset Y", 100, -25, 200, this, targetHUDTracking::get);

    public final DecelerateAnimation decelerateAnimation = new DecelerateAnimation(175, 1);
    public EntityLivingBase target;

    @EventTarget
    public void onTick(TickEvent e) {
        KillAura aura = getModule(KillAura.class);

        if (!(mc.currentScreen instanceof GuiChat)) {
            if (aura.isEnabled()) {
                if (KillAura.currentTarget instanceof EntityPlayer) {
                    target = KillAura.currentTarget;
                }

                if ((esp.get() && targetHUDTracking.get()) || KillAura.currentTarget instanceof EntityPlayer) {
                    decelerateAnimation.setDirection(Direction.FORWARDS);
                }
            }

            if (!(esp.get() && targetHUDTracking.get())) {
                if (!aura.isEnabled() || !(KillAura.currentTarget instanceof EntityPlayer)) {
                    decelerateAnimation.setDirection(Direction.BACKWARDS);
                    if (decelerateAnimation.finished(Direction.BACKWARDS)) {
                        target = null;
                    }
                }
            }
        } else if (target == null) {
            decelerateAnimation.setDirection(Direction.FORWARDS);
            target = mc.thePlayer;
        }
    }
}
