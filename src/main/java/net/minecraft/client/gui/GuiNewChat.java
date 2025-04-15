package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class GuiNewChat extends Gui {
    private static final Logger logger = LogManager.getLogger();
    private final Minecraft mc;
    @Getter
    private final List<String> sentMessages = Lists.newArrayList();
    private final List<ChatLine> chatLines = Lists.newArrayList();
    public static final List<ChatLine> drawnChatLines = Lists.newArrayList();
    public static int scrollPos;
    public static boolean isScrolled;
    private String lastMessage;
    private int sameMessageAmount;
    private int line;
    private final HashMap<String, String> stringCache = new HashMap<>();

    public GuiNewChat(Minecraft mcIn) {
        this.mc = mcIn;
    }

    public void clearChatMessages() {
        drawnChatLines.clear();
        this.chatLines.clear();
        this.sentMessages.clear();
    }

    public void printChatMessage(IChatComponent chatComponent) {
        if (Demise.INSTANCE.getModuleManager().getModule(Interface.class).isEnabled() && Demise.INSTANCE.getModuleManager().getModule(Interface.class).chatCombine.get()) {
            String text = fixString(chatComponent.getFormattedText());
            if (text.equals(this.lastMessage)) {
                (Minecraft.getMinecraft()).ingameGUI.getChatGUI().deleteChatLine(this.line);
                this.sameMessageAmount++;
                this.lastMessage = text;
                chatComponent.appendText(EnumChatFormatting.WHITE + " (" + "x" + this.sameMessageAmount + ")");
            } else {
                this.sameMessageAmount = 1;
                this.lastMessage = text;
            }
            this.line++;
            if (this.line > 256)
                this.line = 0;

            printChatMessageWithOptionalDeletion(chatComponent, this.line);
        } else {
            printChatMessageWithOptionalDeletion(chatComponent, this.line);
        }
    }

    private String fixString(String str) {
        if (stringCache.containsKey(str)) return stringCache.get(str);

        str = str.replaceAll("\uF8FF", ""); //remove air chars

        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if ((int) c > (33 + 65248) && (int) c < (128 + 65248))
                sb.append(Character.toChars((int) c - 65248));
            else
                sb.append(c);
        }

        String result = sb.toString();
        stringCache.put(str, result);

        return result;
    }

    public void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId) {
        this.setChatLine(chatComponent, chatLineId, GuiIngame.getUpdateCounter(), false);
        logger.info("[CHAT] " + chatComponent.getUnformattedText());
    }

    private void setChatLine(IChatComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly) {
        if (chatLineId != 0) {
            this.deleteChatLine(chatLineId);
        }

        int i = MathHelper.floor_float((float) getChatWidth() / getChatScale());
        List<IChatComponent> list = GuiUtilRenderComponents.splitText(chatComponent, i, this.mc.fontRendererObj, false, false);
        boolean flag = getChatOpen();

        for (IChatComponent ichatcomponent : list) {
            if (flag && scrollPos > 0) {
                isScrolled = true;
                this.scroll(1);
            }

            drawnChatLines.add(0, new ChatLine(updateCounter, ichatcomponent, chatLineId));
        }

        while (drawnChatLines.size() > 100) {
            drawnChatLines.remove(drawnChatLines.size() - 1);
        }

        if (!displayOnly) {
            this.chatLines.add(0, new ChatLine(updateCounter, chatComponent, chatLineId));

            while (this.chatLines.size() > 100) {
                this.chatLines.remove(this.chatLines.size() - 1);
            }
        }
    }

    public void refreshChat() {
        drawnChatLines.clear();
        this.resetScroll();

        for (int i = this.chatLines.size() - 1; i >= 0; --i) {
            ChatLine chatline = this.chatLines.get(i);
            this.setChatLine(chatline.getChatComponent(), chatline.getChatLineID(), chatline.getUpdatedCounter(), true);
        }
    }

    public void addToSentMessages(String message) {
        if (this.sentMessages.isEmpty() || !this.sentMessages.get(this.sentMessages.size() - 1).equals(message)) {
            this.sentMessages.add(message);
        }
    }

    public void resetScroll() {
        scrollPos = 0;
        isScrolled = false;
    }

    public void scroll(int amount) {
        scrollPos += amount;
        int i = drawnChatLines.size();

        if (scrollPos > i - getLineCount()) {
            scrollPos = i - getLineCount();
        }

        if (scrollPos <= 0) {
            scrollPos = 0;
            isScrolled = false;
        }
    }

    public IChatComponent getChatComponent(int mouseX, int mouseY) {
        if (!getChatOpen()) {
            return null;
        } else {
            ScaledResolution scaledresolution = new ScaledResolution(this.mc);
            int i = scaledresolution.getScaleFactor();
            float f = getChatScale();
            int j = mouseX / i - 10;
            int k = mouseY / i - 33;
            j = MathHelper.floor_float((float) j / f);
            k = MathHelper.floor_float((float) k / f);

            if (j >= 0 && k >= 0) {
                int l = Math.min(getLineCount(), drawnChatLines.size());

                if (j <= MathHelper.floor_float((float) getChatWidth() / getChatScale()) && k < this.mc.fontRendererObj.FONT_HEIGHT * l + l) {
                    int i1 = k / this.mc.fontRendererObj.FONT_HEIGHT + scrollPos;

                    if (i1 >= 0 && i1 < drawnChatLines.size()) {
                        ChatLine chatline = drawnChatLines.get(i1);
                        int j1 = 0;

                        for (IChatComponent ichatcomponent : chatline.getChatComponent()) {
                            if (ichatcomponent instanceof ChatComponentText) {
                                j1 += this.mc.fontRendererObj.getStringWidth(GuiUtilRenderComponents.func_178909_a(((ChatComponentText) ichatcomponent).getChatComponentText_TextValue(), false));

                                if (j1 > j) {
                                    return ichatcomponent;
                                }
                            }
                        }
                    }

                    return null;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public static boolean getChatOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiChat;
    }

    public void deleteChatLine(int id) {
        Iterator<ChatLine> iterator = drawnChatLines.iterator();

        while (iterator.hasNext()) {
            ChatLine chatline = iterator.next();

            if (chatline.getChatLineID() == id) {
                iterator.remove();
            }
        }

        iterator = this.chatLines.iterator();

        while (iterator.hasNext()) {
            ChatLine chatline1 = iterator.next();

            if (chatline1.getChatLineID() == id) {
                iterator.remove();
                break;
            }
        }
    }

    public static int getChatWidth() {
        return calculateChatboxWidth(Minecraft.getMinecraft().gameSettings.chatWidth);
    }

    public static int getChatHeight() {
        return calculateChatboxHeight(GuiNewChat.getChatOpen() ? Minecraft.getMinecraft().gameSettings.chatHeightFocused : Minecraft.getMinecraft().gameSettings.chatHeightUnfocused);
    }

    public static float getChatScale() {
        return Minecraft.getMinecraft().gameSettings.chatScale;
    }

    public static int calculateChatboxWidth(float scale) {
        int i = 320;
        int j = 40;
        return MathHelper.floor_float(scale * (float) (i - j) + (float) j);
    }

    public static int calculateChatboxHeight(float scale) {
        int i = 180;
        int j = 20;
        return MathHelper.floor_float(scale * (float) (i - j) + (float) j);
    }

    public static int getLineCount() {
        return GuiNewChat.getChatHeight() / 9;
    }
}
