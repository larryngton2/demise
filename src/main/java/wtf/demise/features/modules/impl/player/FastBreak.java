package wtf.demise.features.modules.impl.player;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "FastBreak", description = "Allows you to break blocks faster.")
public class FastBreak extends Module {
    private final BoolValue legit = new BoolValue("Legit", true, this);
    private final SliderValue speed = new SliderValue("Speed", 0.5f, 0, 0.9f, 0.1f, this, () -> !legit.get());

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(legit.get() ? "Legit" : "+" + speed.get());

        mc.playerController.blockHitDelay = 0;

        if (!legit.get() && mc.playerController.curBlockDamageMP > 1 - speed.get()) {
            mc.playerController.curBlockDamageMP = 1;
        }
    }
}