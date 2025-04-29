package wtf.demise.utils.discord;

import lombok.Getter;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiSelectWorld;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.misc.ServerUtils;
import wtf.demise.utils.player.PlayerUtils;

public class DiscordInfo implements InstanceAccess {
    private boolean running = true;
    private long timeElapsed = 0;
    @Getter
    private String name;
    @Getter
    private String id;
    @Getter
    private String smallImageText;

    public int getTotal() {
        return INSTANCE.getModuleManager().getModules().size();
    }

    public long getCount() {
        return INSTANCE.getModuleManager().getModules().stream().filter(Module::isEnabled).count();
    }

    public void init() {
        this.timeElapsed = System.currentTimeMillis();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().setReadyEventHandler(discordUser -> {
            System.out.println("[Discord] Connected to user " + discordUser.username + "#" + discordUser.discriminator);
            if (discordUser.userId != null) {
                name = discordUser.username + (discordUser.discriminator.equals("0") ? "" : discordUser.discriminator);
            } else {
                System.exit(0);
            }
        }).build();

        DiscordRPC.discordInitialize("1365297079034970112", handlers, true);
        new Thread("Discord RPC Callback") {
            @Override
            public void run() {
                while (running) {
                    if (mc.thePlayer != null) {
                        if (mc.isSingleplayer()) {
                            update("Ign: " + detectUsername(), "Playing in Singleplayer", true);
                        } else if (mc.getCurrentServerData() != null) {
                            update("Ign: " + detectUsername(), "Playing on " + PlayerUtils.getCurrServer(), true);
                        } else if (mc.currentScreen instanceof GuiDownloadTerrain) {
                            update("...", "", false);
                        }
                    } else {
                        if (mc.currentScreen instanceof GuiSelectWorld) {
                            update("Selecting World...", "", false);
                        } else if (mc.currentScreen instanceof GuiMultiplayer) {
                            update("Selecting Server...", "", false);
                        } else if (mc.currentScreen instanceof GuiDownloadTerrain) {
                            update("...", "", false);
                        } else {
                            update("Idling...", "", false);
                        }
                    }

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    DiscordRPC.discordRunCallbacks();
                }
            }
        }.start();
    }

    public void stop() {
        running = false;
        DiscordRPC.discordShutdown();
    }

    public String detectUsername() {
        String string;
        string = mc.thePlayer.getName();

        return string;
    }

    public void update(String line1, String line2, Boolean smallImage) {
        DiscordRichPresence.Builder rpc = new DiscordRichPresence.Builder(line2).setDetails(line1).setBigImage("logo", "demise [#" + INSTANCE.version + "]");
        if (smallImage) {
            rpc.setSmallImage("closer", smallImageText);
        }
        rpc.setStartTimestamps(timeElapsed);
        DiscordRPC.discordUpdatePresence(rpc.build());
    }

    public void updateSmallImageText(String text) {
        smallImageText = text;
    }
}