package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;

//sucks ass
@ModuleInfo(name = "HitSelect", description = "Automatically hit selects in order to start combos.")
public class HitSelect extends Module {
    private final SliderValue maxWaitTime = new SliderValue("Max wait time", 500, 100, 1000, this);

    public boolean blockClicking;
    private boolean wTap;
    private boolean shouldClick;
    private boolean startedCombo;

    private final TimerUtils resetTimer = new TimerUtils();
    private final TimerUtils clickTimer = new TimerUtils();
    private final TimerUtils maxWaitTimer = new TimerUtils();
    private boolean waitTimerReset;

    @EventTarget
    public void onGameEvent(GameEvent e) {
        EntityLivingBase target = PlayerUtils.getTarget(8);

        if (target == null) {
            blockClicking = false;
            startedCombo = false;
            shouldClick = false;
            return;
        }

        float calcYaw = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
        float diffX = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - target.rotationYawHead));

        if (diffX > 120) {
            blockClicking = false;
            startedCombo = false;
            shouldClick = false;
            return;
        }

        boolean playerHurt = mc.thePlayer.hurtTime > 0;
        boolean targetHurt = target.hurtTime > 0;

        if (!playerHurt && !targetHurt) {
            if (resetTimer.hasTimeElapsed(250)) {
                startedCombo = false;
                clickTimer.reset();
                shouldClick = false;
            }
        } else {
            resetTimer.reset();
        }

        if (PlayerUtils.getDistanceToEntityBox(target) < 3) {
            if (!waitTimerReset) {
                maxWaitTimer.reset();
                waitTimerReset = true;
            }
        } else {
            waitTimerReset = false;
        }

        if (!playerHurt && !targetHurt && maxWaitTimer.hasTimeElapsed(maxWaitTime.get())) {
            clickTimer.setTime(clickTimer.getTime() + 999999999);
            shouldClick = true;
            startedCombo = true;
        }

        if (!startedCombo) {
            if (playerHurt && !targetHurt) {
                if (clickTimer.hasTimeElapsed(100)) {
                    shouldClick = true;
                    startedCombo = true;
                    wTap = true;
                } else {
                    blockClicking = true;
                    return;
                }
            } else {
                blockClicking = true;
                return;
            }
        }

        blockClicking = !shouldClick && (!playerHurt || !targetHurt);
    }

    @EventTarget
    public void onMovementInput(MoveInputEvent e) {
        if (wTap) {
            e.setForward(0);
            wTap = false;
        }
    }
}
