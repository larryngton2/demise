package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.AntiBot;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.math.MathUtils;

@ModuleInfo(name = "ImageESP", category = ModuleCategory.Visual)
public final class ImageESP extends Module {
    //todo bc im bored
    private final ModeValue mode = new ModeValue("Image", new String[]{"1", "2", "3"}, "1", this);

    private final ResourceLocation one = new ResourceLocation("demise/img/uhm/Yae Miko.png");

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        ResourceLocation optimizedResource = switch (mode.get()) {
            case "1" -> one;
            default -> null;
        };

        for (final EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.isEntityAlive() && player != mc.thePlayer && !getModule(AntiBot.class).isBot(player) && !player.isInvisible()) {
                final double x = MathUtils.interpolate(player.posX, player.lastTickPosX) - Minecraft.getMinecraft().getRenderManager().renderPosX;
                final double y = MathUtils.interpolate(player.posY, player.lastTickPosY) - Minecraft.getMinecraft().getRenderManager().renderPosY;
                final double z = MathUtils.interpolate(player.posZ, player.lastTickPosZ) - Minecraft.getMinecraft().getRenderManager().renderPosZ;

                GlStateManager.pushMatrix();
                GL11.glColor4d(1.0, 1.0, 1.0, 1.0);
                GL11.glDisable(2929);

                final float distance = MathHelper.clamp_float(mc.thePlayer.getDistanceToEntity(player), 20.0f, Float.MAX_VALUE);
                final double scale = 0.005 * distance;

                GlStateManager.translate(x, y, z);
                GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
                GlStateManager.scale(-0.1, -0.1, 0.0);

                mc.getTextureManager().bindTexture(optimizedResource);

                Gui.drawScaledCustomSizeModalRect(player.width / 2.0f - distance / 3.0f, -player.height - distance, 0.0f, 0.0f, 1.0f, 1.0f, 252f * (scale / 2f), 476f * (scale / 2f), 1.0f, 1.0f);
                GL11.glEnable(2929);

                GlStateManager.popMatrix();
            }
        }
    }
}
