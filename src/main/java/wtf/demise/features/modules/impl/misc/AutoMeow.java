package wtf.demise.features.modules.impl.misc;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@ModuleInfo(name = "AutoMeow", description = "meow")
public class AutoMeow extends wtf.demise.features.modules.Module {
    private final SliderValue delay = new SliderValue("Delay", 1, 0, 200, 1, this);
    private final SliderValue minLength = new SliderValue("Min length", 5, 1, 50, 1, this);
    private final SliderValue maxLength = new SliderValue("Max length", 10, 1, 50, 1, this);

    private final List<String> baseMessages = Arrays.asList(
            "nya",
            "nya~",
            "mew",
            "meow",
            "mrrp",
            ":3",
            "meowmeow",
            "mow"
    );

    private final TimerUtils timer = new TimerUtils();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (timer.hasTimeElapsed(delay.get() * 50L)) {
            int messagesToSend = MathUtils.randomizeInt(minLength.get(), maxLength.get());

            String concatenatedMessage = generateMessages(messagesToSend);
            mc.thePlayer.sendChatMessage(concatenatedMessage);

            timer.reset();
        }
    }

    private String generateMessages(int count) {
        return ThreadLocalRandom.current()
                .ints(count, 0, baseMessages.size())
                .mapToObj(baseMessages::get)
                .collect(Collectors.joining(" "));
    }
}
