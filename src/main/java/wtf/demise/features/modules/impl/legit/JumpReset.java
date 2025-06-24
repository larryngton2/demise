package wtf.demise.features.modules.impl.legit;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "JumpReset", description = "Automatically jump resets in order to reduce velocity.", category = ModuleCategory.Legit)
public class JumpReset extends Module {
    private final SliderValue chance = new SliderValue("Chance", 50, 1, 100, 1, this);
    private final SliderValue minDelay = new SliderValue("Min delay", 0, 0, 500, 1, this);
    private final SliderValue maxDelay = new SliderValue("Max delay", 0, 0, 500, 1, this);

    private final TimerUtils timer = new TimerUtils();
    private boolean jump;
    private boolean gotHit;
    private int delay;
    private int lastHurtTime;

    @EventTarget
    public void onGame(GameEvent e) {
        setTag(String.valueOf(chance.get()));

        if (mc.thePlayer.hurtTime == 10 && mc.thePlayer.hurtTime != lastHurtTime && rand.nextInt(100) <= chance.get()) {
            gotHit = true;
            delay = MathUtils.randomizeInt(minDelay.get(), maxDelay.get());
            timer.reset();
        }
        lastHurtTime = mc.thePlayer.hurtTime;

        if (timer.hasTimeElapsed(delay) && !jump && gotHit) {
            //KeyBinding.onTick(mc.gameSettings.keyBindJump.getKeyCode());
            mc.thePlayer.movementInput.jump = true;
            ChatUtils.sendMessageClient("Jumped");
            jump = true;
            timer.reset();
            gotHit = false;
        }

        if (mc.thePlayer.onGround) {
            jump = false;
        }
    }

    /*
    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (jump) {
            e.setJumping(true);
            ChatUtils.sendMessageClient("sfsdgdff");
            if (mc.thePlayer.hurtTime == 10) {
                ChatUtils.sendMessageClient("sfsdgdff but better");
            }
            jump = false;
        }
    }

     */
}