package wtf.demise.features.command.impl;

import net.minecraft.client.Minecraft;
import wtf.demise.features.command.Command;

public class JumpCommand extends Command {
    @Override
    public String[] getAliases() {
        return new String[]{"jump", "j"};
    }

    @Override
    public void execute(final String[] arguments) {
        Minecraft.getMinecraft().thePlayer.jump();
    }

    @Override
    public String getUsage() {
        return "jump/j";
    }
}