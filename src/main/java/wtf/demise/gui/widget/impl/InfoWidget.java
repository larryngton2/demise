package wtf.demise.gui.widget.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.ArrayList;

public class InfoWidget extends Widget {
    private final ArrayList<Long> leftClicks = new ArrayList<>();
    private final ArrayList<Long> rightClicks = new ArrayList<>();

    public InfoWidget() {
        super("Info");
        this.x = 0.8848025f;
        this.y = 0.962963f;
        Demise.INSTANCE.getEventManager().unregister(this);
        Demise.INSTANCE.getEventManager().register(this);
    }

    @Override
    public void render() {
        draw(false, false);
    }

    @Override
    public void onShader(ShaderEvent event) {
        draw(true, event.getShaderType() == ShaderEvent.ShaderType.GLOW);
    }

    private void draw(boolean shader, boolean isGlow) {
        float x;

        long time = System.currentTimeMillis();
        leftClicks.removeIf(aLong -> aLong + 1000L < time);
        rightClicks.removeIf(aLong -> aLong + 1000L < time);

        String str1 = Minecraft.getDebugFPS() + " FPS";
        String cpsText = leftClicks.size() + " | " + rightClicks.size() + " CPS";
        float width1 = Fonts.interRegular.get(15).getStringWidth(str1) + 6;
        float width2 = Fonts.interRegular.get(15).getStringWidth(mc.thePlayer.getName()) + 16;
        float width3 = Fonts.interRegular.get(15).getStringWidth(cpsText) + 6;

        if (renderX < sr.getScaledWidth() / 2f) {
            x = renderX;
        } else {
            x = renderX + width - (width1 + width2 + width3 + 10);
        }

        float height = Fonts.interRegular.get(15).getHeight() + 5;
        float textY = (float) (renderY + Fonts.interRegular.get(15).getHeight() + 0.5 - Fonts.interRegular.get(15).getHeight() / 2f);
        float x1 = x + width3 + 5;
        float x2 = x1 + width1 + 5;

        this.height = height;

        if (!shader) {
            RoundedUtils.drawRound(x, renderY, width3, height, 3, new Color(setting.bgColor(), true));
            Fonts.interRegular.get(15).drawGradient(cpsText, x + 3, textY, (index) -> new Color(setting.color(index)));

            RoundedUtils.drawRound(x1, renderY, width1, height, 3, new Color(setting.bgColor(), true));
            Fonts.interRegular.get(15).drawGradient(str1, x1 + 3, textY, (index) -> new Color(setting.color(index)));

            RoundedUtils.drawRound(x2, renderY, width2, height, 3, new Color(setting.bgColor(), true));
            RenderUtils.renderPlayerHead(mc.thePlayer, x2 + 2, (renderY + height / 2) - 5, 10, 10);
            Fonts.interRegular.get(15).drawGradient(mc.thePlayer.getName(), x2 + 14, textY, (index) -> new Color(setting.color(index)));
        } else {
            if (!isGlow) {
                RoundedUtils.drawShaderRound(x, renderY, width3, height, 3, Color.black);
                RoundedUtils.drawShaderRound(x1, renderY, width1, height, 3, Color.black);
                RoundedUtils.drawShaderRound(x2, renderY, width2, height, 3, Color.black);
            } else {
                RoundedUtils.drawGradientPreset(x, renderY, width3, height, 3);
                RoundedUtils.drawGradientPreset(x1, renderY, width1, height, 3);
                RoundedUtils.drawGradientPreset(x2, renderY, width2, height, 3);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.OUTGOING) return;

        if (e.getPacket() instanceof C0APacketAnimation) {
            leftClicks.add(System.currentTimeMillis());
        }

        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            rightClicks.add(System.currentTimeMillis());
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Info");
    }
}