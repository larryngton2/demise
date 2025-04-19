package wtf.demise.features.modules.impl.misc;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.ChatUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@ModuleInfo(name = "AutoMeow", category = ModuleCategory.Misc)
public class AutoMeow extends Module {
    private final SliderValue delay = new SliderValue("Delay (sec)", 1, 0, 200, 1, this);
    private final SliderValue lengthMin = new SliderValue("Length (min)", 5, 1, 50, 1, this);
    private final SliderValue lengthMax = new SliderValue("Length (max)", 10, 1, 50, 1, this);

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

    private long lastMessageTime = 0;

    @Override
    public void onEnable() {
        lastMessageTime = System.currentTimeMillis();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime > (delay.get() * 50)) {
            int minLength = (int) lengthMin.get();
            int maxLength = (int) lengthMax.get();
            int messagesToSend = ThreadLocalRandom.current().nextInt(minLength, maxLength + 1);

            String concatenatedMessage = generateMessages(messagesToSend);
            ChatUtils.sendMessageServer(concatenatedMessage);

            lastMessageTime = currentTime;
        }
    }

    private String generateMessages(int count) {
        return ThreadLocalRandom.current()
                .ints(count, 0, baseMessages.size())
                .mapToObj(baseMessages::get)
                .collect(Collectors.joining(" "));
    }
}