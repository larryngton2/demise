package wtf.demise.features.modules.impl.legit;

import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.PlayerTickEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "AutoClicker", category = ModuleCategory.Legit)
public class AutoClicker extends Module {
    private final BoolValue left = new BoolValue("Left click", true, this);
    private final SliderValue lminCPS = new SliderValue("CPS (left min)", 10, 1, 20, this, left::get);
    private final SliderValue lmaxCPS = new SliderValue("CPS (left max)", 12, 1, 20, this, left::get);
    private final BoolValue breakBlocks = new BoolValue("Break blocks (left)", true, this, left::get);

    private final BoolValue right = new BoolValue("Right click", true, this);
    private final SliderValue rminCPS = new SliderValue("CPS (right min)", 10, 1, 20, this, right::get);
    private final SliderValue rmaxCPS = new SliderValue("CPS (right max)", 12, 1, 20, this, right::get);

    private final TimerUtils leftTimer = new TimerUtils();
    private final TimerUtils rightTimer = new TimerUtils();

    @Override
    public void onEnable() {
        leftTimer.reset();
        rightTimer.reset();
    }

    private boolean isLeftReady() {
        return leftTimer.hasTimeElapsed(1000 / (ThreadLocalRandom.current().nextInt((int) lminCPS.get(), (int) lmaxCPS.get() + 1) * 1.5));
    }

    private boolean isRightReady() {
        return rightTimer.hasTimeElapsed(1000 / (ThreadLocalRandom.current().nextInt((int) rminCPS.get(), (int) rmaxCPS.get() + 1) * 1.5));
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent e) {
        if (e.state == PlayerTickEvent.State.PRE) {
            if (right.get()) {
                if (mc.gameSettings.keyBindUseItem.isKeyDown() && isRightReady()) {
                    mc.rightClickMouse();
                    rightTimer.reset();
                }
            }

            if (left.get()) {
                if (breakBlocks.get() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
                    return;

                if (mc.gameSettings.keyBindAttack.isKeyDown() && isLeftReady() && !mc.thePlayer.isUsingItem()) {
                    mc.clickMouse();
                    leftTimer.reset();
                }
            }
        }
    }
}
