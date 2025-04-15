package wtf.demise.features.modules.impl.legit;

import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "JumpReset", category = ModuleCategory.Legit)
public class JumpReset extends Module {
    private final SliderValue chance = new SliderValue("Chance", 75, 1, 100, 1, this);
    private final SliderValue minHurtTime = new SliderValue("Min hurtTime", 7, 0, 9, 1, this);
    private final SliderValue maxHurtTime = new SliderValue("Max hurtTime", 10, 1, 10, 1, this);
    private final SliderValue minDelay = new SliderValue("Min delay", 25, 0, 250, 1, this);
    private final SliderValue maxDelay = new SliderValue("Max delay", 50, 0, 250, 1, this);

    private final TimerUtils timer = new TimerUtils();
    private boolean jumpNow;
    private boolean gotHit;
    private int delay;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(chance.get()));

        if (rand.nextInt(100) <= chance.get()) {
            if (mc.thePlayer.onGround && Range.between(minHurtTime.get(), maxHurtTime.get()).contains((float) mc.thePlayer.hurtTime) && !gotHit) {
                delay = MathUtils.randomizeInt(minDelay.get(), maxDelay.get());
                timer.reset();
                gotHit = true;
            }

            if (gotHit && timer.hasTimeElapsed(delay)) {
                jumpNow = true;
                gotHit = false;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (jumpNow && mc.thePlayer.onGround && Range.between(minHurtTime.get(), maxHurtTime.get()).contains((float) mc.thePlayer.hurtTime)) {
            e.setJumping(true);
            jumpNow = false;
        }
    }
}
