package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "ComboBreaker", description = "Attempts to break your opponent's combo.", category = ModuleCategory.Combat)
public class ComboBreaker extends Module {
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 0.1f, 8, 0.1f, this);

    private boolean repressW;
    private boolean repressS;

    @EventTarget
    public void onGame(GameEvent e) {
        EntityLivingBase target;

        if (!getModule(KillAura.class).isEnabled()) {
            target = PlayerUtils.getTarget(attackRange.get() * 2, false);
        } else {
            target = KillAura.currentTarget;
        }

        if (target == null) {
            return;
        }

        if (PlayerUtils.getDistanceToEntityBox(target) >= attackRange.get()) {
            if (!mc.thePlayer.onGround && mc.thePlayer.hurtTime != 0 && target.hurtTime == 0) {
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                    repressW = true;
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
                    repressS = true;
                }

                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
            } else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);

                if (repressW) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                    repressW = false;
                }

                if (repressS) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
                    repressS = false;
                }
            }
        } else {
            if (repressW) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                repressW = false;
            }

            if (repressS) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
                repressS = false;
            }
        }
    }
}
