package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

public class GuiWinGame extends GuiScreen {
    private static final Logger logger = LogManager.getLogger();
    private static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("textures/misc/vignette.png");
    private int field_146581_h;
    private List<String> field_146582_i;
    private int field_146579_r;
    private final float field_146578_s = 0.5F;

    public void updateScreen() {
        MusicTicker musicticker = mc.getMusicTicker();
        SoundHandler soundhandler = mc.getSoundHandler();

        if (this.field_146581_h == 0) {
            musicticker.func_181557_a();
            musicticker.func_181558_a(MusicTicker.MusicType.CREDITS);
            soundhandler.resumeSounds();
        }

        soundhandler.update();
        ++this.field_146581_h;
        float f = (float) (this.field_146579_r + height + height + 24) / this.field_146578_s;

        if ((float) this.field_146581_h > f) {
            this.sendRespawnPacket();
        }
    }

    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.sendRespawnPacket();
        }
    }

    private void sendRespawnPacket() {
        mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.PERFORM_RESPAWN));
        mc.displayGuiScreen(null);
    }

    public void initGui() {
        if (this.field_146582_i == null) {
            this.field_146582_i = Lists.newArrayList();

            try {
                String s = "";
                String s1 = "" + EnumChatFormatting.WHITE + EnumChatFormatting.OBFUSCATED + EnumChatFormatting.GREEN + EnumChatFormatting.AQUA;
                int i = 274;
                InputStream inputstream = mc.getResourceManager().getResource(new ResourceLocation("texts/end.txt")).getInputStream();
                BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(inputstream, Charsets.UTF_8));
                Random random = new Random(8124371L);

                while ((s = bufferedreader.readLine()) != null) {
                    String s2;
                    String s3;

                    for (s = s.replaceAll("PLAYERNAME", mc.getSession().getUsername()); s.contains(s1); s = s2 + EnumChatFormatting.WHITE + EnumChatFormatting.OBFUSCATED + "XXXXXXXX".substring(0, random.nextInt(4) + 3) + s3) {
                        int j = s.indexOf(s1);
                        s2 = s.substring(0, j);
                        s3 = s.substring(j + s1.length());
                    }

                    this.field_146582_i.addAll(mc.fontRendererObj.listFormattedStringToWidth(s, i));
                    this.field_146582_i.add("");
                }

                inputstream.close();

                for (int k = 0; k < 8; ++k) {
                    this.field_146582_i.add("");
                }

                inputstream = mc.getResourceManager().getResource(new ResourceLocation("texts/credits.txt")).getInputStream();
                bufferedreader = new BufferedReader(new InputStreamReader(inputstream, Charsets.UTF_8));

                while ((s = bufferedreader.readLine()) != null) {
                    s = s.replaceAll("PLAYERNAME", mc.getSession().getUsername());
                    s = s.replaceAll("\t", "    ");
                    this.field_146582_i.addAll(mc.fontRendererObj.listFormattedStringToWidth(s, i));
                    this.field_146582_i.add("");
                }

                inputstream.close();
                this.field_146579_r = this.field_146582_i.size() * 12;
            } catch (Exception exception) {
                logger.error("Couldn't load credits", exception);
            }
        }
    }

    private void drawWinGameScreen(int p_146575_1_, int p_146575_2_, float p_146575_3_) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        mc.getTextureManager().bindTexture(Gui.optionsBackground);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        int i = width;
        float f = 0.0F - ((float) this.field_146581_h + p_146575_3_) * 0.5F * this.field_146578_s;
        float f1 = (float) height - ((float) this.field_146581_h + p_146575_3_) * 0.5F * this.field_146578_s;
        float f2 = 0.015625F;
        float f3 = ((float) this.field_146581_h + p_146575_3_ - 0.0F) * 0.02F;
        float f4 = (float) (this.field_146579_r + height + height + 24) / this.field_146578_s;
        float f5 = (f4 - 20.0F - ((float) this.field_146581_h + p_146575_3_)) * 0.005F;

        if (f5 < f3) {
            f3 = f5;
        }

        if (f3 > 1.0F) {
            f3 = 1.0F;
        }

        f3 = f3 * f3;
        f3 = f3 * 96.0F / 255.0F;
        worldrenderer.pos(0.0D, height, zLevel).tex(0.0D, f * f2).color(f3, f3, f3, 1.0F).endVertex();
        worldrenderer.pos(i, height, zLevel).tex((float) i * f2, f * f2).color(f3, f3, f3, 1.0F).endVertex();
        worldrenderer.pos(i, 0.0D, zLevel).tex((float) i * f2, f1 * f2).color(f3, f3, f3, 1.0F).endVertex();
        worldrenderer.pos(0.0D, 0.0D, zLevel).tex(0.0D, f1 * f2).color(f3, f3, f3, 1.0F).endVertex();
        tessellator.draw();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawWinGameScreen(mouseX, mouseY, partialTicks);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 274;
        int j = width / 2 - i / 2;
        int k = height + 50;
        float f = -((float) this.field_146581_h + partialTicks) * this.field_146578_s;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, f, 0.0F);
        mc.getTextureManager().bindTexture(MINECRAFT_LOGO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(j, k, 0, 0, 155, 44);
        drawTexturedModalRect(j + 155, k, 0, 45, 155, 44);
        int l = k + 200;

        for (int i1 = 0; i1 < this.field_146582_i.size(); ++i1) {
            if (i1 == this.field_146582_i.size() - 1) {
                float f1 = (float) l + f - (float) (height / 2 - 6);

                if (f1 < 0.0F) {
                    GlStateManager.translate(0.0F, -f1, 0.0F);
                }
            }

            if ((float) l + f + 12.0F + 8.0F > 0.0F && (float) l + f < (float) height) {
                String s = this.field_146582_i.get(i1);

                if (s.startsWith("[C]")) {
                    this.fontRendererObj.drawStringWithShadow(s.substring(3), (float) (j + (i - this.fontRendererObj.getStringWidth(s.substring(3))) / 2), (float) l, 16777215);
                } else {
                    this.fontRendererObj.fontRandom.setSeed((long) i1 * 4238972211L + (long) (this.field_146581_h / 4));
                    this.fontRendererObj.drawStringWithShadow(s, (float) j, (float) l, 16777215);
                }
            }

            l += 12;
        }

        GlStateManager.popMatrix();
        mc.getTextureManager().bindTexture(VIGNETTE_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(0, 769);
        int j1 = width;
        int k1 = height;
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldrenderer.pos(0.0D, k1, zLevel).tex(0.0D, 1.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(j1, k1, zLevel).tex(1.0D, 1.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(j1, 0.0D, zLevel).tex(1.0D, 0.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        worldrenderer.pos(0.0D, 0.0D, zLevel).tex(0.0D, 0.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
