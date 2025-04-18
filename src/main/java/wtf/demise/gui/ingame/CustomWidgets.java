package wtf.demise.gui.ingame;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.render.ChatGUIEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.mainmenu.GuiMainMenu;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.client.gui.Gui.drawRect;

public class CustomWidgets implements InstanceAccess {
    public static ScaledResolution sr;
    public static float f;
    private float x;
    public static ScoreObjective scoreObjective;
    private float interpolatedHeight;
    private float interpolatedY = sr == null ? 1080 : sr.getScaledHeight() - 80 - 5;
    private float interpolatedChatY;
    private boolean fade = false;
    private int alpha = 255;

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        int i = sr.getScaledWidth() / 2;

        drawCustomHotbar(i);
        drawChat(GuiIngame.getUpdateCounter(), false);
        drawRealSlot(i, false);
        drawScoreboard(scoreObjective, sr, false);

        if (fade) {
            RenderUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, alpha).getRGB());

            alpha -= 2;

            if (alpha < 0) {
                alpha = 255;
                fade = false;
            }
        } else {
            alpha = 255;
        }
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (!(mc.currentScreen instanceof GuiMainMenu)) {
            GuiMainMenu.fade = false;
        }
    }

    private void drawCustomHotbar(int i) {
        if (x == 0) x = i - 90 + SpoofSlotUtils.getSpoofedSlot() * 20;

        x = MathUtils.interpolate(x, i - 90 + SpoofSlotUtils.getSpoofedSlot() * 20, 0.25f);

        RoundedUtils.drawRound(i - 90, sr.getScaledHeight() - 26, 181, 21, 7, new Color(getModule(Interface.class).bgColor(), true));
        RoundedUtils.drawRound(x, sr.getScaledHeight() - 26, 21, 21, 7, new Color(getModule(Interface.class).bgColor(), true).darker());

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

    public void drawChat(int updateCounter, boolean shader) {
        if (mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
            int i = GuiNewChat.getLineCount();
            boolean flag = false;
            int j = 0;
            int k = GuiNewChat.drawnChatLines.size();
            float f = mc.gameSettings.chatOpacity * 0.9F + 0.1F;

            if (k > 0) {
                if (GuiNewChat.getChatOpen()) {
                    flag = true;
                }

                float f1 = GuiNewChat.getChatScale();
                int l = MathHelper.ceiling_float_int((float) GuiNewChat.getChatWidth() / f1);
                GlStateManager.pushMatrix();

                float height;

                if (flag) {
                    height = sr.getScaledHeight() - 33;
                } else {
                    height = sr.getScaledHeight() - 10;
                }

                if (interpolatedChatY == 0) {
                    interpolatedChatY = height;
                }

                interpolatedChatY = MathUtils.interpolate(interpolatedChatY, height, 0.1f);

                GlStateManager.translate(10, interpolatedChatY, 0.0F);
                GlStateManager.scale(f1, f1, 1.0F);

                for (int i1 = 0; i1 + GuiNewChat.scrollPos < GuiNewChat.drawnChatLines.size() && i1 < i; ++i1) {
                    ChatLine chatline = GuiNewChat.drawnChatLines.get(i1 + GuiNewChat.scrollPos);

                    if (chatline != null) {
                        int j1 = updateCounter - chatline.getUpdatedCounter();

                        if (j1 < 200 || flag) {
                            ++j;
                        }
                    }
                }

                int chatBackgroundAlpha = (int) (255 * f);
                int radius = Math.min(k * 5, 7);
                int lineHeight = j * 9;

                if (lineHeight != 0) {
                    interpolatedHeight = MathUtils.interpolate(interpolatedHeight, lineHeight, 0.1f);
                } else {
                    interpolatedHeight = MathUtils.interpolate(interpolatedHeight, -4, 0.1f);
                }

                if (chatBackgroundAlpha > 3) {
                    if (!shader) {
                        RoundedUtils.drawRound(-2, -interpolatedHeight - 2, l + 4, interpolatedHeight + 4, radius, new Color(getModule(Interface.class).bgColor(), true));
                    } else {
                        RoundedUtils.drawShaderRound(-2, -interpolatedHeight - 2, l + 4, interpolatedHeight + 4, radius, Color.black);
                    }
                }

                for (int i1 = 0; i1 + GuiNewChat.scrollPos < GuiNewChat.drawnChatLines.size() && i1 < i; ++i1) {
                    ChatLine chatline = GuiNewChat.drawnChatLines.get(i1 + GuiNewChat.scrollPos);

                    if (chatline != null) {
                        int j1 = updateCounter - chatline.getUpdatedCounter();

                        if (j1 < 200 || flag) {
                            double d0 = (double) j1 / 200.0D;
                            d0 = 1.0D - d0;
                            d0 = d0 * 10.0D;
                            d0 = MathHelper.clamp_double(d0, 0.0D, 1.0D);
                            d0 = d0 * d0;
                            int l1 = (int) (255.0D * d0);

                            if (flag) {
                                l1 = 255;
                            }

                            l1 = (int) ((float) l1 * f);

                            if (l1 > 3) {
                                int i2 = 0;
                                int j2 = -i1 * 9;
                                String s = chatline.getChatComponent().getFormattedText();
                                GlStateManager.enableBlend();
                                mc.fontRendererObj.drawStringWithShadow(s, (float) i2, (float) (j2 - 8), 16777215 + (l1 << 24));
                                GlStateManager.disableAlpha();
                                GlStateManager.disableBlend();
                            }
                        }
                    }
                }

                if (flag) {
                    int k2 = mc.fontRendererObj.FONT_HEIGHT;
                    GlStateManager.translate(-3.0F, 0.0F, 0.0F);
                    int l2 = k * k2 + k;
                    int i3 = j * k2 + j;
                    int j3 = GuiNewChat.scrollPos * i3 / k;
                    int k1 = i3 * i3 / l2;

                    if (l2 != i3) {
                        int k3 = j3 > 0 ? 170 : 96;
                        int l3 = GuiNewChat.isScrolled ? 13382451 : 3355562;
                        drawRect(0, -j3, 2, -j3 - k1, l3 + (k3 << 24));
                        drawRect(2, -j3, 1, -j3 - k1, 13421772 + (k3 << 24));
                    }
                }

                GlStateManager.popMatrix();
            }
        }
    }

    public void drawChatScreen(boolean shader) {
        float width = MathHelper.ceiling_float_int((float) GuiNewChat.getChatWidth() / GuiNewChat.getChatScale()) + 4;
        float height = 12;
        float x = 8;
        float y = sr.getScaledHeight() - height - x;

        if (!shader) {
            RoundedUtils.drawRound(x, y, width, height, 5, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true));
        } else {
            RoundedUtils.drawShaderRound(x, y, width, height, 5, Color.black);
        }
    }

    private void drawRealSlot(int i, boolean shader) {
        if (!SpoofSlotUtils.isSpoofing() && interpolatedY >= sr.getScaledHeight() + 19) {
            return;
        }

        ItemStack heldItem = mc.thePlayer.getCurrentEquippedItem();
        int count = heldItem == null ? 0 : heldItem.stackSize;
        String countStr = String.valueOf(count);
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
        float renderY = (sr.getScaledHeight() - 80 - 5);

        if (SpoofSlotUtils.isSpoofing()) {
            interpolatedY = MathUtils.interpolate(interpolatedY, renderY, 0.05f);
        } else {
            interpolatedY = MathUtils.interpolate(interpolatedY, sr.getScaledHeight() + height, 0.05f);
        }

        GL11.glPushMatrix();

        if (!shader) {
            RoundedUtils.drawRound(renderX, interpolatedY, totalWidth, height, 7, new Color(getModule(Interface.class).bgColor(), true));

            Fonts.interBold.get(18).drawString(text, renderX + 3 + blockWH + spacing, interpolatedY + height / 2F - Fonts.interBold.get(18).getHeight() / 2F + 2.5f, -1);

            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(heldItem, (int) renderX + 3, (int) (interpolatedY + 10 - (blockWH / 2)));
            RenderHelper.disableStandardItemLighting();
        } else {
            RoundedUtils.drawShaderRound(renderX, interpolatedY, totalWidth, height, 7, Color.black);
        }

        GL11.glPopMatrix();
    }

    public static final Pattern LINK_PATTERN = Pattern.compile("(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[A-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&//=]*)");

    private void drawScoreboard(ScoreObjective objective, ScaledResolution scaledRes, boolean shader) {
        if (objective == null) {
            return;
        }

        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> collection = scoreboard.getSortedScores(objective);
        List<Score> list = Lists.newArrayList(Iterables.filter(collection, p_apply_1_ -> p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#")));

        if (list.size() > 15) {
            collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
        } else {
            collection = list;
        }

        int i = mc.fontRendererObj.getStringWidth(objective.getDisplayName());

        for (Score score : collection) {
            ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
            String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName());
            i = Math.max(i, mc.fontRendererObj.getStringWidth(s));
        }

        int i1 = collection.size() * mc.fontRendererObj.FONT_HEIGHT;
        int j1 = scaledRes.getScaledHeight() / 2 + i1 / 3;
        int k1 = 3;
        int l1 = scaledRes.getScaledWidth() - i - k1 - 10;
        int j = 0;

        int totalHeight = collection.size() * mc.fontRendererObj.FONT_HEIGHT;
        int topY = j1 - totalHeight;

        float x = l1 - 2;
        float y = topY - mc.fontRendererObj.FONT_HEIGHT - 1;

        float width = (scaledRes.getScaledWidth() - k1 + 2) - (l1 - 2) + 6 - 10;

        if (!shader) {
            RoundedUtils.drawRound(x - 3, y - 3, width, j1 - (topY - mc.fontRendererObj.FONT_HEIGHT - 1) + 6, 7, new Color(getModule(Interface.class).bgColor(), true));

            for (Score score1 : collection) {
                ++j;

                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());

                int k = j1 - j * mc.fontRendererObj.FONT_HEIGHT;

                final Matcher linkMatcher = LINK_PATTERN.matcher(s1);
                if (Demise.INSTANCE.getModuleManager().getModule(Interface.class).isEnabled() && linkMatcher.find()) {
                    s1 = "demise.wtf";
                    mc.fontRendererObj.drawGradientWithShadow(s1, l1, k, (index) -> new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(index)));
                } else {
                    mc.fontRendererObj.drawStringWithShadow(s1, l1, k, 553648127);
                }

                if (j == collection.size()) {
                    String s3 = objective.getDisplayName();
                    mc.fontRendererObj.drawStringWithShadow(s3, l1 + i / 2 - mc.fontRendererObj.getStringWidth(s3) / 2, k - mc.fontRendererObj.FONT_HEIGHT, 553648127);
                }
            }
        } else {
            RoundedUtils.drawShaderRound(x - 3, y - 3, width, j1 - (topY - mc.fontRendererObj.FONT_HEIGHT - 1) + 6, 7, Color.black);
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        fade = true;
    }

    @EventTarget
    public void onChatGUI(ChatGUIEvent e) {
        drawChatScreen(false);
    }

    @EventTarget
    public void onShader2D(Shader2DEvent e) {
        int i = sr.getScaledWidth() / 2;

        RoundedUtils.drawShaderRound(i - 90, sr.getScaledHeight() - 26, 181, 21, 7, Color.black);

        if (e.getShaderType() == Shader2DEvent.ShaderType.SHADOW) {
            RoundedUtils.drawShaderRound(x, sr.getScaledHeight() - 26, 21, 21, 7, Color.black);
        }

        drawChat(GuiIngame.getUpdateCounter(), true);

        if (GuiNewChat.getChatOpen()) {
            drawChatScreen(true);
        }

        drawRealSlot(i, true);
        drawScoreboard(scoreObjective, sr, true);
    }

    public <M extends Module> M getModule(Class<M> clazz) {
        return Demise.INSTANCE.getModuleManager().getModule(clazz);
    }
}