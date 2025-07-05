package wtf.demise.features.command.impl;

import net.minecraft.util.EnumChatFormatting;
import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.features.command.CommandExecutionException;
import wtf.demise.features.modules.Module;
import wtf.demise.utils.misc.ChatUtils;

import java.util.Optional;

public class HideCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"hide", "h", "visible", "v"};
    }

    @Override
    public void execute(final String[] arguments) throws CommandExecutionException {
        if (arguments.length != 2) {
            throw new CommandExecutionException(this.getUsage());
        }

        final String subCommand = arguments[1].toLowerCase();
        switch (subCommand) {
            case "clear" -> handleClearCommand();
            case "list" -> handleListCommand();
            default -> handleToggleModuleVisibility(subCommand);
        }
    }

    private void handleClearCommand() {
        Demise.INSTANCE.getModuleManager().getModules().forEach(module -> module.setHidden(false));
        ChatUtils.sendMessageClient("Cleared all hidden module.");
    }

    private void handleListCommand() {
        ChatUtils.sendMessageClient("Hidden modules: ");
        Demise.INSTANCE.getModuleManager().getModules().stream()
                .filter(Module::isHidden)
                .forEach(module -> ChatUtils.sendMessageClient(EnumChatFormatting.GRAY + "- " + EnumChatFormatting.RED + module.getName()));
    }

    private void handleToggleModuleVisibility(String moduleName) {
        Optional.ofNullable(Demise.INSTANCE.getModuleManager().getModule(moduleName))
                .ifPresent(module -> {
                    module.setHidden(!module.isHidden());
                    String status = module.isHidden() ? "§Chidden§7." : "§Ashown§7.";
                    ChatUtils.sendMessageClient(String.format("%s is now %s", module.getName(), status));
                });
    }

    @Override
    public String getUsage() {
        return "hide <module> | clear | list";
    }
}