package wtf.demise.features.command.impl;

import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.features.command.CommandExecutionException;
import wtf.demise.utils.misc.ChatUtils;

import static wtf.demise.utils.InstanceAccess.mc;

public class FriendCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"friend", "f", "fr"};
    }

    @Override
    public void execute(final String[] arguments) throws CommandExecutionException {
        if (arguments.length == 1) {
            ChatUtils.sendMessageClient("Usage: " + getUsage());
            return;
        }
        final String lowerCase = arguments[1].toLowerCase();
        if (arguments.length == 2) {
            switch (lowerCase) {
                case "clear": {
                    ChatUtils.sendMessageClient("Cleared all friended players");
                    Demise.INSTANCE.getFriendManager().getFriends().clear();
                    break;
                }
                case "list": {
                    if (!Demise.INSTANCE.getFriendManager().getFriends().isEmpty()) {
                        ChatUtils.sendMessageClient("Friend§7[§f" + Demise.INSTANCE.getFriendManager().getFriends().size() + "§7]§f : §a" + Demise.INSTANCE.getFriendManager().getFriendsName());
                        break;
                    }
                    ChatUtils.sendMessageClient("The friend list is empty");
                    break;
                }
            }
        } else {
            if (arguments.length != 3) {
                throw new CommandExecutionException(this.getUsage());
            }
            if (arguments[2].contains(mc.thePlayer.getName())) {
                ChatUtils.sendMessageClient("§c§lNO");
                return;
            }
            final String lowerCase2 = arguments[1].toLowerCase();
            switch (lowerCase2) {
                case "add": {
                    ChatUtils.sendMessageClient("§b" + arguments[2] + " §7has been §2friended");
                    Demise.INSTANCE.getFriendManager().add(arguments[2]);
                    break;
                }
                case "remove": {
                    ChatUtils.sendMessageClient("§b" + arguments[2] + " §7has been §2unfriended");
                    Demise.INSTANCE.getFriendManager().remove(arguments[2]);
                    break;
                }
            }
        }
    }

    @Override
    public String getUsage() {
        return "friend add <name> | remove <name> | list | clear";
    }
}
