package wtf.demise.features.command.impl;

import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.features.command.CommandExecutionException;
import wtf.demise.features.friend.FriendManager;
import wtf.demise.utils.misc.ChatUtils;
import static wtf.demise.utils.InstanceAccess.mc;

public class FriendCommand extends Command {
    private final FriendManager friendManager = Demise.INSTANCE.getFriendManager();

    @Override
    public String[] getAliases() {
        return new String[]{"friend", "f", "fr"};
    }

    @Override
    public void execute(final String[] args) throws CommandExecutionException {
        if (args.length < 2) {
            showUsage();
            return;
        }

        String operation = args[1].toLowerCase();
        switch (operation) {
            case "clear" -> handleClear();
            case "list" -> handleList();
            case "add", "remove" -> {
                if (args.length != 3) {
                    throw new CommandExecutionException(getUsage());
                }
                handlePlayerOperation(operation, args[2]);
            }
            default -> throw new CommandExecutionException(getUsage());
        }
    }

    private void showUsage() {
        ChatUtils.sendMessageClient("Usage: " + getUsage());
    }

    private void handleClear() {
        friendManager.getFriends().clear();
        ChatUtils.sendMessageClient("Cleared all friended players");
    }

    private void handleList() {
        if (friendManager.getFriends().isEmpty()) {
            ChatUtils.sendMessageClient("The friend list is empty");
            return;
        }

        String message = String.format("Friend§7[§f%d§7]§f : §a%s",
                friendManager.getFriends().size(),
                friendManager.getFriendsName());
        ChatUtils.sendMessageClient(message);
    }

    private void handlePlayerOperation(String operation, String playerName) {
        if (playerName.contains(mc.thePlayer.getName())) {
            ChatUtils.sendMessageClient("§c§lNO");
            return;
        }

        if (operation.equalsIgnoreCase("list")) {
            friendManager.add(playerName);
            ChatUtils.sendMessageClient("§b" + playerName + " §7has been §2friended");
        } else {
            friendManager.remove(playerName);
            ChatUtils.sendMessageClient("§b" + playerName + " §7has been §2unfriended");
        }
    }

    @Override
    public String getUsage() {
        return "friend add <name> | remove <name> | list | clear";
    }
}