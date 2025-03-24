package wtf.demise.gui.widget.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.ParticleRenderer;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class TargetHUDWidget extends Widget {
    public TargetHUDWidget() {
        super("Target HUD");
        this.x = 0.5f;
        this.y = 0.8f;
    }

    @Override
    public void render() {
        int count = 0;
        float lastTargetWidth = 0;
        for (EntityPlayer target : setting.animationEntityPlayerMap.keySet()) {
            if (count > 1) continue;
            this.height = getTHUDHeight();
            float currentTargetWidth = getTHUDWidth(target);
            this.width = currentTargetWidth;
            TargetHUD targetHUD = new TargetHUD((float) (renderX + ((count % 3) * (lastTargetWidth + 4)) * setting.animationEntityPlayerMap.get(target).getOutput()), (float) (this.renderY + ((count / 3) * (this.height + 4)) * setting.animationEntityPlayerMap.get(target).getOutput()), target, setting.animationEntityPlayerMap.get(target), false, setting.targetHudMode);
            targetHUD.render();
            lastTargetWidth = currentTargetWidth;
            count++;
        }
    }

    @Override
    public void onShader(Shader2DEvent event) {
        int count = 0;
        float lastTargetWidth = 0;
        for (EntityPlayer target : setting.animationEntityPlayerMap.keySet()) {
            if (count > 1) continue;
            this.height = getTHUDHeight();
            float currentTargetWidth = getTHUDWidth(target);
            this.width = currentTargetWidth;
            TargetHUD targetHUD = new TargetHUD((float) (renderX + ((count % 3) * (lastTargetWidth + 4)) * setting.animationEntityPlayerMap.get(target).getOutput()), (float) (this.renderY + ((count / 3) * (this.height + 4)) * setting.animationEntityPlayerMap.get(target).getOutput()), target, setting.animationEntityPlayerMap.get(target), true, setting.targetHudMode);
            targetHUD.render();
            lastTargetWidth = currentTargetWidth;
            count++;
        }
    }

    public float getTHUDWidth(Entity entity) {
        return switch (setting.targetHudMode.get()) {
            case "Moon" -> 35 + Fonts.interSemiBold.get(18).getStringWidth(entity.getName()) + 33;
            case "Demise" -> 120;
            default -> 0;
        };
    }

    public float getTHUDHeight() {
        return switch (setting.targetHudMode.get()) {
            case "Moon" -> 40.5f;
            case "Demise" -> 37;
            default -> 0;
        };
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Target HUD");
    }
}

@Getter
@Setter
class TargetHUD implements InstanceAccess {
    private float x, y, width, height;
    private EntityPlayer target;
    private Animation animation;
    private boolean shader;
    private ModeValue style;
    private Interface setting = INSTANCE.getModuleManager().getModule(Interface.class);
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0");

    public TargetHUD(float x, float y, EntityPlayer target, Animation animation, boolean shader, ModeValue style) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.animation = animation;
        this.shader = shader;
        this.style = style;
    }

    public void render() {
        setWidth(INSTANCE.getWidgetManager().get(TargetHUDWidget.class).getTHUDWidth(target));
        setHeight(INSTANCE.getWidgetManager().get(TargetHUDWidget.class).getTHUDHeight());
        GlStateManager.pushMatrix();

        GlStateManager.translate(x + width / 2F, y + height / 2F, 0);
        GlStateManager.scale(animation.getOutput(), animation.getOutput(), animation.getOutput());
        GlStateManager.translate(-(x + width / 2F), -(y + height / 2F), 0);

        switch (style.get()) {
            case "Moon": {
                float healthPercentage = PlayerUtils.getActualHealth(target) / target.getMaxHealth();
                float space = (width - 48) / 100;

                target.healthAnimation.animate((100 * space) * MathHelper.clamp_float(healthPercentage, 0, 1), 30);

                if (!shader) {
                    RoundedUtils.drawRound(x, y, width, height, 8, new Color(setting.bgColor(), true));

                    RoundedUtils.drawRound(x + 42, y + 26.5f, (100 * space), 8, 4, new Color(0, 0, 0, 150));
                    String text = String.format("%.1f", PlayerUtils.getActualHealth(target));

                    RoundedUtils.drawRound(x + 42, y + 26.5f, target.healthAnimation.getOutput(), 8.5f, 4, new Color(setting.color(0)));
                    RenderUtils.renderPlayer2D(target, x + 2.5f, y + 2.5f, 35, 10, -1);
                    Fonts.interSemiBold.get(13).drawStringWithShadow(text + "HP", x + 40, y + 17, -1);
                    Fonts.interSemiBold.get(18).drawStringWithShadow(target.getName(), x + 40, y + 6, -1);
                } else {
                    RoundedUtils.drawRound(x, y, width, height, 8, new Color(setting.bgColor()));
                }

                if (setting.targetHudParticle.get()) {
                    ParticleRenderer.renderParticle(target, x + 2.5f, y + 2.5f);
                }
            }
            break;

            case "Demise": {
                float healthPercentage = PlayerUtils.getActualHealth(target) / target.getMaxHealth();
                float space = (width - 42.5f) / 100;

                target.healthAnimation.animate((100 * space) * MathHelper.clamp_float(healthPercentage, 0, 1), 30);

                if (!shader) {
                    RoundedUtils.drawRound(x, y, width, height, 7, new Color(setting.bgColor(), true));

                    RoundedUtils.drawRound(x + 38f, y + 28, (100 * space), 4, 2, new Color(0, 0, 0, 150));
                    String text = String.valueOf(BigDecimal.valueOf(PlayerUtils.getActualHealth(target)).setScale(2, RoundingMode.FLOOR).doubleValue());
                    double initialDiff = BigDecimal.valueOf((mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) - (PlayerUtils.getActualHealth(target) + target.getAbsorptionAmount())).setScale(2, RoundingMode.FLOOR).doubleValue();
                    String diff;

                    if (initialDiff > 0) {
                        diff = "+" + initialDiff;
                    } else if (initialDiff < 0) {
                        diff = String.valueOf(initialDiff);
                    } else {
                        diff = "±" + initialDiff;
                    }

                    RoundedUtils.drawRound(x + 38f, y + 28, target.healthAnimation.getOutput(), 4, 2, new Color(setting.color(0)));
                    RenderUtils.renderPlayer2D(target, x + 2.5f, y + 2.5f, 32, 10, -1);
                    Fonts.interSemiBold.get(13).drawStringWithShadow(text + "HP", x + 37, y + 17, Color.lightGray.getRGB());
                    Fonts.interSemiBold.get(13).drawStringWithShadow(diff, x + 115 - Fonts.interSemiBold.get(13).getStringWidth(diff), y + 17, Color.lightGray.getRGB());
                    Fonts.interSemiBold.get(18).drawStringWithShadow(target.getName(), x + 37, y + 6, -1);
                } else {
                    RoundedUtils.drawShaderRound(x, y, width, height, 7, new Color(setting.bgColor()));
                }

                if (setting.targetHudParticle.get()) {
                    ParticleRenderer.renderParticle(target, x + 2.5f, y + 2.5f);
                }
            }
            break;
        }
        GlStateManager.popMatrix();
    }
}
