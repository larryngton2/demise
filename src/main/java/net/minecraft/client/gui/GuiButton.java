package net.minecraft.client.gui;

import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.MainMenuOptions;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class GuiButton extends Gui {
    protected static final ResourceLocation buttonTextures = new ResourceLocation("textures/gui/widgets.png");
    @Setter
    protected float width;
    @Setter
    protected float height;
    public float xPosition;
    public float yPosition;
    public String displayString;
    public int id;
    public boolean enabled;
    public boolean visible;
    protected boolean hovered;
    private float interpolatedX;
    private float interpolatedY;
    private float interpolatedWidth;
    private float interpolatedHeight;
    public static boolean shader;

    public GuiButton(int buttonId, float x, float y, String buttonText) {
        this(buttonId, x, y, 200, 20, buttonText);
    }

    public GuiButton(int buttonId, float x, float y, float widthIn, float heightIn, String buttonText) {
        this.width = 200;
        this.height = 20;
        this.enabled = true;
        this.visible = true;
        this.id = buttonId;
        this.xPosition = x;
        this.yPosition = y;
        this.width = widthIn;
        this.height = heightIn;
        this.displayString = buttonText;
    }

    protected int getHoverState(boolean mouseOver) {
        int i = 1;

        if (!this.enabled) {
            i = 0;
        } else if (mouseOver) {
            i = 2;
        }

        return i;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            switch (Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).buttonStyle.get()) {
                case "Vanilla": {
                    FontRenderer fontrenderer = mc.fontRendererObj;
                    mc.getTextureManager().bindTexture(buttonTextures);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
                    int i = this.getHoverState(this.hovered);
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.blendFunc(770, 771);
                    drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + i * 20, this.width / 2, this.height);
                    drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
                    this.mouseDragged(mc, mouseX, mouseY);
                    int j = 14737632;

                    if (!this.enabled) {
                        j = 10526880;
                    } else if (this.hovered) {
                        j = 16777120;
                    }

                    drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, j);
                }
                break;
                case "Custom": {
                    boolean hovered = MouseUtils.isHovered(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, mouseX, mouseY);

                    if (interpolatedX == 0 || interpolatedY == 0 || interpolatedWidth == 0 || interpolatedHeight == 0) {
                        interpolatedX = xPosition;
                        interpolatedY = yPosition;
                        interpolatedWidth = width;
                        interpolatedHeight = height;
                    }

                    float interpolation = 0.1f;

                    if (hovered) {
                        interpolatedX = MathUtils.interpolate(interpolatedX, xPosition + 1.5f, interpolation);
                        interpolatedY = MathUtils.interpolate(interpolatedY, yPosition + 1.5f, interpolation);
                        interpolatedWidth = MathUtils.interpolate(interpolatedWidth, width - 3, interpolation);
                        interpolatedHeight = MathUtils.interpolate(interpolatedHeight, height - 3, interpolation);
                    } else {
                        interpolatedX = MathUtils.interpolate(interpolatedX, xPosition, interpolation);
                        interpolatedY = MathUtils.interpolate(interpolatedY, yPosition, interpolation);
                        interpolatedWidth = MathUtils.interpolate(interpolatedWidth, width, interpolation);
                        interpolatedHeight = MathUtils.interpolate(interpolatedHeight, height, interpolation);
                    }

                    if (shader) {
                        RoundedUtils.drawShaderRound(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, 7, Color.black);
                    } else {
                        RoundedUtils.drawRound(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, 7, new Color(0, 0, 0, 75));
                        Fonts.interRegular.get(15).drawCenteredString(displayString, interpolatedX + interpolatedWidth / 2f, interpolatedY + Fonts.interRegular.get(15).getMiddleOfBox(interpolatedHeight) + 2, enabled ? 14737632 : 10526880);
                    }

                    this.mouseDragged(mc, mouseX, mouseY);
                }
            }
        }
    }

    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
    }

    public void mouseReleased(int mouseX, int mouseY) {
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return this.enabled && this.visible && mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
    }

    public boolean isMouseOver() {
        return this.hovered;
    }

    public void drawButtonForegroundLayer(int mouseX, int mouseY) {
    }

    public void playPressSound(SoundHandler soundHandlerIn) {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    public float getButtonWidth() {
        return this.width;
    }
}