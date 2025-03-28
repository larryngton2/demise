package wtf.demise.features.command.impl;

import wtf.demise.features.command.Command;
import wtf.demise.userinfo.HWID;
import wtf.demise.utils.misc.DebugUtils;

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
    public void execute(final String[] arguments) {
        DebugUtils.sendMessage("Your HWID is: " + HWID.getHWID());
    }
}
