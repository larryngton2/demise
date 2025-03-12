package wtf.demise.gui.widget.impl;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.DecelerateAnimation;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class ScaffoldCounterWidget extends Widget {
    public ScaffoldCounterWidget() {
        super("Scaffold Counter");
        this.x = 0.4f;
        this.y = 0.7f;
    }

    private final Animation anim = new DecelerateAnimation(175, 1);

    @Override
    public void render() {
        Scaffold scaffold = INSTANCE.getModuleManager().getModule(Scaffold.class);
        switch (scaffold.counter.get().toLowerCase()) {
            case "normal": {
                anim.setDirection(scaffold.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!scaffold.isEnabled() && anim.isDone()) return;
                int slot = scaffold.getBlockSlot();
                ItemStack heldItem = slot == -1 ? null : mc.thePlayer.inventory.mainInventory[slot];
                int count = slot == -1 ? 0 : scaffold.getBlockCount();
                String countStr = String.valueOf(count);
                float output = (float) anim.getOutput();
                float blockWH = heldItem != null ? 15 : -2;
                int spacing = 3;
                String text = "§l" + countStr + "§r block" + (count != 1 ? "s" : "");
                float textWidth = Fonts.interBold.get(18).getStringWidth(text);

                float totalWidth = ((textWidth + blockWH + spacing) + 6) * output;
                this.width = totalWidth;
                float height = 20;
                this.height = height;
                GL11.glPushMatrix();
                RenderUtils.scissor(renderX - 1.5, renderY - 1.5, totalWidth + 3, height + 3);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                RoundedUtils.drawRound(renderX, renderY, totalWidth, height, 5, new Color(INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true));

                Fonts.interBold.get(18).drawString(text, renderX + 3 + blockWH + spacing, renderY + height / 2F - Fonts.interBold.get(18).getHeight() / 2F + 2.5f, -1);

                if (heldItem != null) {
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(heldItem, (int) renderX + 3, (int) (renderY + 10 - (blockWH / 2)));
                    RenderHelper.disableStandardItemLighting();
                }
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GL11.glPopMatrix();
                break;
            }
            case "exhibition": {
                if (!scaffold.isEnabled()) return;
                int c = ColorUtils.getColor(255, 0, 0, 150);
                if (scaffold.getBlockCount() >= 64 && 128 > scaffold.getBlockCount()) {
                    c = ColorUtils.getColor(255, 255, 0, 150);
                } else if (scaffold.getBlockCount() >= 128) {
                    c = ColorUtils.getColor(0, 255, 0, 150);
                }
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                mc.fontRendererObj.drawString(String.valueOf(scaffold.getBlockCount()), scaledResolution.getScaledWidth() / 2 - (mc.fontRendererObj.getStringWidth(String.valueOf(scaffold.getBlockCount())) / 2) - 1, scaledResolution.getScaledHeight() / 2 - 36, 0xff000000, false);
                mc.fontRendererObj.drawString(String.valueOf(scaffold.getBlockCount()), scaledResolution.getScaledWidth() / 2 - (mc.fontRendererObj.getStringWidth(String.valueOf(scaffold.getBlockCount())) / 2) + 1, scaledResolution.getScaledHeight() / 2 - 36, 0xff000000, false);
                mc.fontRendererObj.drawString(String.valueOf(scaffold.getBlockCount()), scaledResolution.getScaledWidth() / 2 - (mc.fontRendererObj.getStringWidth(String.valueOf(scaffold.getBlockCount())) / 2), scaledResolution.getScaledHeight() / 2 - 35, 0xff000000, false);
                mc.fontRendererObj.drawString(String.valueOf(scaffold.getBlockCount()), scaledResolution.getScaledWidth() / 2 - (mc.fontRendererObj.getStringWidth(String.valueOf(scaffold.getBlockCount())) / 2), scaledResolution.getScaledHeight() / 2 - 37, 0xff000000, false);
                mc.fontRendererObj.drawString(String.valueOf(scaffold.getBlockCount()), scaledResolution.getScaledWidth() / 2 - (mc.fontRendererObj.getStringWidth(String.valueOf(scaffold.getBlockCount())) / 2), scaledResolution.getScaledHeight() / 2 - 36, c, false);
                break;
            }
            case "simple": {
                if (!scaffold.isEnabled()) return;
                int c = ColorUtils.getColor(255, 0, 0, 150);
                if (scaffold.getBlockCount() >= 64 && 128 > scaffold.getBlockCount()) {
                    c = ColorUtils.getColor(255, 255, 0, 150);
                } else if (scaffold.getBlockCount() >= 128) {
                    c = ColorUtils.getColor(0, 255, 0, 150);
                }
                ScaledResolution scaledResolution = new ScaledResolution(mc);
                Fonts.interMedium.get(18).drawCenteredStringWithShadow(String.valueOf(scaffold.getBlockCount()), scaledResolution.getScaledWidth() / 2f, scaledResolution.getScaledHeight() / 2f + 10, new Color(c).brighter().getRGB());
                break;
            }
        }
    }

    @Override
    public void onShader(Shader2DEvent event) {
        Scaffold scaffold = INSTANCE.getModuleManager().getModule(Scaffold.class);

        if (scaffold.counter.get().equalsIgnoreCase("normal")) {
            anim.setDirection(scaffold.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!scaffold.isEnabled() && anim.isDone()) return;
            int slot = scaffold.getBlockSlot();
            ItemStack heldItem = slot == -1 ? null : mc.thePlayer.inventory.mainInventory[slot];
            int count = slot == -1 ? 0 : scaffold.getBlockCount();
            String countStr = String.valueOf(count);
            float output = (float) anim.getOutput();
            float blockWH = heldItem != null ? 15 : -2;
            int spacing = 3;
            String text = "§l" + countStr + "§r block" + (count != 1 ? "s" : "");
            float textWidth = Fonts.interBold.get(18).getStringWidth(text);
            float totalWidth = ((textWidth + blockWH + spacing) + 6) * output;
            float height = 20;
            GL11.glPushMatrix();
            RenderUtils.scissor(renderX - 1.5, renderY - 1.5, totalWidth + 3, height + 3);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RoundedUtils.drawRound(renderX, renderY, totalWidth, height, 5, new Color(INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true));
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glPopMatrix();
        }
    }

    @Override
    public boolean shouldRender() {
        Scaffold scaffold = INSTANCE.getModuleManager().getModule(Scaffold.class);

        return !scaffold.counter.is("None");
    }
}
