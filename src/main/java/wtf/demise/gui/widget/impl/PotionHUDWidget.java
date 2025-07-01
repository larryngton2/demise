package wtf.demise.gui.widget.impl;

import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import wtf.demise.Demise;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.animations.ContinualAnimation;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.ArrayList;

public class PotionHUDWidget extends Widget {
    public PotionHUDWidget() {
        super("Potion HUD");
        this.x = 0.008333334f;
        this.y = 0.05185188f;
    }

    private final ContinualAnimation heightAnimation = new ContinualAnimation();

    @Override
    public void render() {
        ArrayList<PotionEffect> potions = new ArrayList<>(mc.thePlayer.getActivePotionEffects());

        width = 92;
        height = heightAnimation.getOutput();

        RoundedUtils.drawRound(renderX, renderY, width, height, 6, new Color(setting.bgColor(), true));

        Fonts.interSemiBold.get(13).drawString("Potions", renderX + 8, renderY + 7 + 2, -1);

        Fonts.nursultan.get(14).drawString("E", renderX + width - 16, renderY + 9, setting.color(0));

        float offset = renderY + 21;

        for (PotionEffect potion : potions) {
            String name = I18n.format(Potion.potionTypes[potion.getPotionID()].getName()) + " " + (potion.getAmplifier() > 0 ? I18n.format("enchantment.level." + (potion.getAmplifier() + 1)) : "");
            String duration = Potion.getDurationString(potion);

            Fonts.interRegular.get(11).drawStringWithShadow(name, renderX + 8, offset, -1);
            Fonts.interRegular.get(11).drawStringWithShadow(duration, renderX + width - 8 - Fonts.interRegular.get(11).getStringWidth(duration), offset, -1);

            offset += 10;
        }

        heightAnimation.animate(20 + potions.size() * 10, 20);
    }

    @Override
    public void onShader(Shader2DEvent event) {
        ArrayList<PotionEffect> potions = new ArrayList<>(mc.thePlayer.getActivePotionEffects());
        width = 92;
        height = heightAnimation.getOutput();

        if (event.getShaderType() != Shader2DEvent.ShaderType.GLOW) {
            RoundedUtils.drawShaderRound(renderX, renderY, width, height, 6, Color.black);
        } else {
            RoundedUtils.drawGradientPreset(renderX, renderY, width, height, 6);
        }

        heightAnimation.animate(20 + potions.size() * 10, 20);
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Potion HUD");
    }
}