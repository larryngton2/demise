package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ColorValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

@ModuleInfo(name = "ChestESP", description = "ESP but for chests.")
public class ChestESP extends Module {

    private final ModeValue mode = new ModeValue("Mode", new String[]{"3D", "2D"}, "3D", this);
    public final BoolValue outline = new BoolValue("Outline", false, this, () -> mode.is("3D"));
    public final BoolValue filled = new BoolValue("Filled", true, this, () -> mode.is("3D"));
    public final BoolValue syncColor = new BoolValue("Sync Color", false, this, () -> mode.is("3D"));
    public final ColorValue color = new ColorValue("Color", new Color(255, 255, 255, 128), this, () -> !syncColor.get() && mode.is("3D"));

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        int amount = 0;

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityEnderChest) {
                if (!tileEntity.isInvalid() && mc.theWorld.getBlockState(tileEntity.getPos()) != null) {
                    switch (mode.get()) {
                        case "3D":
                            if (syncColor.get()) {
                                RenderUtils.renderBlock(tileEntity.getPos(), getModule(Interface.class).color(0), outline.get(), filled.get());
                            } else {
                                RenderUtils.renderBlock(tileEntity.getPos(), color.get().getRGB(), outline.get(), filled.get());
                            }
                            break;
                        case "2D": {
                            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
                            GL11.glPushMatrix();

                            render2D(amount, tileEntity);

                            amount++;

                            GL11.glPopMatrix();
                            GL11.glPopAttrib();
                            break;
                        }
                    }
                }
            }
        }
    }

    private void render2D(final int amount, final TileEntity p) {
        GL11.glPushMatrix();

        final RenderManager renderManager = mc.getRenderManager();

        final double x = (p.getPos().getX() + 0.5) - renderManager.renderPosX;
        final double y = p.getPos().getY() - renderManager.renderPosY;
        final double z = (p.getPos().getZ() + 0.5) - renderManager.renderPosZ;

        GL11.glTranslated(x, y, z);

        GL11.glRotated(-renderManager.playerViewY, 0.0D, 1.0D, 0.0D);
        GL11.glRotated(renderManager.playerViewX, mc.gameSettings.thirdPersonView == 2 ? -1.0D : 1.0D, 0.0D, 0.0D);

        final float scale = 1 / 100f;
        GL11.glScalef(-scale, -scale, scale);

        final Color c = new Color(getModule(Interface.class).color(amount, 127));

        final float offset = renderManager.playerViewX * 0.5f;

        lineNoGl(-50, offset, 50, offset, c);
        lineNoGl(-50, -95 + offset, -50, offset, c);
        lineNoGl(-50, -95 + offset, 50, -95 + offset, c);
        lineNoGl(50, -95 + offset, 50, offset, c);

        GL11.glPopMatrix();
    }

    public void lineNoGl(final double firstX, final double firstY, final double secondX, final double secondY, final Color color) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();

        if (color != null)
            GL11.glColor4d(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);

        lineWidth(1);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_LINES);
        {
            GL11.glVertex2d(firstX, firstY);
            GL11.glVertex2d(secondX, secondY);

        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        Color white = Color.white;

        GL11.glColor4d(white.getRed() / 255F, white.getGreen() / 255F, white.getBlue() / 255F, white.getAlpha() / 255F);
    }

    public void lineWidth(final double width) {
        GL11.glLineWidth((float) width);
    }

    public void color(final double red, final double green, final double blue, final double alpha) {
        GL11.glColor4d(red, green, blue, alpha);
    }

    public void end() {
        GL11.glEnd();
    }

    public void begin(final int glMode) {
        GL11.glBegin(glMode);
    }

    public void pop() {
        GL11.glPopMatrix();
    }

    public void enable(final int glTarget) {
        GL11.glEnable(glTarget);
    }

    public void disable(final int glTarget) {
        GL11.glDisable(glTarget);
    }
}
