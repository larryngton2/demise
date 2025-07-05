package wtf.demise.features.command.impl;

import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.utils.misc.ChatUtils;

import java.util.Arrays;

public class HelpCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"help", "h"};
    }

    @Override
    public void execute(final String[] arguments) {
        for (Command command : Demise.INSTANCE.getCommandManager().cmd) {
            if (!(command instanceof ModuleCommand))
                ChatUtils.sendMessageClient(Arrays.toString(command.getAliases()) + ": " + command.getUsage());
        }
    }

    @Override
    public String getUsage() {
        return "help/h";
    }
}
