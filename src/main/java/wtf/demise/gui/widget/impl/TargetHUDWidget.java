package wtf.demise.gui.widget.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjglx.util.vector.Vector2f;
import wtf.demise.Demise;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.modules.impl.visual.TargetHud;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class TargetHUDWidget extends Widget {
    private final TargetHud targetHud;

    public TargetHUDWidget() {
        super("Target HUD");
        this.x = 0.44270813f;
        this.y = 0.6333332f;
        this.targetHud = Demise.INSTANCE.getModuleManager().getModule(TargetHud.class);
    }

    private float interpolatedX;
    private float interpolatedY;

    @Override
    public void render() {
        switch (targetHud.mode.get()) {
            case "New":
                this.height = 37;
                this.width = 120;
                break;
            case "Old":
                if (targetHud.target != null) {
                    String TargetName = targetHud.target.getDisplayName().getFormattedText();

                    double initialDiff = BigDecimal.valueOf((mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) - (PlayerUtils.getActualHealth(targetHud.target) + targetHud.target.getAbsorptionAmount())).setScale(2, RoundingMode.FLOOR).doubleValue();

                    if (mc.thePlayer != null) {
                        String status = initialDiff >= 0 ? " §aW" : " §cL";
                        TargetName = TargetName + status;
                    }

                    float current$minX;
                    float current$maxX;
                    float current$minY;
                    float current$maxY;

                    final ScaledResolution scaledResolution = new ScaledResolution(mc);
                    final int padding = 8;
                    final int n3 = mc.fontRendererObj.getStringWidth(TargetName) + padding + 20;
                    final float n4 = scaledResolution.getScaledWidth() / 2f - n3 / 2f + x;
                    final float n5 = scaledResolution.getScaledHeight() / 2f + 15 + y;
                    current$minX = n4 - padding;
                    current$minY = n5 - padding;
                    current$maxX = n4 + n3;
                    current$maxY = n5 + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + padding;

                    this.width = current$maxX - current$minX;
                    this.height = current$maxY - current$minY + 8;
                }
                break;
        }

        if (targetHud.target != null) {
            renderTargetHUD(false, (EntityPlayer) targetHud.target, false);
        }
    }

    @Override
    public void onShader(ShaderEvent e) {
        if (targetHud.target != null && targetHud.mode.is("New")) {
            renderTargetHUD(true, (EntityPlayer) targetHud.target, e.getShaderType() == ShaderEvent.ShaderType.GLOW);
        }
    }

    private void renderTargetHUD(boolean shader, EntityPlayer target, boolean isGlow) {
        if (target != null) {
            TargetHUD targetHUD;
            if (!targetHud.trackTarget.get()) {
                targetHUD = new TargetHUD(renderX, renderY, target, shader, isGlow, targetHud.mode.get());
            } else {
                float[] pos = new float[]{getPos(target, shader).x, getPos(target, shader).y};
                targetHUD = new TargetHUD(pos[0], pos[1], target, shader, isGlow, targetHud.mode.get());
            }
            targetHUD.render();
        }
    }

    private Vector2f getPos(Entity target, boolean shader) {
        float x = (float) MathUtils.interpolate(target.prevPosX, target.posX);
        float y = (float) MathUtils.interpolate(target.prevPosY, target.posY) + target.height - targetHud.offsetY.get() / 100;
        float z = (float) MathUtils.interpolate(target.prevPosZ, target.posZ);

        Vector2f pos = RenderUtils.worldToScreen(x, y, z, sr, false);

        if (pos == null) {
            interpolatedX = MathUtils.interpolate(interpolatedX, renderX, 0.05f);
            interpolatedY = MathUtils.interpolate(interpolatedY, renderY, 0.05f);

            return new Vector2f(interpolatedX, interpolatedY);
        }

        Vector2f rPos = new Vector2f(pos.x, pos.y);
        float rPosX = rPos.x + (targetHud.centerX.get() ? target.width - width / 2 : targetHud.offsetX.get());
        float rPosY = rPos.y;

        if (!shader) {
            if (interpolatedX == 0 && interpolatedY == 0) {
                interpolatedX = rPosX;
                interpolatedY = rPosY;
            } else {
                interpolatedX = MathUtils.interpolate(interpolatedX, rPosX, targetHud.interpolation.get());
                interpolatedY = MathUtils.interpolate(interpolatedY, rPosY, targetHud.interpolation.get());
            }
        }

        return new Vector2f(interpolatedX, interpolatedY);
    }

    @Override
    public boolean shouldRender() {
        return targetHud.isEnabled();
    }
}

@Getter
@Setter
class TargetHUD implements InstanceAccess {
    private float x, y, width, height;
    private EntityPlayer target;
    private boolean shader;
    private Interface setting = INSTANCE.getModuleManager().getModule(Interface.class);
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0");
    private boolean isGlow;
    private String mode;

