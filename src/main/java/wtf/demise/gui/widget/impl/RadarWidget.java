package wtf.demise.gui.widget.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.impl.combat.AntiBot;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class RadarWidget extends Widget {
    public RadarWidget() {
        super("Radar");
        Demise.INSTANCE.getEventManager().unregister(this);
        Demise.INSTANCE.getEventManager().register(this);
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPost()) return;

        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (Demise.INSTANCE.getModuleManager().getModule(AntiBot.class).isBot(p) && Demise.INSTANCE.getModuleManager().getModule(AntiBot.class).isEnabled())
                continue;

            p.lastDistanceFromPlayerX = p.distanceFromPlayerX;
            p.lastDistanceFromPlayerZ = p.distanceFromPlayerZ;

            p.distanceFromPlayerX = mc.thePlayer.posX - p.posX;
            p.distanceFromPlayerZ = mc.thePlayer.posZ - p.posZ;

            p.distanceFromPlayerX *= 1.6f;
            p.distanceFromPlayerZ *= 1.6f;
        }
    }

    @Override
    public void render() {
        this.width = this.height = 100;

        int middleX = (int) (renderX + width / 2);
        int middleY = (int) (renderY + height / 2);

        RoundedUtils.drawRound(renderX, renderY, width, height, 5, new Color(setting.bgColor(), true));

        GlStateManager.pushMatrix();

        GlStateManager.translate(middleX, middleY, 0);
        GlStateManager.rotate(mc.thePlayer.rotationYaw, 0, 0, -1);
        GlStateManager.translate(-middleX, -middleY, 0);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(renderX, renderY, width, height);

        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (Demise.INSTANCE.getModuleManager().getModule(AntiBot.class).isBot(p) && Demise.INSTANCE.getModuleManager().getModule(AntiBot.class).isEnabled())
                continue;

            Color c = new Color(setting.color());

            double renderCurrentPosX = p.lastDistanceFromPlayerX + (p.distanceFromPlayerX - p.lastDistanceFromPlayerX) * mc.timer.partialTicks;
            double renderCurrentPosZ = p.lastDistanceFromPlayerZ + (p.distanceFromPlayerZ - p.lastDistanceFromPlayerZ) * mc.timer.partialTicks;

            double distance = Math.abs(renderCurrentPosX) + Math.abs(renderCurrentPosZ);
            float maxOpacityDistance = 75;

            if (distance < maxOpacityDistance) {
                RenderUtils.drawFilledCircle(middleX + renderCurrentPosX, middleY + renderCurrentPosZ, 1, c.hashCode(), 40);
                RenderUtils.drawFilledCircle(middleX + renderCurrentPosX, middleY + renderCurrentPosZ, 2, new Color(c.getRed(), c.getGreen(), c.getBlue(), 30).hashCode(), 40);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GlStateManager.popMatrix();

        GL11.glColor3d(255, 255, 255);
    }

    @Override
    public void onShader(Shader2DEvent event) {
        RoundedUtils.drawShaderRound(renderX, renderY, width, height, 5, Color.BLACK);
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Radar");
    }
}
