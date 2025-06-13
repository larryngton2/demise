package wtf.demise.gui.widget.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjglx.util.vector.Vector2f;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class TargetHUDWidget extends Widget {
    public TargetHUDWidget() {
        super("Target HUD");
        this.x = 0.44270813f;
        this.y = 0.6333332f;
    }

    private float interpolatedX;
    private float interpolatedY;

    @Override
    public void render() {
        this.height = 37;
        this.width = 120;

        if (setting.target != null) {
            TargetHUD targetHUD;
            if (!setting.targetHUDTracking.get()) {
                targetHUD = new TargetHUD(renderX, renderY, (EntityPlayer) setting.target, setting.decelerateAnimation, false);
            } else {
                targetHUD = new TargetHUD(getPos().x, getPos().y, (EntityPlayer) setting.target, setting.decelerateAnimation, false);
            }
            targetHUD.render();
        }
    }

    @Override
    public void onShader(Shader2DEvent e) {
        if (setting.target != null) {
            TargetHUD targetHUD;
            if (!setting.targetHUDTracking.get()) {
                targetHUD = new TargetHUD(renderX, renderY, (EntityPlayer) setting.target, setting.decelerateAnimation, true);
            } else {
                targetHUD = new TargetHUD(getPos().x, getPos().y, (EntityPlayer) setting.target, setting.decelerateAnimation, true);
            }
            targetHUD.render();
        }
    }

    private Vector2f getPos() {
        Entity target = setting.target;

        float x = (float) (target.prevPosX + (target.posX - target.prevPosX) * mc.timer.renderPartialTicks);
        float y = (float) (target.prevPosY + (target.posY - target.prevPosY) * mc.timer.renderPartialTicks + target.height);
        float z = (float) (target.prevPosZ + (target.posZ - target.prevPosZ) * mc.timer.renderPartialTicks);

        x -= (float) mc.getRenderManager().viewerPosX;
        y -= (float) mc.getRenderManager().viewerPosY;
        z -= (float) mc.getRenderManager().viewerPosZ;

        Vector2f pos = RenderUtils.worldToScreen(x, y, z, sr);

        if (pos == null) {
            interpolatedX = MathUtils.interpolate(interpolatedX, renderX, setting.interpolation.get());
            interpolatedY = MathUtils.interpolate(interpolatedY, renderY, setting.interpolation.get());

            return new Vector2f(interpolatedX, interpolatedY);
        }

        Vector2f rPos = new Vector2f((float) Math.floor(pos.x), (float) Math.floor(pos.y));
        float rPosX = rPos.x + (setting.centerX.get() ? target.width - width / 2 : setting.offsetX.get());
        float rPosY = (int) rPos.y + setting.offsetY.get() - height;

        if (interpolatedX == 0 && interpolatedY == 0) {
            interpolatedX = rPosX;
            interpolatedY = rPosY;
        }

        interpolatedX = MathUtils.interpolate(interpolatedX, rPosX, setting.interpolation.get());
        interpolatedY = MathUtils.interpolate(interpolatedY, rPosY, setting.interpolation.get());

        return new Vector2f(interpolatedX, interpolatedY);
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
    private Interface setting = INSTANCE.getModuleManager().getModule(Interface.class);
    private final DecimalFormat decimalFormat = new DecimalFormat("0.0");

    public TargetHUD(float x, float y, EntityPlayer target, Animation animation, boolean shader) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.animation = animation;
        this.shader = shader;
    }

    public void render() {
        setWidth(INSTANCE.getWidgetManager().get(TargetHUDWidget.class).width);
        setHeight(INSTANCE.getWidgetManager().get(TargetHUDWidget.class).height);
        GlStateManager.pushMatrix();

        GlStateManager.translate(x + width / 2F, y + height / 2F, 0);
        GlStateManager.scale(animation.getOutput(), animation.getOutput(), animation.getOutput());
        GlStateManager.translate(-(x + width / 2F), -(y + height / 2F), 0);

        float healthPercentage = PlayerUtils.getActualHealth(target) / target.getMaxHealth();
        float space = (width - 42.5f) / 100;

        target.healthAnimation.animate((100 * space) * MathHelper.clamp_float(healthPercentage, 0, 1), 50);

        if (!shader) {
            RoundedUtils.drawRound(x, y, width, height, 7, new Color(setting.bgColor(), true));

            RoundedUtils.drawRound(x + 38f, y + 28, (100 * space), 4, 2, new Color(0, 0, 0, 150));
            RoundedUtils.drawRound(x + 38f, y + 28, target.healthAnimation.getOutput(), 4, 2, new Color(setting.color(0)));
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

            RenderUtils.renderPlayerHead(target, x + 2.5f, y + 2.5f, 32, 10, -1);
        } else {
            RoundedUtils.drawShaderRound(x, y, width, height, 7, new Color(setting.bgColor()));
        }

        GlStateManager.popMatrix();
    }
}
