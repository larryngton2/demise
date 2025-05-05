package wtf.demise.features.modules.impl.misc;

import net.minecraft.network.play.server.S02PacketChat;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.features.values.impl.TextValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "AutoRegister", description = "Automatically registers you in servers.", category = ModuleCategory.Misc)
public class AutoRegister extends Module {
    private final TextValue password = new TextValue("Password", "FoxesAreCute", this);
    private final SliderValue delay = new SliderValue("Delay", 5, 0, 20, 1, this);

    private final TimerUtils timer = new TimerUtils();
    private boolean received;
    private boolean reg;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (timer.hasTimeElapsed(delay.get() * 50) && received) {
            ChatUtils.sendMessageServer((reg ? "/reg " : "/register ") + password.get() + " " + password.get());
            received = false;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            if (e.getPacket() instanceof S02PacketChat s02PacketChat) {
                if (s02PacketChat.getChatComponent().getUnformattedText().contains("/reg")) {
                    reg = true;
                    received = true;
                    timer.reset();
                } else if (s02PacketChat.getChatComponent().getUnformattedText().contains("/register")) {
                    reg = false;
                    received = true;
                    timer.reset();
                }
            }
        }
    }
}