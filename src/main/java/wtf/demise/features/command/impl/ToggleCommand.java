package wtf.demise.features.command.impl;

import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.features.command.CommandExecutionException;
import wtf.demise.features.modules.Module;
import wtf.demise.utils.misc.ChatUtils;

public final class ToggleCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"toggle", "t"};
    }

    @Override
    public void execute(final String[] arguments) throws CommandExecutionException {
        if (arguments.length == 2) {
            final String moduleName = arguments[1];
            for (final Module module : Demise.INSTANCE.getModuleManager().getModules()) {
                if (module.getName().replaceAll(" ", "").equalsIgnoreCase(moduleName)) {
                    module.toggle();
                    ChatUtils.sendMessageClient(module.getName() + " has been " + (module.isEnabled() ? "\u00a7AEnabled\u00a77." : "\u00a7CDisabled\u00a77."));
                    return;
                }
            }
        }
        throw new CommandExecutionException(this.getUsage());
    }

    @Override
    public String getUsage() {
        return "toggle/t <module name>";
    }
}
