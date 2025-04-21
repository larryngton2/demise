package wtf.demise.gui.button;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.MainMenuOptions;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class MenuButton implements Button, InstanceAccess {
    protected static final ResourceLocation buttonTextures = new ResourceLocation("textures/gui/widgets.png");
    public final String text;
    public float x, y, width, height;
    public float radius;
    public Runnable clickAction;
    private float interpolatedX;
    private float interpolatedY;
    private float interpolatedWidth;
    private float interpolatedHeight;
    public static boolean shader;

    public MenuButton(String text) {
        this.text = text;
    }

    @Override
    public void initGui() {
        interpolatedX = x;
        interpolatedY = y;
        interpolatedWidth = width;
        interpolatedHeight = height;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        switch (Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).buttonStyle.get()) {
            case "Vanilla": {
                FontRenderer fontrenderer = mc.fontRendererObj;
                mc.getTextureManager().bindTexture(buttonTextures);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);

                int i = this.getHoverState(hovered);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.blendFunc(770, 771);
                Gui.drawTexturedModalRect(x, y, 0, 46 + i * 20, width / 2, height);
                Gui.drawTexturedModalRect(x + width / 2, y, 200 - width / 2, 46 + i * 20, width / 2, height);
                int j = 14737632;

                if (hovered) {
                    j = 16777120;
                }

                drawCenteredString(fontrenderer, text, x + width / 2, y + (height - 8) / 2, j);
            }
            break;
            case "Custom": {
                boolean hovered = MouseUtils.isHovered(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, mouseX, mouseY);

                if (interpolatedX == 0 || interpolatedY == 0 || interpolatedWidth == 0 || interpolatedHeight == 0) {
                    interpolatedX = x;
                    interpolatedY = y;
                    interpolatedWidth = width;
                    interpolatedHeight = height;
                }

                float interpolation = 0.1f;

                if (hovered) {
                    interpolatedX = MathUtils.interpolate(interpolatedX, x + 1.5f, interpolation);
                    interpolatedY = MathUtils.interpolate(interpolatedY, y + 1.5f, interpolation);
                    interpolatedWidth = MathUtils.interpolate(interpolatedWidth, width - 3, interpolation);
                    interpolatedHeight = MathUtils.interpolate(interpolatedHeight, height - 3, interpolation);
                } else {
                    interpolatedX = MathUtils.interpolate(interpolatedX, x, interpolation);
                    interpolatedY = MathUtils.interpolate(interpolatedY, y, interpolation);
                    interpolatedWidth = MathUtils.interpolate(interpolatedWidth, width, interpolation);
                    interpolatedHeight = MathUtils.interpolate(interpolatedHeight, height, interpolation);
                }

                if (!shader) {
                    RoundedUtils.drawRound(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, 7, new Color(0, 0, 0, 75));
                } else {
                    RoundedUtils.drawShaderRound(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, 7, Color.black);
                }

                Fonts.interRegular.get(15).drawCenteredString(text, interpolatedX + interpolatedWidth / 2f, interpolatedY + Fonts.interRegular.get(15).getMiddleOfBox(interpolatedHeight) + 2, -1);
            }
        }
    }

    protected int getHoverState(boolean mouseOver) {
        return mouseOver ? 2 : 1;
    }

    private void drawCenteredString(FontRenderer fontRendererIn, String text, float x, float y, int color) {
        fontRendererIn.drawStringWithShadow(text, x - fontRendererIn.getStringWidth(text) / 2, y, color);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        if (hovered) clickAction.run();
    }
}