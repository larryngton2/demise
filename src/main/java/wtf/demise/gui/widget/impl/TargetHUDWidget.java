package wtf.demise.gui.widget.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjglx.util.vector.Vector2f;
import wtf.demise.Demise;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.modules.impl.visual.TargetHud;
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
        this.height = 37;
        this.width = 120;

        //todo improve
        if (targetHud.esp.get() && targetHud.targetHUDTracking.get()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityPlayer entityPlayer && PlayerUtils.getDistanceToEntityBox(entityPlayer) < 6) {
                    if (entityPlayer == mc.thePlayer) continue;
                    if (targetHud.target != null && entityPlayer == targetHud.target) {
                        renderTargetHUD(false, entityPlayer, false);
                        continue;
                    }

                    renderTargetHUD(false, entityPlayer, true);
                }
            }
        } else if (targetHud.target != null) {
            renderTargetHUD(false, (EntityPlayer) targetHud.target, false);
        }
    }

    @Override
    public void onShader(Shader2DEvent e) {
        if (targetHud.esp.get() && targetHud.targetHUDTracking.get()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityPlayer entityPlayer && PlayerUtils.getDistanceToEntityBox(entityPlayer) < 6) {
                    if (entityPlayer == mc.thePlayer) continue;
                    if (targetHud.target != null && entityPlayer == targetHud.target) {
                        renderTargetHUD(true, entityPlayer, false);
                        continue;
                    }

                    renderTargetHUD(true, entityPlayer, true);
                }
            }
        } else if (targetHud.target != null) {
            renderTargetHUD(true, (EntityPlayer) targetHud.target, false);
        }
    }

    private void renderTargetHUD(boolean shader, EntityPlayer target, boolean visibleCheck) {
        if (target != null) {
            TargetHUD targetHUD;
            if (!targetHud.targetHUDTracking.get()) {
                targetHUD = new TargetHUD(renderX, renderY, target, targetHud.decelerateAnimation, shader);
            } else {
                float[] pos = new float[]{getPos(target, visibleCheck).x, getPos(target, visibleCheck).y};
                targetHUD = new TargetHUD(pos[0], pos[1], target, targetHud.decelerateAnimation, shader);
            }
            targetHUD.render();
        }
    }

    private Vector2f getPos(Entity target, boolean visibleCheck) {
        float x = (float) MathUtils.interpolate(target.prevPosX, target.posX);
        float y = (float) MathUtils.interpolate(target.prevPosY, target.posY) + target.height;
        float z = (float) MathUtils.interpolate(target.prevPosZ, target.posZ);

        x -= (float) mc.getRenderManager().viewerPosX;
        y -= (float) mc.getRenderManager().viewerPosY;
        z -= (float) mc.getRenderManager().viewerPosZ;

        Vector2f pos = RenderUtils.worldToScreen(x, y, z, sr, visibleCheck);

        if (pos == null) {
            if (!targetHud.esp.get()) {
                interpolatedX = MathUtils.interpolate(interpolatedX, renderX, 0.05f);
                interpolatedY = MathUtils.interpolate(interpolatedY, renderY, 0.05f);

                return new Vector2f(interpolatedX, interpolatedY);
            } else {
                return new Vector2f(renderX, renderY);
            }
        }

        Vector2f rPos = new Vector2f((float) Math.floor(pos.x), (float) Math.floor(pos.y));
        float rPosX = rPos.x + (targetHud.centerX.get() ? target.width - width / 2 : targetHud.offsetX.get());
        float rPosY = (int) rPos.y + targetHud.offsetY.get() - height;

        if ((interpolatedX == 0 && interpolatedY == 0) || targetHud.esp.get()) {
            interpolatedX = rPosX;
            interpolatedY = rPosY;
        } else {
            interpolatedX = MathUtils.interpolate(interpolatedX, rPosX, targetHud.interpolation.get());
            interpolatedY = MathUtils.interpolate(interpolatedY, rPosY, targetHud.interpolation.get());
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
            RoundedUtils.drawShaderRound(x, y, width, height, 7, new Color(setting.bgColor()));
        }

        GlStateManager.popMatrix();
    }
}
