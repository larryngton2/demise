package wtf.demise.features.modules.impl.misc;

import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import org.json.JSONArray;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ModuleInfo(name = "Translator", description = "Translates your chat.", category = ModuleCategory.Misc)
public class Translator extends Module {
    Executor translatorThread = Executors.newFixedThreadPool(1);

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            if (mc.theWorld == null || mc.thePlayer == null) return;

            if (e.getPacket() instanceof S02PacketChat s02) {
                if (!s02.isChat()) {
                    return;
                }

                String text = StringUtils.stripControlCodes(s02.getChatComponent().getFormattedText());

                if (text.contains("\n")) {
                    return;
                }

                e.setCancelled(true);
                sendTranslatedMessage(text);
            }
        }
    }

    public void sendTranslatedMessage(String text) {
        this.translatorThread.execute(() -> {
            JSONArray array = null;

            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=" + URLEncoder.encode(text)).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                array = new JSONArray(reader.readLine());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (array != null) {
                String translated = array.getJSONArray(0).getJSONArray(0).getString(0);
                ChatComponentText translatedComponent = new ChatComponentText(translated);
                String language = new Locale(array.getString(2)).getDisplayLanguage(Locale.ENGLISH);

                if (!translated.equals(text)) {
                    translatedComponent.appendText(" ");
                    ChatComponentText hoverComponent = new ChatComponentText(EnumChatFormatting.DARK_GRAY + "[T]");
                    ChatStyle style = new ChatStyle();
                    style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Translated from " + language + "\n" + text)));
                    hoverComponent.setChatStyle(style);

                    translatedComponent.appendSibling(hoverComponent);
                }

                mc.thePlayer.addChatMessage(translatedComponent);
            }
        });
    }
}