    public TargetHUD(float x, float y, EntityPlayer target, boolean shader, boolean isGlow, String mode) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.shader = shader;
        this.isGlow = isGlow;
        this.mode = mode;
    }

    public void render() {
        setWidth(INSTANCE.getWidgetManager().get(TargetHUDWidget.class).width);
        setHeight(INSTANCE.getWidgetManager().get(TargetHUDWidget.class).height);
        GlStateManager.pushMatrix();

        if (!shader) {
            TargetHud.interpolatedScale = MathUtils.interpolate(TargetHud.interpolatedScale, TargetHud.targetScale, 0.25f);
        }

        GlStateManager.translate(x + width / 2F, y + height / 2F, 0);
        GlStateManager.scale(TargetHud.interpolatedScale, TargetHud.interpolatedScale, TargetHud.interpolatedScale);
        GlStateManager.translate(-(x + width / 2F), -(y + height / 2F), 0);

        switch (mode) {
            case "New": {
                float healthPercentage = PlayerUtils.getActualHealth(target) / target.getMaxHealth();
                float space = (width - 42.5f) / 100;

                target.healthAnimation.animate((100 * space) * MathHelper.clamp_float(healthPercentage, 0, 1), 50);

                if (!shader) {
                    RoundedUtils.drawRound(x, y, width, height, 7, new Color(setting.bgColor(), true));

                    RoundedUtils.drawRound(x + 38f, y + 28, (100 * space), 4, 2, new Color(0, 0, 0, 150));
                    RoundedUtils.drawGradientHorizontal(x + 38f, y + 28, target.healthAnimation.getOutput(), 4, 2, new Color(setting.color()), new Color(setting.color((int) target.healthAnimation.getOutput())));
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

                    Fonts.interSemiBold.get(13).drawString(text + "HP", x + 37, y + 17, Color.lightGray.getRGB());
                    Fonts.interSemiBold.get(13).drawString(diff, x + 115 - Fonts.interSemiBold.get(13).getStringWidth(diff), y + 17, Color.lightGray.getRGB());
                    Fonts.interSemiBold.get(18).drawString(target.getName(), x + 37, y + 6, -1);

                    RenderUtils.renderPlayerHead(target, x + 2.5f, y + 2.5f, 32, 10);
                } else {
                    if (!isGlow) {
                        RoundedUtils.drawShaderRound(x, y, width, height, 7, new Color(setting.bgColor()));
                    } else {
                        RoundedUtils.drawGradientPreset(x, y, width, height, 7);
                    }
                }
            }
            break;
            case "Old": {
                // actual shitcode from old demise
                String TargetName = target.getDisplayName().getFormattedText();
                float health = MathHelper.clamp_float(target.getHealth() / target.getMaxHealth(), 0, 1);
                String TargetHealth = String.format("%.1f", target.getHealth()) + " §c❤ ";

                String status = (health <= (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / mc.thePlayer.getMaxHealth()) ? " §aW" : " §cL";
                TargetName = TargetName + status;

                float current$minX, current$minY, current$maxX, current$maxY;

                int padding = 8;
                float n3 = mc.fontRendererObj.getStringWidth(TargetName) + padding + 20;
                float n4 = x;
                float n5 = y;
                current$minX = n4;
                current$minY = n5;
                current$maxX = n4 + n3 + padding;
                current$maxY = n5 + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + padding * 2;

                RenderUtils.drawCustomRect(current$minX, current$minY, current$maxX, current$maxY + 7, (Color.black.getRGB() & 0xFFFFFF) | 60 << 24);

                float n13 = current$minX + 6 + 27;
                float n14 = current$maxX - 2;
                float n15 = current$maxY + 0.45f;
                float healthBar = (float) (n14 + (n13 - n14) * (1.0 - ((health < 0.01) ? 0 : health)));
                if (healthBar - n13 < 1) {
                    healthBar = n13;
                }

                RenderUtils.drawCustomRect(n13 - 1, n15, healthBar - 1, n15 + 4, setting.color());

                mc.fontRendererObj.drawString(TargetName, (n4 + 24) + padding, n5 - 4 + padding, (new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF) | 255 << 24, true);
                mc.fontRendererObj.drawString(TargetHealth, (n4 + 24) + padding, n5 + 6 + padding, (new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF) | 255 << 24, true);

                float targetX = current$minX + 2.65f;
                float targetY = current$minY + 2.65f;
                GlStateManager.color(1, 1, 1, 1);
                RenderUtils.renderPlayerHead(target, targetX, targetY, 26.5f, 0);
            }
            break;
        }

        GlStateManager.popMatrix();
    }
}