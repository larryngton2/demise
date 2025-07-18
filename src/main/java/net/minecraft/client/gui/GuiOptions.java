package net.minecraft.client.gui;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.EnumDifficulty;

public class GuiOptions extends GuiScreen implements GuiYesNoCallback {
    private static final GameSettings.Options[] field_146440_f = new GameSettings.Options[]{GameSettings.Options.FOV};
    private final GuiScreen field_146441_g;
    private final GameSettings game_settings_1;
    private GuiButton field_175357_i;
    private GuiLockIconButton field_175356_r;
    protected String field_146442_a = "Options";

    public GuiOptions(GuiScreen p_i1046_1_, GameSettings p_i1046_2_) {
        this.field_146441_g = p_i1046_1_;
        this.game_settings_1 = p_i1046_2_;
    }

    public void initGui() {
        int i = 0;
        this.field_146442_a = I18n.format("options.title");

        for (GameSettings.Options gamesettings$options : field_146440_f) {
            if (gamesettings$options.getEnumFloat()) {
                this.buttonList.add(new GuiOptionSlider(gamesettings$options.returnEnumOrdinal(), width / 2 - 155 + i % 2 * 160, height / 6 - 12 + 24 * (i >> 1), gamesettings$options));
            } else {
                GuiOptionButton guioptionbutton = new GuiOptionButton(gamesettings$options.returnEnumOrdinal(), width / 2 - 155 + i % 2 * 160, height / 6 - 12 + 24 * (i >> 1), gamesettings$options, this.game_settings_1.getKeyBinding(gamesettings$options));
                this.buttonList.add(guioptionbutton);
            }

            ++i;
        }

        if (mc.theWorld != null) {
            EnumDifficulty enumdifficulty = mc.theWorld.getDifficulty();
            this.field_175357_i = new GuiButton(108, (float) width / 2 - 155 + i % 2 * 160, (float) height / 6 - 12 + 24 * (i >> 1), 150, 20, this.func_175355_a(enumdifficulty));
            this.buttonList.add(this.field_175357_i);

            if (mc.isSingleplayer() && !mc.theWorld.getWorldInfo().isHardcoreModeEnabled()) {
                this.field_175357_i.setWidth(this.field_175357_i.getButtonWidth() - 20);
                this.field_175356_r = new GuiLockIconButton(109, this.field_175357_i.xPosition + this.field_175357_i.getButtonWidth(), this.field_175357_i.yPosition);
                this.buttonList.add(this.field_175356_r);
                this.field_175356_r.func_175229_b(mc.theWorld.getWorldInfo().isDifficultyLocked());
                this.field_175356_r.enabled = !this.field_175356_r.func_175230_c();
                this.field_175357_i.enabled = !this.field_175356_r.func_175230_c();
            } else {
                this.field_175357_i.enabled = false;
            }
        } else {
            GuiOptionButton guioptionbutton1 = new GuiOptionButton(GameSettings.Options.REALMS_NOTIFICATIONS.returnEnumOrdinal(), width / 2 - 155 + i % 2 * 160, height / 6 - 12 + 24 * (i >> 1), GameSettings.Options.REALMS_NOTIFICATIONS, this.game_settings_1.getKeyBinding(GameSettings.Options.REALMS_NOTIFICATIONS));
            this.buttonList.add(guioptionbutton1);
        }

        this.buttonList.add(new GuiButton(110, (float) width / 2 - 155, (float) height / 6 + 48 - 6, 150, 20, I18n.format("options.skinCustomisation")));
        this.buttonList.add(new GuiButton(8675309, (float) width / 2 + 5, (float) height / 6 + 48 - 6, 150, 20, "Super Secret Settings...") {
            public void playPressSound(SoundHandler soundHandlerIn) {
                SoundEventAccessorComposite soundeventaccessorcomposite = soundHandlerIn.getRandomSoundFromCategories(SoundCategory.ANIMALS, SoundCategory.BLOCKS, SoundCategory.MOBS, SoundCategory.PLAYERS, SoundCategory.WEATHER);

                if (soundeventaccessorcomposite != null) {
                    soundHandlerIn.playSound(PositionedSoundRecord.create(soundeventaccessorcomposite.getSoundEventLocation(), 0.5F));
                }
            }
        });

        this.buttonList.add(new GuiButton(106, (float) width / 2 - 155, (float) height / 6 + 72 - 6, 150, 20, I18n.format("options.sounds")));
        this.buttonList.add(new GuiButton(107, (float) width / 2 + 5, (float) height / 6 + 72 - 6, 150, 20, I18n.format("options.stream")));
        this.buttonList.add(new GuiButton(101, (float) width / 2 - 155, (float) height / 6 + 96 - 6, 150, 20, I18n.format("options.video")));
        this.buttonList.add(new GuiButton(100, (float) width / 2 + 5, (float) height / 6 + 96 - 6, 150, 20, I18n.format("options.controls")));
        this.buttonList.add(new GuiButton(102, (float) width / 2 - 155, (float) height / 6 + 120 - 6, 150, 20, I18n.format("options.language")));
        this.buttonList.add(new GuiButton(103, (float) width / 2 + 5, (float) height / 6 + 120 - 6, 150, 20, I18n.format("options.chat.title")));
        this.buttonList.add(new GuiButton(105, (float) width / 2 - 155, (float) height / 6 + 144 - 6, 150, 20, I18n.format("options.resourcepack")));
        this.buttonList.add(new GuiButton(104, (float) width / 2 + 5, (float) height / 6 + 144 - 6, 150, 20, I18n.format("options.snooper.view")));
        this.buttonList.add(new GuiButton(200, (float) width / 2 - 100, (float) height / 6 + 168, I18n.format("gui.done")));
    }

