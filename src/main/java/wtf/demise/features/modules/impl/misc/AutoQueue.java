package wtf.demise.features.modules.impl.misc;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "AutoQueue", description = "Automatically re-queues when a match is over.")
public class AutoQueue extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Hypixel", "Mineblaze duels", "Mineberry duels"}, "Hypixel", this);

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            Packet<?> packet = e.getPacket();

            if (packet instanceof S02PacketChat chat) {
                switch (mode.get()) {
                    case "Hypixel":
                        if (chat.isChat()) return;
                        if (chat.getChatComponent().getFormattedText().contains("play again?")) {
                            for (IChatComponent iChatComponent : chat.getChatComponent().getSiblings()) {
                                for (String value : iChatComponent.toString().split("'")) {
                                    if (value.startsWith("/play") && !value.contains(".")) {
                                        ChatUtils.sendMessageServer(value);

                                        Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Auto Queue", "Joined a new game", 5);
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case "Mineblaze duels":
                        if (chat.isChat()) return;
                        if (chat.getChatComponent().getFormattedText().contains("Новая игра")) {
                            ChatUtils.sendMessageServer("/next");
                            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Auto Queue", "Joined a new game", 5);
                        }
                        break;
                    case "Mineberry duels":
                        if (chat.isChat()) return;
                        if (chat.getChatComponent().getFormattedText().contains("New Game")) {
                            ChatUtils.sendMessageServer("/next");
                            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Auto Queue", "Joined a new game", 5);
                        }
                        break;
                }
            }
        }
    }
}
