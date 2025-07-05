package wtf.demise.features.command.impl;

import net.minecraft.util.EnumChatFormatting;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.features.command.CommandExecutionException;
import wtf.demise.features.modules.Module;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.misc.StringUtils;

public class BindCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"bind", "b"};
    }

    @Override
    public void execute(final String[] arguments) throws CommandExecutionException {
        if (arguments.length == 3) {
            handleModuleBinding(arguments[1], arguments[2]);
        } else if (arguments.length == 2) {
            handleSpecialCommands(arguments[1]);
        } else {
            throw new CommandExecutionException(getUsage());
        }
    }

    private void handleModuleBinding(String moduleName, String keyName) {
        Module targetModule = findModuleByName(moduleName);
        if (targetModule == null) {
            ChatUtils.sendMessageClient("Could not find module.");
            return;
        }

        int keyBind = Keyboard.getKeyIndex(keyName.toUpperCase());
        targetModule.setKeyBind(keyBind);
        String message = formatBindingMessage(targetModule);
        ChatUtils.sendMessageClient(message);
    }

    private void handleSpecialCommands(String action) {
        if (action.equalsIgnoreCase("clear")) {
            clearAllBindings();
        } else if (action.equalsIgnoreCase("list")) {
            listAllBindings();
        }
    }

    private Module findModuleByName(String moduleName) {
        return Demise.INSTANCE.getModuleManager().getModules().stream()
                .filter(module -> module.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
    }

    private void clearAllBindings() {
        Demise.INSTANCE.getModuleManager().getModules()
                .forEach(module -> module.setKeyBind(0));
        ChatUtils.sendMessageClient("Cleared all binds.");
    }

    private void listAllBindings() {
        ChatUtils.sendMessageClient("Binds: ");
        Demise.INSTANCE.getModuleManager().getModules().stream()
                .filter(module -> module.getKeyBind() != 0)
                .forEach(this::displayModuleBinding);
    }

    private void displayModuleBinding(Module module) {
        String bindingInfo = String.format("%s- %s%s: %s",
                EnumChatFormatting.GRAY,
                EnumChatFormatting.RED,
                module.getName(),
                Keyboard.getKeyName(module.getKeyBind()));
        ChatUtils.sendMessageClient(bindingInfo);
    }

    private String formatBindingMessage(Module module) {
        return String.format("Set %s to %s.",
                module.getName(),
                StringUtils.upperSnakeCaseToPascal(Keyboard.getKeyName(module.getKeyBind())));
    }

    @Override
    public String getUsage() {
        return "bind <module> <key> | clear | list";
    }
}