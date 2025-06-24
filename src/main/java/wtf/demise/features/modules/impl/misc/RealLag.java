package wtf.demise.features.modules.impl.misc;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.packet.LagUtils;

@ModuleInfo(name = "RealLag", description = "FakeLag, but real.", category = ModuleCategory.Misc)
public class RealLag extends Module {
    private final SliderValue ping = new SliderValue("Ping", 100, 0, 1000, 1, this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(String.valueOf(ping.get()));
        LagUtils.spoof((int) ping.get(), true, true, true, true, true, true, false);
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        LagUtils.disable();
        LagUtils.dispatch();
    }

    @Override
    public void onDisable() {
        LagUtils.disable();
        LagUtils.dispatch();
    }
}
