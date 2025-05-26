package wtf.demise.gui.widget.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.ProjectionComponent;
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

    @Override
    public void render() {
        this.height = getTHUDHeight();
        this.width = getTHUDWidth();

        if (setting.target != null) {
            if (!setting.targetHUDTracking.get()) {
                TargetHUD targetHUD = new TargetHUD(renderX, renderY, (EntityPlayer) setting.target, setting.decelerateAnimation, false);
                targetHUD.render();
            } else {
                renderHUDOnTarget(setting.target, setting.target.posX, setting.target.posY, setting.target.posZ);
            }
        }
    }

    //todo... to fucking do
    protected void renderHUDOnTarget(Entity entityIn, double x, double y, double z) {
        double d0 = entityIn.getDistanceSqToEntity(mc.getRenderManager().livingPlayer);

        if (d0 <= (double) (64 * 64)) {
            float f = 1.6F;
            float f1 = 0.016666668F * f;
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height * 0.85f, (float) z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-f1, -f1, f1);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            int i = 0;

            int j = (int) (getTHUDWidth() / 2);
            GlStateManager.disableTexture2D();
            GlStateManager.enableTexture2D();
            TargetHUD targetHUD = new TargetHUD(-j, i, (EntityPlayer) entityIn, setting.decelerateAnimation, false);
            targetHUD.render();
            //fontrenderer.drawStringWithShadow(str, -j, i, 553648127);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            targetHUD.render();
            //fontrenderer.drawStringWithShadow(str, -j, i, -1);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void onShader(Shader2DEvent event) {
        if (setting.target != null) {
            this.height = getTHUDHeight();
            this.width = getTHUDWidth();
            TargetHUD targetHUD = new TargetHUD(renderX, renderY, (EntityPlayer) setting.target, setting.decelerateAnimation, true);
            targetHUD.render();
        }
    }

    public float getTHUDWidth() {
        return 120;
    }

    public float getTHUDHeight() {
        return 37;
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
        setWidth(INSTANCE.getWidgetManager().get(TargetHUDWidget.class).getTHUDWidth());
        setHeight(INSTANCE.getWidgetManager().get(TargetHUDWidget.class).getTHUDHeight());
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