    public String func_175355_a(EnumDifficulty p_175355_1_) {
        IChatComponent ichatcomponent = new ChatComponentText("");
        ichatcomponent.appendSibling(new ChatComponentTranslation("options.difficulty"));
        ichatcomponent.appendText(": ");
        ichatcomponent.appendSibling(new ChatComponentTranslation(p_175355_1_.getDifficultyResourceKey()));
        return ichatcomponent.getFormattedText();
    }

    public void confirmClicked(boolean result, int id) {
        mc.displayGuiScreen(this);

        if (id == 109 && result && mc.theWorld != null) {
            mc.theWorld.getWorldInfo().setDifficultyLocked(true);
            this.field_175356_r.func_175229_b(true);
            this.field_175356_r.enabled = false;
            this.field_175357_i.enabled = false;
        }
    }

    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id < 100 && button instanceof GuiOptionButton) {
                GameSettings.Options gamesettings$options = ((GuiOptionButton) button).returnEnumOptions();
                this.game_settings_1.setOptionValue(gamesettings$options, 1);
                button.displayString = this.game_settings_1.getKeyBinding(GameSettings.Options.getEnumOptions(button.id));
            }

            if (button.id == 108) {
                mc.theWorld.getWorldInfo().setDifficulty(EnumDifficulty.getDifficultyEnum(mc.theWorld.getDifficulty().getDifficultyId() + 1));
                this.field_175357_i.displayString = this.func_175355_a(mc.theWorld.getDifficulty());
            }

            if (button.id == 109) {
                mc.displayGuiScreen(new GuiYesNo(this, (new ChatComponentTranslation("difficulty.lock.title")).getFormattedText(), (new ChatComponentTranslation("difficulty.lock.question", new ChatComponentTranslation(mc.theWorld.getWorldInfo().getDifficulty().getDifficultyResourceKey()))).getFormattedText(), 109));
            }

            if (button.id == 110) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(new GuiCustomizeSkin(this));
            }

            if (button.id == 8675309) {
                mc.entityRenderer.activateNextShader();
            }

            if (button.id == 101) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(new GuiVideoSettings(this, this.game_settings_1));
            }

            if (button.id == 100) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(new GuiControls(this, this.game_settings_1));
            }

            if (button.id == 102) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(new GuiLanguage(this, this.game_settings_1, mc.getLanguageManager()));
            }

            if (button.id == 103) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(new ScreenChatOptions(this, this.game_settings_1));
            }

            if (button.id == 104) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(new GuiSnooper(this, this.game_settings_1));
            }

            if (button.id == 200) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(this.field_146441_g);
            }

            if (button.id == 105) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(new GuiScreenResourcePacks(this));
            }

            if (button.id == 106) {
                mc.gameSettings.saveOptions();
                mc.displayGuiScreen(new GuiScreenOptionsSounds(this, this.game_settings_1));
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, this.field_146442_a, (float) width / 2, 15, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
