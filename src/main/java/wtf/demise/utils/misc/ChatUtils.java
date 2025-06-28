package wtf.demise.utils.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import wtf.demise.utils.InstanceAccess;

@UtilityClass
public class ChatUtils implements InstanceAccess {
    public void sendMessageClient(String message) {
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.DARK_GRAY + "demise" + EnumChatFormatting.GRAY + " » " + EnumChatFormatting.WHITE + message));
    }

    public void sendMessageClient(boolean message) {
        sendMessageClient(String.valueOf(message));
    }

    public void sendMessageClient(int message) {
        sendMessageClient(String.valueOf(message));
    }

    public void sendMessageClient(float message) {
        sendMessageClient(String.valueOf(message));
    }

    public void sendMessageClient(double message) {
        sendMessageClient(String.valueOf(message));
    }

    public void sendMessageServer(String message) {
        mc.thePlayer.sendChatMessage(message);
    }
}