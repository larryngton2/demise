package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.events.impl.render.RenderNameTagEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.misc.CheatDetector;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.font.FontRenderer;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.GLUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@ModuleInfo(name = "NameTags", category = ModuleCategory.Visual)
public class NameTags extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Modern"}, "Modern", this);
    private final SliderValue offsetY = new SliderValue("Offset Y", 0, -20, 20, 1, this);
    private final SliderValue tagsSize = new SliderValue("Tags size", 1, 0.1f, 2, 0.05f, this);
    private final BoolValue tagsHealth = new BoolValue("Tags health", true, this);
    private final BoolValue tagsBackground = new BoolValue("Tags background", true, this);

    private final Map<EntityPlayer, float[]> entityPosMap = new HashMap<>();
    public Map<EntityPlayer, float[][]> playerRotationMap = new HashMap<>();

    @Override
    public void onDisable() {
        entityPosMap.clear();
        playerRotationMap.clear();
    }

    @EventTarget
    public void onRenderNameTag(RenderNameTagEvent event) {
        if (entityPosMap.containsKey(event.getEntity()))
            event.setCancelled(true);
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        entityPosMap.clear();
        playerRotationMap.clear();
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        renderTags(false);
    }

    @EventTarget
    public void onShader2D(Shader2DEvent e) {
        renderTags(true);
    }

    private void renderTags(boolean shader) {
        for (EntityPlayer player : entityPosMap.keySet()) {
            if ((player.getDistanceToEntity(mc.thePlayer) < 1.0F && mc.gameSettings.thirdPersonView == 0) || !RenderUtils.isBBInFrustum(player.getEntityBoundingBox()))
                continue;

            float[] positions = entityPosMap.get(player);
            float x = positions[0];
            float y = positions[1];
            float x2 = positions[2];

            FontRenderer modernFont = Fonts.interRegular.get((int) (17 * tagsSize.get()));
            String healthString = tagsHealth.get() ? " " + (MathUtils.roundToHalf(PlayerUtils.getActualHealth(player))) + EnumChatFormatting.RED + "❤" : "";
            String name = player.getDisplayName().getFormattedText() + healthString;

            if (Demise.INSTANCE.getModuleManager().getModule(CheatDetector.class).isCheater(player)) {
                name += EnumChatFormatting.RED + " [Cheater]";
            }

            float halfWidth = mode.is("Vanilla") ? (float) mc.fontRendererObj.getStringWidth(name) / 2 * tagsSize.get() : (float) modernFont.getStringWidth(name) / 2;
            float xDif = x2 - x;
            float middle = x + (xDif / 2);
            float textHeight = mode.is("Vanilla") ? mc.fontRendererObj.FONT_HEIGHT * tagsSize.get() : (float) modernFont.getHeight();
            float renderY = y - textHeight - offsetY.get();

            float left = middle - halfWidth - 1;
            float right = middle + halfWidth + 1;

            if (mode.is("Vanilla")) {
                if (tagsBackground.get()) {
                    Gui.drawRect(left, renderY - 1, right, renderY + textHeight + 1, !shader ? new Color(0x96000000).getRGB() : Color.black.getRGB());
                }

                if (!shader) {
                    mc.fontRendererObj.drawScaledString(name, middle - halfWidth, renderY + 0.5f, tagsSize.get(), -1);
                }
            } else {
                if (tagsBackground.get()) {
                    if (!shader) {
                        RoundedUtils.drawRound(middle - halfWidth - 3, renderY - 0.5f - 3, modernFont.getStringWidth(name) + 6, textHeight + 3, 4, new Color(getModule(Interface.class).bgColor(), true));
                    } else {
                        RoundedUtils.drawShaderRound(middle - halfWidth - 3, renderY - 0.5f - 3, modernFont.getStringWidth(name) + 6, textHeight + 3, 4, Color.black);
                    }
                }

                if (!shader) {
                    modernFont.drawStringWithShadow(name, middle - halfWidth, renderY + 0.5f, -1);
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!entityPosMap.isEmpty()) entityPosMap.clear();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            double posX = (MathUtils.interpolate(player.prevPosX, player.posX) - mc.getRenderManager().viewerPosX);
            double posY = (MathUtils.interpolate(player.prevPosY, player.posY) - mc.getRenderManager().viewerPosY);
            double posZ = (MathUtils.interpolate(player.prevPosZ, player.posZ) - mc.getRenderManager().viewerPosZ);

            double halfWidth = player.width / 2.0D;
            AxisAlignedBB bb = new AxisAlignedBB(posX - halfWidth, posY, posZ - halfWidth, posX + halfWidth, posY + player.height + (player.isSneaking() ? -0.2D : 0.1D), posZ + halfWidth).expand(0.1, 0.1, 0.1);

            double[][] vectors = {{bb.minX, bb.minY, bb.minZ},
                    {bb.minX, bb.maxY, bb.minZ},
                    {bb.minX, bb.maxY, bb.maxZ},
                    {bb.minX, bb.minY, bb.maxZ},
                    {bb.maxX, bb.minY, bb.minZ},
                    {bb.maxX, bb.maxY, bb.minZ},
                    {bb.maxX, bb.maxY, bb.maxZ},
                    {bb.maxX, bb.minY, bb.maxZ}};

            float[] projection;
            float[] position = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, -1.0f, -1.0F};

            for (double[] vec : vectors) {
                projection = GLUtils.project2D((float) vec[0], (float) vec[1], (float) vec[2], event.scaledResolution().getScaleFactor());
                if (projection != null && projection[2] >= 0.0F && projection[2] < 1.0F) {
                    float pX = projection[0];
                    float pY = projection[1];
                    position[0] = Math.min(position[0], pX);
                    position[1] = Math.min(position[1], pY);
                    position[2] = Math.max(position[2], pX);
                    position[3] = Math.max(position[3], pY);
                }
            }

            entityPosMap.put(player, position);
        }
    }

    public boolean isValid(Entity entity) {
        if (entity instanceof EntityPlayer player) {

            if (!player.isEntityAlive()) {
                return false;
            }

            if (player == mc.thePlayer) {
                return false;
            }

            return RenderUtils.isBBInFrustum(entity.getEntityBoundingBox()) && mc.theWorld.playerEntities.contains(player);
        }

        return false;
    }
}