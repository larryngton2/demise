package wtf.demise.gui.widget.impl;

import net.minecraft.client.Minecraft;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class InfoWidget extends Widget {
    public InfoWidget() {
        super("Info");
    }

    @Override
    public void render() {
        draw(false);
    }

    @Override
    public void onShader(Shader2DEvent event) {
        draw(true);
    }

    private void draw(boolean shader) {
        float x;

        String str1 = Minecraft.getDebugFPS() + " FPS";
        float width1 = Fonts.interRegular.get(15).getStringWidth(str1) + 6;
        float width2 = Fonts.interRegular.get(15).getStringWidth(mc.thePlayer.getName()) + 18;

        if (renderX < sr.getScaledWidth() / 2) {
            x = renderX;
        } else {
            x = renderX + width - (width1 + width2);
        }

        float height = Fonts.interRegular.get(15).getHeight() + 5;
        float textY = (float) (renderY + Fonts.interRegular.get(15).getHeight() + 0.5 - Fonts.interRegular.get(15).getHeight() / 2);
        float x1 = x + width1 + 5;

        this.height = height;

        if (!shader) {
            RoundedUtils.drawRound(x, renderY, width1, height, 3, new Color(setting.bgColor(), true));
            Fonts.interRegular.get(15).drawString(str1, x + 3, textY, -1);

            RoundedUtils.drawRound(x1, renderY, width2, height, 3, new Color(setting.bgColor(), true));
            RenderUtils.renderPlayerHead(mc.thePlayer, x1 + 2, (renderY + height / 2) - 5, 10, 10, -1);
            Fonts.interRegular.get(15).drawString(mc.thePlayer.getName(), x1 + 14, textY, -1);
        } else {
            RoundedUtils.drawShaderRound(x, renderY, width1, height, 3, Color.black);
            RoundedUtils.drawShaderRound(x1, renderY, width2, height, 3, Color.black);
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Info");
    }
}
