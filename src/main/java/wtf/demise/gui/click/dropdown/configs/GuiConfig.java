package wtf.demise.gui.click.dropdown.configs;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.config.impl.ModuleConfig;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.misc.ChatUtils;

import java.io.File;
import java.io.IOException;

public class GuiConfig extends GuiScreen {
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;

    public GuiConfig() {
        Demise.INSTANCE.getEventManager().unregister(this);
        Demise.INSTANCE.getEventManager().register(this);
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(new GuiButton(0, 10, height - 30, 100, 20, "Back"));

        int totalConfigs = Demise.INSTANCE.getConfigManager().getConfigList().size();
        int maxVisibleConfigs = (height - 50 - 40) / 25;
        int startIndex = (int) (scrollOffset / 25);
        int endIndex = Math.min(startIndex + maxVisibleConfigs, totalConfigs);

        int currentOffset = 0;
        int index = 0;
        for (String config : Demise.INSTANCE.getConfigManager().getConfigList()) {
            if (index >= startIndex && index < endIndex) {
                buttonList.add(new GuiButton(config.hashCode(), width / 2f - 50, 50 + currentOffset, 100, 20, config));
                buttonList.add(new GuiButton(config.hashCode() + 1, width / 2f + 60, 50 + currentOffset, 40, 20, "Save"));
                buttonList.add(new GuiButton(config.hashCode() + 2, width / 2f + 105, 50 + currentOffset, 40, 20, "Delete"));
                currentOffset += 25;
            }
            index++;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            int totalConfigs = Demise.INSTANCE.getConfigManager().getConfigList().size();
            int maxVisibleConfigs = (height - 50 - 40) / 25;
            float maxScroll = Math.max(0, (totalConfigs - maxVisibleConfigs) * 25);

            float scrollAmount = delta > 0 ? -25 : 25;
            targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset + scrollAmount));
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (mc.currentScreen != this) return;
        //RoundedUtils.drawRound(width / 2f - 100, 10, width / 2f + 100, height - 30, 7, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true));
        Fonts.interRegular.get(20).drawCenteredString("Config manager", width / 2f, 15, -1);
        Fonts.interRegular.get(18).drawCenteredString("Current config: " + Demise.INSTANCE.getConfigManager().getCurrentConfig(), width / 2f, 30, -1);

        scrollOffset += (targetScrollOffset - scrollOffset) * 0.5f;
        if (Math.abs(targetScrollOffset - scrollOffset) > 0.1) {
            initGui();
        }
    }

    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 0) {
                Demise.INSTANCE.getEventManager().unregister(this);
                mc.displayGuiScreen(Demise.INSTANCE.getDropdownGUI());
            }

            for (String config : Demise.INSTANCE.getConfigManager().getConfigList()) {
                if (button.id == config.hashCode()) {
                    ModuleConfig cfg = new ModuleConfig(config);
                    if (Demise.INSTANCE.getConfigManager().loadConfig(cfg)) {
                        Demise.INSTANCE.getConfigManager().setCurrentConfig(config);
                        ChatUtils.sendMessageClient("Loaded config: " + config);
                    } else {
                        ChatUtils.sendMessageClient("Invalid config: " + config);
                    }
                    break;
                }

                if (button.id == config.hashCode() + 1) {
                    ModuleConfig cfg = new ModuleConfig(config);
                    boolean success = Demise.INSTANCE.getConfigManager().saveConfig(cfg);
                    ChatUtils.sendMessageClient(success ? "Saved config: " + config : "Failed to save config: " + config);
                    break;
                }

                if (button.id == config.hashCode() + 2) {
                    File configFile = new File(Demise.INSTANCE.getMainDir(), config + ".json");
                    if (!configFile.exists()) {
                        ChatUtils.sendMessageClient("Config does not exist: " + config);
                        return;
                    }

                    String message = configFile.delete() ? "Removed config: " + config : "Failed to remove config: " + config;
                    ChatUtils.sendMessageClient(message);
                    initGui();
                    break;
                }
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}