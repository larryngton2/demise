package wtf.demise.gui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.util.ResourceLocation;
import wtf.demise.Demise;
import wtf.demise.userinfo.CurrentUser;
import wtf.demise.gui.button.MenuButton;
import wtf.demise.gui.font.Fonts;
import wtf.demise.userinfo.HWID;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;
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
            new MenuButton("Alt Manager"),
            new MenuButton("Options")
    );

    private float interpolatedX;
    private float interpolatedY;
    public static boolean fade = true;
    private int alpha = 255;
    private boolean funny;
    private ScaledResolution sr;
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void initGui() {
        buttons.forEach(MenuButton::initGui);

        sr = new ScaledResolution(mc);
        funny = Math.random() > 0.99;
        timer.reset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        MainMenu.draw(Demise.INSTANCE.getStartTimeLong());

        float buttonWidth = 120;
        float buttonHeight = 23;

        int count = 20;

        if (!MainMenu.drawShader) {
            RenderUtils.drawImage(new ResourceLocation("demise/texture/background.png"), 0, 0, sr.getScaledWidth(), sr.getScaledHeight());
        }

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
            if (CurrentUser.USER != null) {
                RenderUtils.drawRect(0, 0, mc.displayWidth, mc.displayHeight, new Color(0, 0, 0, alpha).getRGB());
            }

            alpha -= 2;

            if (alpha < 0) {
                fade = false;
            }
        }

        mc.fontRendererObj.drawStringWithShadow("Alpha build", 2, 2, -1);
        mc.fontRendererObj.drawStringWithShadow(HWID.getHWID(), 2, 3 + mc.fontRendererObj.FONT_HEIGHT, -1);
        mc.fontRendererObj.drawStringWithShadow(Minecraft.getDebugFPS() + "fps", 2, 4 + (mc.fontRendererObj.FONT_HEIGHT * 2), -1);

        if (CurrentUser.USER == null) {
            String string = "Invalid account detected.";
            String string1 = "Shutting down in 5s.";

            float width = 150;
            float height = Fonts.interBold.get(20).getHeight() + 23 + Fonts.interMedium.get(18).getHeight();
            float xx = ((float) sr.getScaledWidth() / 2) - (width / 2);
            float yy = ((float) sr.getScaledHeight() / 2) - (height / 2);

            RenderUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, 0).getRGB());

            Blur.startBlur();
            RoundedUtils.drawShaderRound(xx, yy, width, height, 7, Color.black);
            RenderUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), Color.black.getRGB());
            Blur.endBlur(25, 1);

            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            RoundedUtils.drawShaderRound(xx, yy, width, height, 7, Color.black);
            stencilFramebuffer.unbindFramebuffer();
            Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 50, 1);

            RoundedUtils.drawRound(xx, yy, width, height, 7, new Color(0, 0, 0, 100));

            Fonts.interBold.get(20).drawCenteredStringWithShadow(string, xx + (width / 2), yy + 12, new Color(117, 47, 47).getRGB());
            Fonts.interMedium.get(15).drawCenteredStringWithShadow(string1, xx + (width / 2), yy + Fonts.interBold.get(20).getHeight() + 15, -1);

            if (timer.hasTimeElapsed(6000)) {
                mc.shutdown();
            }
        } else {
            Fonts.interMedium.get(14).drawStringWithShadow("Welcome, " + CurrentUser.USER, width - 5 - (Fonts.interMedium.get(14).getStringWidth("Welcome, " + CurrentUser.USER)), height - (2 + Fonts.interMedium.get(14).getHeight()), -1);
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