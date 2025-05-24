package wtf.demise.features.modules.impl.misc;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "Test", category = ModuleCategory.Misc)
public class Test extends Module {
    private final BoolValue pTicks = new BoolValue("partial ticks", false, this);

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        if (pTicks.get()) {
            ChatUtils.sendMessageClient(String.valueOf(mc.timer.partialTicks));
        }
    }
}