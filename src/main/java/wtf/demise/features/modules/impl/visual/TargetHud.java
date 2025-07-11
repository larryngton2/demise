package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "TargetHud", description = "Renders a widget with the current target's info.")
public class TargetHud extends Module {
    public final BoolValue targetHUDTracking = new BoolValue("TargetHUD tracking", false, this);
    public final SliderValue interpolation = new SliderValue("Interpolation", 0.5f, 0.01f, 1, 0.01f, this, targetHUDTracking::get);
    public final BoolValue centerX = new BoolValue("Center X", true, this, targetHUDTracking::get);
    public final SliderValue offsetX = new SliderValue("Offset X", 0, -100, 100, this, () -> targetHUDTracking.get() && !centerX.get());
    public final SliderValue offsetY = new SliderValue("Offset Y", 100, -25, 200, this, targetHUDTracking::get);

    public static float interpolatedScale;
    public static float targetScale;
    public EntityLivingBase target;
    private final TimerUtils attackTimer = new TimerUtils();

    @EventTarget
    public void onGameEvent(GameEvent e) {
        KillAura aura = getModule(KillAura.class);

        if (!(mc.currentScreen instanceof GuiChat)) {
            if (aura.isEnabled() && KillAura.currentTarget instanceof EntityPlayer) {
                targetScale = 1;
                target = KillAura.currentTarget;
            } else {
                if (attackTimer.hasTimeElapsed(1000)) {
                    targetScale = 0;
                    if (Math.abs(interpolatedScale - targetScale) < 0.01f) {
                        target = null;
                    }
                }
            }
        } else if (target == null) {
            targetScale = 1;
            target = mc.thePlayer;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        KillAura aura = getModule(KillAura.class);

        if ((!aura.isEnabled() || KillAura.currentTarget == null) && e.getTargetEntity() instanceof EntityPlayer) {
            targetScale = 1;
            target = (EntityLivingBase) e.getTargetEntity();
            attackTimer.reset();
        }
    }
}