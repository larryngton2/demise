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
        if (arguments.length != 2) {
            throw new CommandExecutionException(getUsage());
        }

        String targetModuleName = arguments[1];
        Module module = findModuleByName(targetModuleName);

        if (module == null) {
            throw new CommandExecutionException(getUsage());
        }

        toggleModuleAndNotify(module);
    }

    private Module findModuleByName(String moduleName) {
        return Demise.INSTANCE.getModuleManager().getModules().stream()
                .filter(module -> module.getName().replaceAll(" ", "").equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
    }

    private void toggleModuleAndNotify(Module module) {
        module.toggle();
        String status = module.isEnabled() ? "§AEnabled" : "§CDisabled.";
        ChatUtils.sendMessageClient(module.getName() + " has been " + status);
    }

    @Override
    public String getUsage() {
        return "toggle/t <module name>";
    }
}