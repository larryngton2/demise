package wtf.demise.features.modules.impl.misc;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.features.values.impl.TextValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "Spammer", description = "Spams.")
public class Spammer extends Module {
    private final TextValue string = new TextValue("String", "hi", this);
    private final SliderValue delay = new SliderValue("Delay", 5, 1, 100, 1, this);

    private final TimerUtils timer = new TimerUtils();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (timer.hasTimeElapsed(delay.get() * 50L)) {
            ChatUtils.sendMessageServer(string.get());
            timer.reset();
        }
    }
}
