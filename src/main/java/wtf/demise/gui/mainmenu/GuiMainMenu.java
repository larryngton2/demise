package wtf.demise.gui.mainmenu;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.EnumChatFormatting;
import wtf.demise.Demise;
import wtf.demise.gui.button.MenuButton;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.render.shader.impl.MainMenu;

import java.io.IOException;
import java.util.List;

public class GuiMainMenu extends GuiScreen {
    private final List<MenuButton> buttons = List.of(
            new MenuButton("Singleplayer"),
            new MenuButton("Multiplayer"),
            new MenuButton("Alts"),
            new MenuButton("Options"),
            new MenuButton("Shutdown"));

    @Override
    public void initGui() {
        buttons.forEach(MenuButton::initGui);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MainMenu.draw(Demise.INSTANCE.getStartTimeLong());

        float buttonWidth = 100;
        float buttonHeight = 20;

        int count = 20;

        Fonts.interBold.get(35).drawCenteredStringWithShadow(Demise.INSTANCE.getClientName(), (width / 2f - buttonWidth / 2f) + buttonWidth / 2, (height / 2f) + count - (buttons.size() * buttonHeight) / 2f, -1);
        Fonts.interMedium.get(14).drawStringWithShadow("Welcome back, " + EnumChatFormatting.AQUA + Demise.INSTANCE.getDiscordRP().getName(), width - (2 + Fonts.interMedium.get(14).getStringWidth("Welcome back," + Demise.INSTANCE.getDiscordRP().getName())), height - (2 + Fonts.interMedium.get(14).getHeight()), -1);

        for (MenuButton button : buttons) {
            button.x = width / 2f - buttonWidth / 2f;
            button.y = ((height / 2f) + count - (buttons.size() * buttonHeight) / 2f) + Fonts.interBold.get(36).getHeight() + 2;
            button.width = buttonWidth;
            button.height = buttonHeight;
            button.clickAction = () -> {
                switch (button.text) {
                    case "Singleplayer" -> mc.displayGuiScreen(new GuiSelectWorld(this));
                    case "Multiplayer" -> mc.displayGuiScreen(new GuiMultiplayer(this));
                    case "Alts" -> mc.displayGuiScreen(Demise.INSTANCE.getAltRepositoryGUI());
                    case "Options" -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                    case "Shutdown" -> mc.shutdown();
                }
            };
            button.drawScreen(mouseX, mouseY);
            count += (int) (buttonHeight + 3);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        buttons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }
}