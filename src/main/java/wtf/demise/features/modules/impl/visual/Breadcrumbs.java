package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimedVec3;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

@ModuleInfo(name = "Breadcrumbs", description = "Renders a trail behind you.", category = ModuleCategory.Visual)
public class Breadcrumbs extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Line", "Dot"}, "Line", this);
    private final SliderValue timeout = new SliderValue("Time", 15, 1, 150, 1, this);

    private final List<TimedVec3> path = new ArrayList<>();

    @Override
    public void onEnable() {
        path.clear();
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        path.clear();
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPre() || mode.is("Line")) {
            if (mc.thePlayer.lastTickPosX != mc.thePlayer.posX || mc.thePlayer.lastTickPosY != mc.thePlayer.posY || mc.thePlayer.lastTickPosZ != mc.thePlayer.posZ) {
                path.add(new TimedVec3(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ), System.currentTimeMillis()));
            }

            long currentTime = System.currentTimeMillis();
            path.removeIf(timedVec -> currentTime - timedVec.time > (timeout.get() * 50L));
        }
    }

    @EventTarget
    public void onRender3DEvent(Render3DEvent e) {
        switch (mode.get()) {
            case "Dot": {
                // pasted from rise 5 🤑🤑
                GlStateManager.disableDepth();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                int i = 0;
                try {
                    for (TimedVec3 vec : path) {
                        i++;

                        Vec3 v = vec.vec;

                        boolean draw = true;

                        double x = v.xCoord - (mc.getRenderManager()).renderPosX;
                        double y = v.yCoord - (mc.getRenderManager()).renderPosY;
                        double z = v.zCoord - (mc.getRenderManager()).renderPosZ;

                        double distanceFromPlayer = mc.thePlayer.getDistance(v.xCoord, v.yCoord - 1, v.zCoord);
                        int quality = (int) (distanceFromPlayer * 4 + 10);

                        if (quality > 350) quality = 350;

                        if (i % 10 != 0 && distanceFromPlayer > 25) {
                            draw = false;
                        }

                        if (i % 3 == 0 && distanceFromPlayer > 15) {
                            draw = false;
                        }

                        if (draw) {
                            GL11.glPushMatrix();
                            GL11.glTranslated(x, y, z);

                            float scale = 0.04f;
                            GL11.glScalef(-scale, -scale, -scale);

                            GL11.glRotated(-(mc.getRenderManager()).playerViewY, 0.0D, 1.0D, 0.0D);
                            GL11.glRotated((mc.getRenderManager()).playerViewX, 1.0D, 0.0D, 0.0D);

                            Color c = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(i));

                            RenderUtils.drawFilledCircleNoGL(0, 0, 0.7, c.hashCode(), quality);

                            if (distanceFromPlayer < 4)
                                RenderUtils.drawFilledCircleNoGL(0, 0, 1.4, new Color(c.getRed(), c.getGreen(), c.getBlue(), 50).hashCode(), quality);

                            if (distanceFromPlayer < 20)
                                RenderUtils.drawFilledCircleNoGL(0, 0, 2.3, new Color(c.getRed(), c.getGreen(), c.getBlue(), 30).hashCode(), quality);

                            GL11.glScalef(0.8f, 0.8f, 0.8f);

                            GL11.glPopMatrix();
                        }
                    }
                } catch (ConcurrentModificationException ignored) {
                }

                GL11.glDisable(GL11.GL_LINE_SMOOTH);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_BLEND);
                GlStateManager.enableDepth();

                GL11.glColor3d(255, 255, 255);
            }
            break;
            case "Line": {
                GlStateManager.pushMatrix();
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.disableTexture2D();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);

                GL11.glLineWidth(2.0f);

                long currentTime = System.currentTimeMillis();

                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int i = 0; i < path.size(); i++) {
                    TimedVec3 timedVec = path.get(i);
                    Vec3 v = timedVec.vec;

                    double x = v.xCoord - mc.getRenderManager().renderPosX;
                    double y = v.yCoord - mc.getRenderManager().renderPosY;
                    double z = v.zCoord - mc.getRenderManager().renderPosZ;

                    float alpha = 1.0f - (float) (currentTime - timedVec.time) / (timeout.get() * 50L);
                    alpha = Math.max(0.0f, Math.min(1.0f, alpha));

                    Color color = new Color(getModule(Interface.class).color(i));
                    GL11.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, alpha);

                    GL11.glVertex3d(x, y, z);
                }
                GL11.glEnd();

                GL11.glDisable(GL11.GL_LINE_SMOOTH);
                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
            }
            break;
        }
    }
}