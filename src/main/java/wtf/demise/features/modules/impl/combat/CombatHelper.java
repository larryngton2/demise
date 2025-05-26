package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "CombatHelper", description = "Helps you with combat.", category = ModuleCategory.Legit)
public class CombatHelper extends Module {
    private final SliderValue publicSearchRange = new SliderValue("Public search range", 6, 0.1f, 12, 0.1f, this);

    private final BoolValue comboBreaker = new BoolValue("Combo breaker", true, this);
    private final SliderValue breakerAttackRange = new SliderValue("Breaker attack range", 3, 0.1f, 8, 0.1f, this, comboBreaker::get);

    private final BoolValue keepCombo = new BoolValue("Keep combo", true, this);
    private final SliderValue keepComboAttackRange = new SliderValue("Keep combo attack range", 3, 0.1f, 8, 0.1f, this, keepCombo::get);

    private final BoolValue smartBlocking = new BoolValue("Smart blocking", true, this);
    private final SliderValue blockRange = new SliderValue("Block range", 2, 0.1f, 8, 0.1f, this, smartBlocking::get);

    private boolean isBlocking;
    private EntityLivingBase target;
    private final TimerUtils hurtTimer = new TimerUtils();

    @EventTarget
    public void onGame(GameEvent e) {
        if (!getModule(KillAura.class).isEnabled()) {
            target = PlayerUtils.getTarget(publicSearchRange.get() * 2, false);
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

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (target != null) {
            if (comboBreaker.get()) {
                if (mc.thePlayer.hurtTime != 0) {
                    hurtTimer.reset();
                }

                if (PlayerUtils.getDistanceToEntityBox(target) >= breakerAttackRange.get() && !mc.thePlayer.onGround && !hurtTimer.hasTimeElapsed(500) && target.hurtTime == 0) {
                    MoveUtil.holdS(e);
                }
            }

            if (keepCombo.get() && PlayerUtils.getDistanceToEntityBox(target) < keepComboAttackRange.get() && !target.onGround && target.hurtTime != 0 && mc.thePlayer.hurtTime == 0) {
                MoveUtil.holdS(e);
            }
        }
    }
}