package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "ChinaHat", description = "A rong rong time ago...", category = ModuleCategory.Visual)
public class ChinaHat extends Module {
    private final ModeValue quality = new ModeValue("Quality", new String[]{
            "Not china hat anymore",
            "Umbrella",
            "Very Low",
            "Low",
            "Normal",
            "High",
            "Very High",
            "Smooth"
    }, "Smooth", this);
    private final BoolValue showInFirstPerson = new BoolValue("Show in First Person", false, this);
    private final BoolValue rotate = new BoolValue("Rotate", false, this);

    public static long lastFrame = 0;

    @EventTarget
    public void onRender3DEvent(Render3DEvent event) {
        if (mc.gameSettings.thirdPersonView == 0 && !showInFirstPerson.get()) {
            return;
        }

        lastFrame = System.currentTimeMillis();

        GL11.glPushMatrix();
        GL11.glDisable(GL_TEXTURE_2D);
        GL11.glEnable(GL_LINE_SMOOTH);

        GL11.glEnable(GL_POINT_SMOOTH);
        GL11.glEnable(GL_BLEND);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        final double x = mc.thePlayer.lastTickPosX +
                (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosX;
        final double y = (mc.thePlayer.lastTickPosY +
                (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosY
        ) + mc.thePlayer.getEyeHeight() + 0.5 + (mc.thePlayer.isSneaking() ? -0.2 : 0);
        final double z = mc.thePlayer.lastTickPosZ +
                (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * mc.timer.renderPartialTicks -
                mc.getRenderManager().viewerPosZ;

        Color c;

        final double rad = 0.65f;

        int q = 64;

        boolean increaseCount = false;

        switch (quality.get()) {
            case "Not china hat anymore":
                q = 8;
                increaseCount = true;
                break;
            case "Umbrella":
                q = 16;
                break;
            case "Very Low":
                q = 32;
                increaseCount = true;
                break;
            case "Low":
                increaseCount = true;
                break;
            case "Normal":
                q = 128;
                break;
            case "High":
                q = 256;
                increaseCount = true;
                break;
            case "Very High":
                q = 512;
                increaseCount = true;
                break;
            case "Smooth":
                q = 1024;
                increaseCount = true;
                break;
        }

        final double rotations = rotate.get() ? ((mc.thePlayer.prevRenderYawOffset +
                (mc.thePlayer.renderYawOffset - mc.thePlayer.prevRenderYawOffset
                ) * event.partialTicks()
        ) / 60
        ) + 20 : 0;

        for (float i = 0; i < Math.PI * 2 + (increaseCount ? 0.01 : 0); i += (float) (Math.PI * 4 / q)) {
            final double vecX = x + rad * Math.cos(i + rotations);
            final double vecZ = z + rad * Math.sin(i + rotations);

            c = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(0));

            GL11.glColor4f(c.getRed() / 255.F,
                    c.getGreen() / 255.F,
                    c.getBlue() / 255.F,
                    0.8f
            );

            GL11.glVertex3d(vecX, y - 0.25, vecZ);

            GL11.glColor4f(c.getRed() / 255.F,
                    c.getGreen() / 255.F,
                    c.getBlue() / 255.F,
                    0.8f
            );

            GL11.glVertex3d(x, y, z);

        }

        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glEnable(GL_DEPTH_TEST);
        GlStateManager.enableCull();
        GL11.glDisable(GL_LINE_SMOOTH);
        GL11.glEnable(GL_POINT_SMOOTH);
        GL11.glEnable(GL_TEXTURE_2D);
        GL11.glPopMatrix();

        GL11.glColor3f(255, 255, 255);
    }
}
