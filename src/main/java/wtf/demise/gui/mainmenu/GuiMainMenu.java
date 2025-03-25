package wtf.demise.gui.mainmenu;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import wtf.demise.Demise;
import wtf.demise.gui.button.MenuButton;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.impl.MainMenu;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GuiMainMenu extends GuiScreen {
    private final List<MenuButton> buttons = List.of(
            new MenuButton("Singleplayer"),
            new MenuButton("Multiplayer"),
            new MenuButton("Alt Manager"),
            new MenuButton("Options")
    );

    private float interpolatedX;
    private float interpolatedY;
    public static boolean fade = true;
    private int alpha = 255;
    private boolean funny;

    @Override
    public void initGui() {
        buttons.forEach(MenuButton::initGui);

        funny = Math.random() > 0.99;

        if (alpha != 255) {
            fade = false;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MainMenu.draw(Demise.INSTANCE.getStartTimeLong());

        float buttonWidth = 120;
        float buttonHeight = 23;

        int count = 20;

        //Fonts.interMedium.get(14).drawStringWithShadow("Welcome back, " + EnumChatFormatting.AQUA + Demise.INSTANCE.getDiscordRP().getName(), width - (2 + Fonts.interMedium.get(14).getStringWidth("Welcome back, " + Demise.INSTANCE.getDiscordRP().getName())), height - (2 + Fonts.interMedium.get(14).getHeight()), -1);

        for (MenuButton button : buttons) {
            button.x = width / 2f - buttonWidth / 2f;
            button.y = ((height / 2f) + count - (buttons.size() * buttonHeight) / 2f) + Fonts.interBold.get(36).getHeight() + 2;
            button.width = buttonWidth;
            button.height = buttonHeight;
            button.clickAction = () -> {
                switch (button.text) {
                    case "Singleplayer" -> mc.displayGuiScreen(new GuiSelectWorld(this));
                    case "Multiplayer" -> mc.displayGuiScreen(new GuiMultiplayer(this));
                    case "Alt Manager" -> mc.displayGuiScreen(Demise.INSTANCE.getAltRepositoryGUI());
                    case "Options" -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                }
            };

            button.drawScreen(mouseX, mouseY);
            count += (int) (buttonHeight + 6);
        }

        float x = (width / 2f - buttonWidth / 2f) + buttonWidth / 2 - ((float) Fonts.interBold.get(35).getStringWidth(funny ? "dimaise" : Demise.INSTANCE.getClientName()) / 2);
        float y = (height / 2f) + 20 - (buttons.size() * buttonHeight) / 2f;

        if (interpolatedX == 0 || interpolatedY == 0) {
            interpolatedX = x;
            interpolatedY = y;
        }

        interpolatedX = MathUtils.interpolate(interpolatedX, x, 0.25f);
        interpolatedY = MathUtils.interpolate(interpolatedY, y, 0.25f);

        Fonts.interBold.get(35).drawStringWithShadow(funny ? "dimaise" : Demise.INSTANCE.getClientName(), interpolatedX, interpolatedY, Color.lightGray.getRGB());

        if (fade) {
            RenderUtils.drawRect(0, 0, mc.displayWidth, mc.displayHeight, new Color(0, 0, 0, alpha).getRGB());

            alpha -= 3;

            if (alpha < 0) {
                alpha = 255;
                fade = false;
            }
        } else {
            alpha = 255;
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