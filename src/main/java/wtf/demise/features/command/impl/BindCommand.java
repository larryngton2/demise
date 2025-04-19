package wtf.demise.features.command.impl;

import net.minecraft.util.EnumChatFormatting;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.features.command.CommandExecutionException;
import wtf.demise.features.modules.Module;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.misc.StringUtils;

public final class BindCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"bind", "b"};
    }

    @Override
    public void execute(final String[] arguments) throws CommandExecutionException {
        if (arguments.length == 3) {
            final String moduleName = arguments[1];
            final String keyName = arguments[2];
            boolean foundModule = false;

            for (final Module module : Demise.INSTANCE.getModuleManager().getModules()) {
                if (module.getName().equalsIgnoreCase(moduleName)) {
                    module.setKeyBind(Keyboard.getKeyIndex(keyName.toUpperCase()));
                    final String string = "Set " + module.getName() + " to " + StringUtils.upperSnakeCaseToPascal(Keyboard.getKeyName(module.getKeyBind())) + ".";
                    ChatUtils.sendMessageClient(string);
                    foundModule = true;
                    break;
                }
            }

            if (!foundModule) {
                ChatUtils.sendMessageClient("Cound not find module.");
            }
        } else {
            if (arguments.length != 2) {
                throw new CommandExecutionException(this.getUsage());
            }

            if (arguments[1].equalsIgnoreCase("clear")) {
                for (final Module module2 : Demise.INSTANCE.getModuleManager().getModules()) {
                    module2.setKeyBind(0);
                    ChatUtils.sendMessageClient("Cleared all binds.");
                }
            } else if (arguments[1].equalsIgnoreCase("list")) {
                ChatUtils.sendMessageClient("Binds");
                for (final Module module2 : Demise.INSTANCE.getModuleManager().getModules()) {
                    if (module2.getKeyBind() != 0) {
                        ChatUtils.sendMessageClient(EnumChatFormatting.GRAY + "- " + EnumChatFormatting.RED + module2.getName() + ": " + Keyboard.getKeyName(module2.getKeyBind()));
                    }
                }
            }
        }
    }

    @Override
    public String getUsage() {
        return "bind <module> <key> | clear | list";
    }
}
