package net.minecraft.command.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;

public class CommandSaveAll extends CommandBase {
    public String getCommandName() {
        return "save-all";
    }

    public String getCommandUsage(ICommandSender sender) {
        return "commands.save.usage";
    }

    public void processCommand(ICommandSender sender, String[] args) {
        MinecraftServer minecraftserver = MinecraftServer.getServer();
        sender.addChatMessage(new ChatComponentTranslation("commands.save.start"));

        if (minecraftserver.getConfigurationManager() != null) {
            minecraftserver.getConfigurationManager().saveAllPlayerData();
        }

        try {
            for (int i = 0; i < minecraftserver.worldServers.length; ++i) {
                if (minecraftserver.worldServers[i] != null) {
                    WorldServer worldserver = minecraftserver.worldServers[i];
                    boolean flag = worldserver.disableLevelSaving;
                    worldserver.disableLevelSaving = false;
                    worldserver.saveAllChunks(true, null);
                    worldserver.disableLevelSaving = flag;
                }
            }

            if (args.length > 0 && "flush".equals(args[0])) {
                sender.addChatMessage(new ChatComponentTranslation("commands.save.flushStart"));

                for (int j = 0; j < minecraftserver.worldServers.length; ++j) {
                    if (minecraftserver.worldServers[j] != null) {
                        WorldServer worldserver1 = minecraftserver.worldServers[j];
                        boolean flag1 = worldserver1.disableLevelSaving;
                        worldserver1.disableLevelSaving = false;
                        worldserver1.saveChunkData();
                        worldserver1.disableLevelSaving = flag1;
                    }
                }

                sender.addChatMessage(new ChatComponentTranslation("commands.save.flushEnd"));
            }
        } catch (MinecraftException minecraftexception) {
            notifyOperators(sender, this, "commands.save.failed", minecraftexception.getMessage());
            return;
        }

        notifyOperators(sender, this, "commands.save.success");
    }
}
