package wtf.demise.features.modules.impl.misc;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.TextValue;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.packet.PacketUtils;

@ModuleInfo(name = "AutoQueue", category = ModuleCategory.Misc)
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
                                        PacketUtils.sendPacket(new C01PacketChatMessage(value));

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
                            PacketUtils.sendPacket(new C01PacketChatMessage("/next"));
                            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Auto Queue", "Joined a new game", 5);
                        }
                        break;
                    case "Mineberry duels":
                        if (chat.isChat()) return;
                        if (chat.getChatComponent().getFormattedText().contains("New Game")) {
                            PacketUtils.sendPacket(new C01PacketChatMessage("/next"));
                            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Auto Queue", "Joined a new game", 5);
                        }
                        break;
                }
            }
        }
    }
}
