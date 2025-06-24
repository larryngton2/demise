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
import wtf.demise.features.modules.impl.visual.CustomWidgetsModule;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.mainmenu.GuiMainMenu;
import wtf.demise.gui.widget.impl.ModuleListWidget;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.player.PlayerUtils;
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
    private float interpolatedChatHeight;
    private float interpolatedWidgetY = sr == null ? 1080 : sr.getScaledHeight() - 80 - 5;
    private float interpolatedChatY;
    private boolean fade = false;
    private boolean renderText = false;
    private float alpha = 255;
    private float tAlpha = 255;
    private final TimerUtils textTimer = new TimerUtils();
    private final TimerUtils fadeTimer = new TimerUtils();
    private final TimerUtils textFadeTimer = new TimerUtils();
    private final CustomWidgetsModule customWidgetsModule = Demise.INSTANCE.getModuleManager().getModule(CustomWidgetsModule.class);
    private float interpolatedWidgetWidth;
    private float interpolatedWidgetX;
    private float interpolatedScoreY;
    private float interpolatedScorebgY;
    private float interpolatedScoreHeight;
    private float interpolatedScoreWidth;

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        int i = sr.getScaledWidth() / 2;

        if (!customWidgetsModule.isEnabled()) {
            return;
        }

        if (customWidgetsModule.hotbar.get() && customWidgetsModule.isEnabled()) {
            drawCustomHotbar(i);
        }

        drawHotbarWidget(i, false);

        if (customWidgetsModule.chat.get()) {
            drawChat(GuiIngame.getUpdateCounter(), false);
        }

        if (customWidgetsModule.scoreboard.get()) {
            drawScoreboard(scoreObjective, sr, false);
        }

        if (fade) {
            RenderUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, (int) alpha).getRGB());

            if (fadeTimer.hasTimeElapsed(10)) {
                alpha -= 0.75f;
                fadeTimer.reset();
            }

            if (alpha < 0) {
                alpha = 255;
                fade = false;
            }
        } else {
            alpha = 255;
        }

        if (renderText) {
            Fonts.interRegular.get(35).drawCenteredString("Logged in as " + mc.thePlayer.getName() + " on " + PlayerUtils.getCurrServer(), sr.getScaledWidth() / 2f, 50, new Color(255, 255, 255, (int) tAlpha).getRGB());

            if (textTimer.hasTimeElapsed(2500)) {
                if (textFadeTimer.hasTimeElapsed(10)) {
                    tAlpha -= 2;
                    textFadeTimer.reset();
                }
            }

            if (tAlpha < 0) {
                renderText = false;
            }
        } else {
            tAlpha = 255;
            textTimer.reset();
        }
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (!(mc.currentScreen instanceof GuiMainMenu)) {
            GuiMainMenu.fade = false;
        }
    }

    private void drawCustomHotbar(int i) {
        float targetPos = i - 91 + SpoofSlotUtils.getSpoofedSlot() * 20;

        if (x == 0) x = targetPos;
        x = MathUtils.interpolate(x, targetPos, 0.25f);

        RoundedUtils.drawRound(i - 91, sr.getScaledHeight() - 26, 181, 21, 7, new Color(getModule(Interface.class).bgColor(), true));
        RoundedUtils.drawRound(x, sr.getScaledHeight() - 26, 21, 21, 7, new Color(getModule(Interface.class).bgColor(), true).darker());

        Gui.zLevel = f;

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableGUIStandardItemLighting();

        for (int j = 0; j < 9; ++j) {
            float k = i - 91 + (j * 20) + 2.5f;
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

                interpolatedChatY = MathUtils.interpolate(interpolatedChatY, height, 0.25f);

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
                    interpolatedChatHeight = MathUtils.interpolate(interpolatedChatHeight, lineHeight, 0.25f);
                } else {
                    interpolatedChatHeight = MathUtils.interpolate(interpolatedChatHeight, -4, 0.25f);
                }

                if (chatBackgroundAlpha > 3) {
                    if (!shader) {
                        RoundedUtils.drawRound(-2, -interpolatedChatHeight - 2, l + 4, interpolatedChatHeight + 4, radius, new Color(getModule(Interface.class).bgColor(), true));
                    } else {
                        RoundedUtils.drawShaderRound(-2, -interpolatedChatHeight - 2, l + 4, interpolatedChatHeight + 4, radius, Color.black);
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

    private void drawHotbarWidget(int i, boolean shader) {
        ItemStack heldItem = mc.thePlayer.getCurrentEquippedItem();
        int count = heldItem == null ? 0 : heldItem.stackSize;
        String countStr = String.valueOf(count);
        float blockWH = 15;
        int spacing = 3;

        float totalWidth;
        String text;

        if (SpoofSlotUtils.isSpoofing()) {
            if (heldItem != null) {
                if (heldItem.getItem() instanceof ItemBlock) {
                    text = countStr;
                } else {
                    text = "";
                }

                float textWidth = Fonts.interMedium.get(18).getStringWidth(text);

                if (heldItem.getItem() instanceof ItemBlock) {
                    totalWidth = ((textWidth + blockWH + spacing) + 6);
                } else {
                    totalWidth = ((blockWH + spacing) + 3);
                }
            } else {
                text = "";
                totalWidth = ((Fonts.interMedium.get(18).getStringWidth(text) + blockWH + spacing) + 3);
            }
        } else {
            text = "";
            totalWidth = ((Fonts.interMedium.get(18).getStringWidth(text) + blockWH + spacing) + 3);
        }

        float height = 20;

        float renderX = (i - totalWidth / 2);
        float renderY = (sr.getScaledHeight() - 80 - 5);

        if (SpoofSlotUtils.isSpoofing()) {
            interpolatedWidgetY = MathUtils.interpolate(interpolatedWidgetY, renderY, 0.25f);
        } else {
            interpolatedWidgetY = MathUtils.interpolate(interpolatedWidgetY, sr.getScaledHeight() + height, 0.25f);
        }

        interpolatedWidgetWidth = MathUtils.interpolate(interpolatedWidgetWidth, totalWidth, 0.25f);
        interpolatedWidgetX = MathUtils.interpolate(interpolatedWidgetX, renderX, 0.25f);

        GL11.glPushMatrix();

        if (!shader) {
            RoundedUtils.drawRound(interpolatedWidgetX, interpolatedWidgetY, interpolatedWidgetWidth, height, 7, new Color(getModule(Interface.class).bgColor(), true));

            Fonts.interRegular.get(18).drawGradient(text, interpolatedWidgetX + 3 + blockWH + spacing, interpolatedWidgetY + height / 2F - Fonts.interMedium.get(18).getHeight() / 2F + 2.5f, (index) -> new Color(getModule(Interface.class).color(index)));

            if (SpoofSlotUtils.isSpoofing()) {
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(heldItem, (int) interpolatedWidgetX + 3, (int) (interpolatedWidgetY + 10 - (blockWH / 2)));
                RenderHelper.disableStandardItemLighting();
            }
        } else {
            RoundedUtils.drawShaderRound(interpolatedWidgetX, interpolatedWidgetY, interpolatedWidgetWidth, height, 7, Color.black);
        }

        GL11.glPopMatrix();
    }

    public static final Pattern LINK_PATTERN = Pattern.compile("(http(s)?://.)?(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[A-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)");

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

        int i = Fonts.interRegular.get(15).getStringWidth(objective.getDisplayName());

        for (Score score : collection) {
            ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
            String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName());
            i = Math.max(i, Fonts.interRegular.get(15).getStringWidth(s));
        }

        int i1 = collection.size() * mc.fontRendererObj.FONT_HEIGHT;
        int j1 = scaledRes.getScaledHeight() / 2 + i1 / 3;
        int k1 = 3;
        int l1 = scaledRes.getScaledWidth() - i - k1 - 10;
        int j = 0;

        int totalHeight = collection.size() * mc.fontRendererObj.FONT_HEIGHT;
        int topY = j1 - totalHeight;

        float x = l1 - 2;
        float y = j1;

        float width = (scaledRes.getScaledWidth() - k1 + 2) - (l1 - 2) + 6 - 10;
        float bgY = y - i1 - Fonts.interRegular.get(15).getHeight() - 7;
        float height = j1 - (topY - mc.fontRendererObj.FONT_HEIGHT - 1) + 7;

        Interface anInterface = getModule(Interface.class);

        if (anInterface.elements.isEnabled("Module list")) {
            while (ModuleListWidget.currY + ModuleListWidget.getEnabledModules().size() * ModuleListWidget.getModuleHeight() > bgY - 15 && ModuleListWidget.currX > x - width - 25) {
                y++;
                bgY++;
            }
        }

        interpolatedScorebgY = MathUtils.interpolate(interpolatedScorebgY, bgY, 0.25f);
        interpolatedScoreY = MathUtils.interpolate(interpolatedScoreY, y, 0.25f);
        interpolatedScoreHeight = MathUtils.interpolate(interpolatedScoreHeight, height, 0.25f);
        interpolatedScoreWidth = MathUtils.interpolate(interpolatedScoreWidth, width, 0.25f);

        y = interpolatedScoreY;

        if (!shader) {
            RoundedUtils.drawRound(x - 3, interpolatedScorebgY, interpolatedScoreWidth, interpolatedScoreHeight, 7, new Color(getModule(Interface.class).bgColor(), true));

            for (Score score1 : collection) {
                ++j;

                y -= mc.fontRendererObj.FONT_HEIGHT;

                ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
                String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());

                final Matcher linkMatcher = LINK_PATTERN.matcher(s1);

                if (j <= 2 && (linkMatcher.find() || s1.contains("net") || s1.contains("org") || s1.contains("com"))) {
                    s1 = "demise.wtf";
                    Fonts.interRegular.get(15).drawGradientWithShadow(s1, l1, y, (index) -> new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(index)));
                } else {
                    Fonts.interRegular.get(15).drawStringWithShadow(s1, l1, y, -1);
                }

                if (j == collection.size()) {
                    String s3 = objective.getDisplayName();
                    Fonts.interRegular.get(15).drawStringWithShadow(s3, l1 + i / 2f - Fonts.interRegular.get(15).getStringWidth(s3) / 2f, y - mc.fontRendererObj.FONT_HEIGHT, -1);
                }
            }
        } else {
            RoundedUtils.drawShaderRound(x - 3, interpolatedScorebgY, interpolatedScoreWidth, interpolatedScoreHeight, 7, Color.black);
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        fade = true;
        renderText = true;
    }

    @EventTarget
    public void onChatGUI(ChatGUIEvent e) {
        if (!customWidgetsModule.isEnabled()) {
            return;
        }

        if (customWidgetsModule.chat.get()) {
            drawChatScreen(false);
        }
    }

    @EventTarget
    public void onShader2D(Shader2DEvent e) {
        int i = sr.getScaledWidth() / 2;

        if (!customWidgetsModule.isEnabled()) {
            return;
        }

        if (customWidgetsModule.hotbar.get()) {
            RoundedUtils.drawShaderRound(i - 91, sr.getScaledHeight() - 26, 181, 21, 7, Color.black);

            //if (e.getShaderType() == Shader2DEvent.ShaderType.SHADOW) {
                RoundedUtils.drawShaderRound(x, sr.getScaledHeight() - 26, 21, 21, 7, Color.black);
            //}
        }

        if (customWidgetsModule.chat.get()) {
            drawChat(GuiIngame.getUpdateCounter(), true);

            if (GuiNewChat.getChatOpen()) {
                drawChatScreen(true);
            }
        }

        if (customWidgetsModule.hotbar.get()) {
            drawHotbarWidget(i, true);
        }

        if (customWidgetsModule.scoreboard.get()) {
            drawScoreboard(scoreObjective, sr, true);
        }
    }

    public <M extends Module> M getModule(Class<M> clazz) {
        return Demise.INSTANCE.getModuleManager().getModule(clazz);
    }
}