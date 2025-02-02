package demise.client.command.commands;


import demise.client.clickgui.demise.Terminal;
import demise.client.command.Command;
import demise.client.main.demise;

public class Help extends Command {
    public Help() {
        super("help", "Shows you different command usages", 0, 1, new String[]{"name of module"}, new String[]{"?", "wtf", "what"});
    }

    @Override
    public void onCall(String[] args) {
        if (args.length == 0) {
            demise.commandManager.sort();

            Terminal.print("Available commands:");
            int index = 1;
            for (Command command : demise.commandManager.getCommandList()) {
                if (command.getName().equalsIgnoreCase("help"))
                    continue;

                Terminal.print(index + ") " + command.getName());
                index++;
            }

            Terminal.print("Run \"help commandname\" for more information about the command");
        } else if (args.length == 1) {
            Command command = demise.commandManager.getCommandByName(args[0]);
            if (command == null) {
                Terminal.print("Unable to find a command with the cname or alias with " + args[0]);
                return;
            }

            Terminal.print(command.getName() + "'s info:");
            if (command.getAliases() != null || command.getAliases().length != 0) {
                Terminal.print(command.getName() + "'s aliases:");
                for (String alias : command.getAliases()) {
                    Terminal.print(alias);
                }
            }

            if (!command.getHelp().isEmpty()) {
                Terminal.print(command.getName() + "'s description:");
                Terminal.print(command.getHelp());
            }

            if (command.getArgs() != null) {
                Terminal.print(command.getName() + "'s argument description:");
                Terminal.print("Min args: " + command.getMinArgs() + ", max args: " + command.getMaxArgs());
                int argIndex = 1;
                for (String argText : command.getArgs()) {
                    Terminal.print("Argument " + argIndex + ": " + argText);
                    argIndex++;
                }
            }
        }
    }
}