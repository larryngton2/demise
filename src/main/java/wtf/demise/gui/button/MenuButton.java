package wtf.demise.gui.button;

import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;
import wtf.demise.utils.render.shader.impl.Blur;
import wtf.demise.utils.render.shader.impl.Shadow;

import java.awt.*;

import static wtf.demise.features.modules.impl.visual.Shaders.stencilFramebuffer;

public class MenuButton implements Button {
    public final String text;
    public float x, y, width, height;
    public Runnable clickAction;
    private float interpolatedX;
    private float interpolatedY;
    private float interpolatedWidth;
    private float interpolatedHeight;

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
        boolean hovered = MouseUtils.isHovered(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, mouseX, mouseY);

        float x = this.x;
        float y = this.y;
        float width = this.width;
        float height = this.height;

        if (interpolatedX == 0 || interpolatedY == 0 || interpolatedWidth == 0 || interpolatedHeight == 0) {
            interpolatedX = x;
            interpolatedY = y;
            interpolatedWidth = width;
            interpolatedHeight = height;
        }

        if (hovered) {
            interpolatedX = MathUtils.interpolate(interpolatedX, x + 1.5f, 0.25f);
            interpolatedY = MathUtils.interpolate(interpolatedY, y + 1.5f, 0.25f);
            interpolatedWidth = MathUtils.interpolate(interpolatedWidth, width - 3, 0.25f);
            interpolatedHeight = MathUtils.interpolate(interpolatedHeight, height - 3, 0.25f);
        } else {
            interpolatedX = MathUtils.interpolate(interpolatedX, x, 0.25f);
            interpolatedY = MathUtils.interpolate(interpolatedY, y, 0.25f);
            interpolatedWidth = MathUtils.interpolate(interpolatedWidth, width, 0.25f);
            interpolatedHeight = MathUtils.interpolate(interpolatedHeight, height, 0.25f);
        }

        Blur.startBlur();
        RoundedUtils.drawShaderRound(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, 7, Color.black);
        Blur.endBlur(25, 1);

        stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
        stencilFramebuffer.framebufferClear();
        stencilFramebuffer.bindFramebuffer(true);
        RoundedUtils.drawShaderRound(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, 7, Color.black);
        stencilFramebuffer.unbindFramebuffer();
        Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 50, 1);

        RoundedUtils.drawRound(interpolatedX, interpolatedY, interpolatedWidth, interpolatedHeight, 7, new Color(0, 0, 0, 0));

        Fonts.interRegular.get(15).drawCenteredString(text, interpolatedX + interpolatedWidth / 2f, interpolatedY + Fonts.interRegular.get(15).getMiddleOfBox(interpolatedHeight) + 2, -1);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean hovered = MouseUtils.isHovered(x, y, width, height, mouseX, mouseY);
        if (hovered) clickAction.run();
    }
}