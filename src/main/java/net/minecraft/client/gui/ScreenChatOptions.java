package net.minecraft.client.gui;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

public class ScreenChatOptions extends GuiScreen {
    private static final GameSettings.Options[] field_146399_a = new GameSettings.Options[]{GameSettings.Options.CHAT_VISIBILITY, GameSettings.Options.CHAT_COLOR, GameSettings.Options.CHAT_LINKS, GameSettings.Options.CHAT_OPACITY, GameSettings.Options.CHAT_LINKS_PROMPT, GameSettings.Options.CHAT_SCALE, GameSettings.Options.CHAT_HEIGHT_FOCUSED, GameSettings.Options.CHAT_HEIGHT_UNFOCUSED, GameSettings.Options.CHAT_WIDTH, GameSettings.Options.REDUCED_DEBUG_INFO};
    private final GuiScreen parentScreen;
    private final GameSettings game_settings;
    private String field_146401_i;

    public ScreenChatOptions(GuiScreen parentScreenIn, GameSettings gameSettingsIn) {
        this.parentScreen = parentScreenIn;
        this.game_settings = gameSettingsIn;
    }

    public void initGui() {
        int i = 0;
        this.field_146401_i = I18n.format("options.chat.title");

        for (GameSettings.Options gamesettings$options : field_146399_a) {
            if (gamesettings$options.getEnumFloat()) {
                this.buttonList.add(new GuiOptionSlider(gamesettings$options.returnEnumOrdinal(), width / 2 - 155 + i % 2 * 160, height / 6 + 24 * (i >> 1), gamesettings$options));
            } else {
                this.buttonList.add(new GuiOptionButton(gamesettings$options.returnEnumOrdinal(), width / 2 - 155 + i % 2 * 160, height / 6 + 24 * (i >> 1), gamesettings$options, this.game_settings.getKeyBinding(gamesettings$options)));
            }

            ++i;
        }

        this.buttonList.add(new GuiButton(200, (float) width / 2 - 100, (float) height / 6 + 120, I18n.format("gui.done")));
    }

    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id < 100 && button instanceof GuiOptionButton) {
                this.game_settings.setOptionValue(((GuiOptionButton) button).returnEnumOptions(), 1);
                button.displayString = this.game_settings.getKeyBinding(GameSettings.Options.getEnumOptions(button.id));
            }

            if (button.id == 200) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(this.parentScreen);
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, this.field_146401_i, (float) width / 2, 20, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
