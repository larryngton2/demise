package wtf.demise.utils.misc;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import wtf.demise.utils.InstanceAccess;

public class ChatUtils implements InstanceAccess {
    public static void sendMessageClient(String message) {
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "demise" + EnumChatFormatting.GRAY + " » " + EnumChatFormatting.WHITE + message));
    }

    public static void sendMessageServer(String message) {
        mc.thePlayer.sendChatMessage(message);
    }
}
