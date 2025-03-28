package wtf.demise.userinfo;

import net.minecraft.util.EnumChatFormatting;

public class CurrentUser {
    public static String USER;
    public static String USER_TAG;
    public static String FINAL_USER;

    public CurrentUser() {
        USER = switch (HWID.getHWID()) {
            case "54d9bd1099e90efc87990a0ff8ea4111" -> "larryngton";
            default -> null;
        };

        USER_TAG = switch (HWID.getHWID()) {
            case "54d9bd1099e90efc87990a0ff8ea4111" -> "dev";
            default -> null;
        };

        if (USER != null) {
            if (USER_TAG != null) {
                FINAL_USER = USER + EnumChatFormatting.GRAY + " (" + USER_TAG + ")" + EnumChatFormatting.WHITE;
            } else {
                FINAL_USER = USER;
            }
        } else {
            FINAL_USER = "null";
        }
    }
}
