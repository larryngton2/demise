package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import org.lwjglx.util.vector.Vector2f;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.RenderNameTagEvent;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.misc.CheatDetector;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.font.FontRenderer;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ModuleInfo(name = "NameTags", description = "Renders player nametags.")
public class NameTags extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Modern"}, "Modern", this);
    private final SliderValue offsetY = new SliderValue("Offset Y", 0, -20, 20, 1, this);
    private final SliderValue tagsSize = new SliderValue("Tags size", 1, 0.1f, 2, 0.05f, this);
    private final BoolValue tagsHealth = new BoolValue("Tags health", true, this);
    private final BoolValue tagsBackground = new BoolValue("Tags background", true, this);

    private final Map<UUID, Vector2f> posMap = new HashMap<>();

    @EventTarget
    public void onRenderNameTag(RenderNameTagEvent e) {
        if (isValid(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        renderTags(false, false);
    }

    @EventTarget
    public void onShader2D(ShaderEvent e) {
        renderTags(true, e.getShaderType() == ShaderEvent.ShaderType.GLOW);
    }

    private void renderTags(boolean shader, boolean isGlow) {
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isValid(player) || !RenderUtils.isBBInFrustum(player.getEntityBoundingBox())) continue;

            float playerX = (float) MathUtils.interpolate(player.prevPosX, player.posX);
            float playerY = (float) MathUtils.interpolate(player.prevPosY, player.posY) + player.height + 25f / 100;
            float playerZ = (float) MathUtils.interpolate(player.prevPosZ, player.posZ);

            Vector2f pos = RenderUtils.worldToScreen(playerX, playerY, playerZ, new ScaledResolution(mc), true);

            if (pos == null) {
                pos = posMap.getOrDefault(player.getUniqueID(), new Vector2f(0, 0));
            }

            posMap.put(player.getUniqueID(), pos);

            float x = pos.x;
            float y = pos.y;

            FontRenderer modernFont = Fonts.interRegular.get((int) (17 * tagsSize.get()));
            String healthString = tagsHealth.get() ? " " + (MathUtils.roundToHalf(PlayerUtils.getActualHealth(player))) + EnumChatFormatting.RED + "❤" : "";
            String name = player.getDisplayName().getFormattedText() + healthString;

            if (Demise.INSTANCE.getFriendManager().isFriend(player)) {
                name += EnumChatFormatting.GREEN + " [Friend]";
            }

            if (Demise.INSTANCE.getModuleManager().getModule(CheatDetector.class).isCheater(player)) {
                name += EnumChatFormatting.RED + " [Cheater]";
            }

            float halfWidth = mode.is("Vanilla") ? (float) mc.fontRendererObj.getStringWidth(name) / 2 * tagsSize.get() : (float) modernFont.getStringWidth(name) / 2;
            float middle = x - halfWidth;
            float textHeight = mode.is("Vanilla") ? mc.fontRendererObj.FONT_HEIGHT * tagsSize.get() : (float) modernFont.getHeight();
            float renderY = y - textHeight - offsetY.get();

            float left = middle - halfWidth - 1;
            float right = middle + halfWidth + 1;

            if (mode.is("Vanilla")) {
                if (tagsBackground.get()) {
                    Gui.drawRect(left, renderY - 1, right, renderY + textHeight + 1, (!shader ? new Color(0x96000000).getRGB() : (isGlow ? getModule(Interface.class).color() : Color.black.getRGB())));
                }

                if (!shader) {
                    mc.fontRendererObj.drawScaledString(name, middle + 3, renderY + 0.5f, tagsSize.get(), -1);
                }
            } else {
                if (tagsBackground.get()) {
                    if (!shader) {
                        RoundedUtils.drawRound(middle, renderY - 0.5f - 3, modernFont.getStringWidth(name) + 6, textHeight + 3, 4, new Color(getModule(Interface.class).bgColor(), true));
                    } else {
                        if (isGlow) {
                            RoundedUtils.drawGradientPreset(middle, renderY - 0.5f - 3, modernFont.getStringWidth(name) + 6, textHeight + 3, 4);
                        } else {
                            RoundedUtils.drawShaderRound(middle, renderY - 0.5f - 3, modernFont.getStringWidth(name) + 6, textHeight + 3, 4, Color.black);
                        }
                    }
                }

                if (!shader) {
                    modernFont.drawStringWithShadow(name, middle + 3, renderY + 0.5f, -1);
                }
            }
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