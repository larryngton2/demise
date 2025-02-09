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
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.ParticleRenderer;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.text.DecimalFormat;

import static net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen;

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
            this.height = getTHUDHeight();
            float currentTargetWidth = getTHUDWidth(target);
            this.width = currentTargetWidth;
            if (count > 9) continue;
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
            this.height = getTHUDHeight();
            float currentTargetWidth = getTHUDWidth(target);
            this.width = currentTargetWidth;
            if (count > 9) continue;
            TargetHUD targetHUD = new TargetHUD((float) (renderX + ((count % 3) * (lastTargetWidth + 4)) * setting.animationEntityPlayerMap.get(target).getOutput()), (float) (this.renderY + ((count / 3) * (this.height + 4)) * setting.animationEntityPlayerMap.get(target).getOutput()), target, setting.animationEntityPlayerMap.get(target), true, setting.targetHudMode);
            targetHUD.render();
            lastTargetWidth = currentTargetWidth;
            count++;
        }
    }

    public float getTHUDWidth(Entity entity) {
        return switch (setting.targetHudMode.get()) {
            case "Astolfo" -> Math.max(130, mc.fontRendererObj.getStringWidth(entity.getName()) + 60);
            case "MoonLight" -> Math.max(120, Fonts.interBold.get(18).getStringWidth(entity.getName()) + 50);
            case "Moon" -> 35 + Fonts.interSemiBold.get(18).getStringWidth(entity.getName()) + 33;
            default -> 0;
        };
    }

    public float getTHUDHeight() {
        return switch (setting.targetHudMode.get()) {
            case "Astolfo" -> 56;
            case "MoonLight" -> 44;
            case "Moon" -> 40.5f;
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
        if (!style.is("Exhi")) {
            GlStateManager.translate(x + width / 2F, y + height / 2F, 0);
            GlStateManager.scale(animation.getOutput(), animation.getOutput(), animation.getOutput());
            GlStateManager.translate(-(x + width / 2F), -(y + height / 2F), 0);
        }
        switch (style.get()) {
            case "Astolfo": {
                if (!shader) {
                    RoundedUtils.drawRound(x, y, width, height, 0, ColorUtils.applyOpacity(new Color(0, 0, 0), (float) (.4 * animation.getOutput())));
                    GlStateManager.pushMatrix();
                    drawEntityOnScreen((int) (x + 22), (int) (y + 51), 24, mc.thePlayer.rotationYaw, -mc.thePlayer.rotationPitch, target);
                    mc.fontRendererObj.drawStringWithShadow(target.getName(), x + 50, y + 6, -1);
                    GlStateManager.scale(1.5, 1.5, 1.5);
                    mc.fontRendererObj.drawStringWithShadow(String.format("%.1f", PlayerUtils.getActualHealth(target)) + " ❤", (x + 50) / 1.5f, (y + 22) / 1.5f, setting.color(1));
                    GlStateManager.popMatrix();
                    float healthWidth = (width - 54);
                    target.healthAnimation.animate(healthWidth * MathHelper.clamp_float(PlayerUtils.getActualHealth(target) / target.getMaxHealth(), 0, 1), 30);
                    RoundedUtils.drawRound(x + 48, y + 42, width - 54, 7, 0, ColorUtils.applyOpacity(new Color(setting.color(1)).darker().darker().darker(), (float) (1 * animation.getOutput())));
                    RoundedUtils.drawRound(x + 48, y + 42, target.healthAnimation.getOutput(), 7, 0, ColorUtils.applyOpacity(new Color(setting.color(1)), (float) (1 * animation.getOutput())));
                } else {
                    RoundedUtils.drawRound(x, y, width, height, 0, ColorUtils.applyOpacity(new Color(0, 0, 0), (float) (1f * animation.getOutput())));
                }
            }

            break;
            case "MoonLight": {
                target.healthAnimation.animate((width - 52) * MathHelper.clamp_float(PlayerUtils.getActualHealth(target) / target.getMaxHealth(), 0, 1), 30);
                float hurtTime = (target.hurtTime == 0 ? 0 :
                        target.hurtTime - mc.timer.renderPartialTicks) * 0.5f;
                if (!shader) {
                    //RoundedUtils.drawRoundOutline(x,y,width,height,6,.1f,ColorUtils.applyOpacity(Color.BLACK, (float) (.3f * animation.getOutput())),ColorUtils.applyOpacity(new Color(INSTANCE.getModuleManager().getModule(Interface.class).color(1)), (float) (1f * animation.getOutput())));
                    RoundedUtils.drawRound(x, y, width, height, 6, ColorUtils.applyOpacity(Color.BLACK, (float) (.4f * animation.getOutput())));
                    RenderUtils.renderPlayer2D(target, x + 4 + (hurtTime) / 2, y + 4 + (hurtTime) / 2, 34 - hurtTime, 8f, ColorUtils.interpolateColor2(Color.WHITE, Color.RED, hurtTime / 7));
                    Fonts.interBold.get(18).drawString(target.getName(), x + 43, y + 10, ColorUtils.applyOpacity(Color.WHITE, (float) animation.getOutput()).getRGB());
                    Fonts.interBold.get(14).drawString("HP: " + String.format("%.1f", target.healthAnimation.getOutput() / (width - 52) * target.getMaxHealth()), x + 43, y + 20, ColorUtils.applyOpacity(Color.WHITE, (float) animation.getOutput()).getRGB());
                    RoundedUtils.drawRound(x + 44, y + 30, width - 52, 6, 3, ColorUtils.applyOpacity(Color.BLACK, (float) (.47f * animation.getOutput())));
                    RoundedUtils.drawGradientHorizontal(x + 44, y + 30, target.healthAnimation.getOutput(), 6, 3, ColorUtils.applyOpacity(new Color(INSTANCE.getModuleManager().getModule(Interface.class).color(0)), (float) animation.getOutput()), ColorUtils.applyOpacity(new Color(INSTANCE.getModuleManager().getModule(Interface.class).color(10)), (float) animation.getOutput()));
                } else {
                    RoundedUtils.drawRound(x, y, width, height, 6, ColorUtils.applyOpacity(Color.BLACK, (float) (1f * animation.getOutput())));
                    //RoundedUtils.drawRound(x,y,width,height,6,new Color(INSTANCE.getModuleManager().getModule(Interface.class).color(1)));
                }
            }

            if (setting.targetHudParticle.get()) {
                ParticleRenderer.renderParticle(target, x + 4, y + 4, 34 / 2f);
            }
            break;

            case "Moon": {
                float healthPercentage = PlayerUtils.getActualHealth(target)/ target.getMaxHealth();
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
                    RoundedUtils.drawRound(x, y, width, height, 8, new Color(setting.color()));
                }

                if (setting.targetHudParticle.get()) {
                    ParticleRenderer.renderParticle(target, x + 2.5f, y + 2.5f, 35 / 2f);
                }
            }
            break;
        }
        GlStateManager.popMatrix();
    }
}