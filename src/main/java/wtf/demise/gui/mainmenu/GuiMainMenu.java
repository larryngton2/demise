package wtf.demise.gui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.MainMenuOptions;
import wtf.demise.features.modules.impl.visual.Shaders;
import wtf.demise.gui.button.MenuButton;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.impl.Blur;
import wtf.demise.utils.render.shader.impl.MainMenu;
import wtf.demise.utils.render.shader.impl.Shadow;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import static wtf.demise.features.modules.impl.visual.Shaders.stencilFramebuffer;

public class GuiMainMenu extends GuiScreen {
    private final List<MenuButton> buttons = List.of(
            new MenuButton("Singleplayer"),
            new MenuButton("Multiplayer"),
            new MenuButton("Alt manager"),
            new MenuButton("Options")
    );

    private float interpolatedX;
    private float interpolatedY;
    public static boolean fade = true;
    private int alpha = 255;
    private boolean funny;
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void initGui() {
        buttons.forEach(MenuButton::initGui);
        funny = Math.random() > 0.99;
        timer.reset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MainMenu.draw(Demise.INSTANCE.getStartTimeLong());

        float buttonWidth = 120;

        float buttonHeight = switch (Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).buttonStyle.get()) {
            case "Vanilla" -> 20;
            case "Custom" -> 23;
            default -> 0;
        };

        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).blur.get()) {
            MenuButton.shader = true;
            Blur.startBlur();
            renderButtons(buttonWidth, buttonHeight, mouseX, mouseY);
            Blur.endBlur(25, 1);
        }

        if (Demise.INSTANCE.getModuleManager().getModule(Shaders.class).shadow.get()) {
            MenuButton.shader = true;
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            renderButtons(buttonWidth, buttonHeight, mouseX, mouseY);
            stencilFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 100, 1);
        }

        MenuButton.shader = false;
        renderButtons(buttonWidth, buttonHeight, mouseX, mouseY);

        float x = (width / 2f - buttonWidth / 2f) + buttonWidth / 2 - ((float) Fonts.interBold.get(35).getStringWidth(funny ? "dimaise" : Demise.INSTANCE.getClientName()) / 2);
        float y = (height / 2f) + 20 - (buttons.size() * buttonHeight) / 2f;

        if (interpolatedX == 0 || interpolatedY == 0) {
            interpolatedX = x;
            interpolatedY = y;
        }

        interpolatedX = MathUtils.interpolate(interpolatedX, x, 0.25f);
        interpolatedY = MathUtils.interpolate(interpolatedY, y, 0.25f);

        Fonts.urbanist.get(38).drawString(funny ? "dimaise" : Demise.INSTANCE.getClientName(), interpolatedX, interpolatedY, new Color(255, 255, 255, 208).getRGB());

        if (fade) {
            RenderUtils.drawRect(0, 0, mc.displayWidth, mc.displayHeight, new Color(0, 0, 0, alpha).getRGB());

            alpha -= 2;

            if (alpha < 0) {
                fade = false;
            }
        }

        mc.fontRendererObj.drawStringWithShadow(Minecraft.getDebugFPS() + "fps", 2, 2, -1);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderButtons(float buttonWidth, float buttonHeight, int mouseX, int mouseY) {
        int count = 20;

        for (MenuButton button : buttons) {
            button.x = width / 2f - buttonWidth / 2f;
            button.y = ((height / 2f) + count - (buttons.size() * buttonHeight) / 2f) + Fonts.interBold.get(36).getHeight() + 2;
            button.width = buttonWidth;
            button.height = buttonHeight;
            button.radius = 7;
            button.clickAction = () -> {
                switch (button.text) {
                    case "Singleplayer" -> mc.displayGuiScreen(new GuiSelectWorld(this));
                    case "Multiplayer" -> mc.displayGuiScreen(new GuiMultiplayer(this));
                    case "Alt manager" -> mc.displayGuiScreen(Demise.INSTANCE.getAltRepositoryGUI());
                    case "Options" -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                }
            };

            button.drawScreen(mouseX, mouseY);
            count += (int) (buttonHeight + 6);
        }
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