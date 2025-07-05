package wtf.demise.features.modules.impl.misc;

import net.minecraft.client.settings.KeyBinding;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Twerk", description = "Automatically acts like popular girls at my school.")
public class Twerk extends Module {
    private final SliderValue delay = new SliderValue("Delay", 2, 1, 20, 1, this);

    @EventTarget
    public void onTick(TickEvent e) {
        KeyBinding.setKeyBindState(
                mc.gameSettings.keyBindSneak.getKeyCode(), mc.thePlayer.ticksExisted % (delay.get() * 2) < delay.get()
        );
    }
}
