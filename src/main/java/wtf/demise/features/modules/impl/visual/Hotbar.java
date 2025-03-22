package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseOutSine;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

import static net.minecraft.client.gui.Gui.drawTexturedModalRect;

@ModuleInfo(name = "Hotbar", category = ModuleCategory.Visual)
public class Hotbar extends Module {
    public final BoolValue smooth = new BoolValue("Smooth", true, this);
    public final BoolValue custom = new BoolValue("Custom", true, this);

    public ScaledResolution sr;
    public float f;
    private float x;
    private final Animation anim = new EaseOutSine(175, 1);

    @Override
    public void onDisable() {
        this.setEnabled(true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        int i = sr.getScaledWidth() / 2;

        if (custom.get()) {
            if (smooth.get()) {
                x = MathUtils.lerp(x, i - 90 + SpoofSlotUtils.getSpoofedSlot() * 20, 0.25f);
            } else {
                x = i - 90 + SpoofSlotUtils.getSpoofedSlot() * 20;
            }

            RoundedUtils.drawRound(i - 90, sr.getScaledHeight() - 26, 181, 21, 8, new Color(getModule(Interface.class).bgColor(), true));
            RoundedUtils.drawRound(x, sr.getScaledHeight() - 26, 21, 21, 8, new Color(getModule(Interface.class).bgColor(), true).darker().darker());

            Gui.zLevel = f;

            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();

            for (int j = 0; j < 9; ++j) {
                float k = i - 90 + (j * 20) + 2.5f;
                float l = sr.getScaledHeight() - 23.5f;
                GuiIngame.renderHotbarItem(j, k, l, mc.timer.partialTicks, mc.thePlayer);
            }

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
        }

        renderRealSlot(i, false);
    }

    private void renderRealSlot(int i, boolean shader) {
        anim.setDirection(SpoofSlotUtils.isSpoofing() ? Direction.FORWARDS : Direction.BACKWARDS);
        if (!SpoofSlotUtils.isSpoofing() && anim.isDone()) return;
        ItemStack heldItem = mc.thePlayer.getCurrentEquippedItem();
        int count = heldItem == null ? 0 : heldItem.stackSize;
        String countStr = String.valueOf(count);
        float output = (float) anim.getOutput();
        float blockWH = 15;
        int spacing = 3;

        float totalWidth;
        String text;

        if (heldItem != null) {
            if (heldItem.getItem() instanceof ItemBlock) {
                text = "§l" + countStr + "§r block" + (count != 1 ? "s" : "");
            } else {
                text = "";
            }

            float textWidth = Fonts.interBold.get(18).getStringWidth(text);

            if (heldItem.getItem() instanceof ItemBlock) {
                totalWidth = ((textWidth + blockWH + spacing) + 6);
            } else {
                totalWidth = ((blockWH + spacing) + 3);
            }
        } else {
            text = "";
            totalWidth = ((Fonts.interBold.get(18).getStringWidth(text) + blockWH + spacing) + 3);
        }

        float height = 20;

        float renderX = (i - totalWidth / 2);
        float renderY = (sr.getScaledHeight() - 80 - 5) / output;

        GL11.glPushMatrix();

        if (!shader) {
            RoundedUtils.drawRound(renderX, renderY, totalWidth, height, 5, new Color(getModule(Interface.class).bgColor(), true));

            Fonts.interBold.get(18).drawString(text, renderX + 3 + blockWH + spacing, renderY + height / 2F - Fonts.interBold.get(18).getHeight() / 2F + 2.5f, -1);

            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(heldItem, (int) renderX + 3, (int) (renderY + 10 - (blockWH / 2)));
            RenderHelper.disableStandardItemLighting();
        } else {
            RoundedUtils.drawRound(renderX, renderY, totalWidth, height, 5, Color.black);
        }

        GL11.glPopMatrix();
    }

    @EventTarget
    public void onShader2D(Shader2DEvent e) {
        int i = sr.getScaledWidth() / 2;

        if (custom.get()) {
            RoundedUtils.drawRound(i - 90, sr.getScaledHeight() - 26, 181, 21, 8, Color.black);

            if (e.getShaderType() == Shader2DEvent.ShaderType.SHADOW) {
                RoundedUtils.drawRound(x, sr.getScaledHeight() - 26, 21, 21, 8, Color.black);
            }
        } else {
            if (e.getShaderType() == Shader2DEvent.ShaderType.SHADOW  || e.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                drawTexturedModalRect(i - 91, sr.getScaledHeight() - 22, 0, 0, 182, 22);
            }
        }

        renderRealSlot(i, true);
    }
}
