package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "AutoClicker", category = ModuleCategory.Legit)
public class AutoClicker extends Module {
    private final SliderValue minAps = new SliderValue("Min Aps", 10, 1, 20, this);
    private final SliderValue maxAps = new SliderValue("Max Aps", 12, 1, 20, this);
    private final BoolValue breakBlocks = new BoolValue("Break Blocks", true, this);
    private final TimerUtils clickTimer = new TimerUtils();

    @Override
    public void onEnable(){
        clickTimer.reset();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (breakBlocks.get() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
            return;

        if (mc.gameSettings.keyBindAttack.isKeyDown()) {
            if (clickTimer.hasTimeElapsed(1000 / MathUtils.nextInt((int) minAps.get(), (int) maxAps.get()))) {
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
                clickTimer.reset();
            }
        }
    }
}
