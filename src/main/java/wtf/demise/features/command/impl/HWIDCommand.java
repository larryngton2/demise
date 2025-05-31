package wtf.demise.features.command.impl;

import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.utils.misc.ChatUtils;

public class HWIDCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"hwid", "id"};
    }

    @Override
    public String getUsage() {
        return "hwid / id";
    }

    @Override
    public void execute(String[] arguments) {
        ChatUtils.sendMessageClient("Your HWID is: " + Demise.HWID.getHWID());
    }
}
