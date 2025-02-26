package wtf.demise.features.modules.impl.visual;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.click.neverlose.NeverLose;
import wtf.demise.gui.font.FontRenderer;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.DecelerateAnimation;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;

import static wtf.demise.gui.click.neverlose.NeverLose.*;

@ModuleInfo(name = "Interface", category = ModuleCategory.Visual)
public class Interface extends Module {
    public final TextValue clientName = new TextValue("Client Name", "demise", this);

    public final MultiBoolValue elements = new MultiBoolValue("Elements", Arrays.asList(
            new BoolValue("Watermark", true),
            new BoolValue("Module List", true),
            new BoolValue("Armor", true),
            new BoolValue("Info", true),
            new BoolValue("Health", false),
            new BoolValue("Potion HUD", true),
            new BoolValue("Target HUD", true),
            new BoolValue("Notification", false),
            new BoolValue("Session Info", false)
    ), this);

    public final BoolValue cFont = new BoolValue("C Fonts", true, this, () -> elements.isEnabled("Module List"));
    public final ModeValue fontMode = new ModeValue("C Fonts Mode", new String[]{"Bold", "Semi Bold", "Regular", "Tahoma"}, "Regular", this, () -> cFont.canDisplay() && cFont.get());
    public final SliderValue fontSize = new SliderValue("Font Size", 17, 10, 25, this, cFont::get);
    public final ModeValue watemarkMode = new ModeValue("Watermark Mode", new String[]{"Text", "Exhi"}, "Text", this, () -> elements.isEnabled("Watermark"));
    public final ModeValue arrayPosition = new ModeValue("Position", new String[]{"Right", "Left"}, "Right", this, () -> elements.isEnabled("Module List"));
    public final SliderValue x = new SliderValue("Module List X", -5, -50, 50, this, () -> elements.isEnabled("Module List"));
    public final SliderValue y = new SliderValue("Module List Y", 5, -50, 50, this, () -> elements.isEnabled("Module List"));
    public final SliderValue textHeight = new SliderValue("Text Height", 0, 0, 10, this, () -> elements.isEnabled("Module List"));
    public final ModeValue tags = new ModeValue("Suffix", new String[]{"None", "Simple", "Bracket", "Dash"}, "Simple", this, () -> elements.isEnabled("Module List"));
    public final ModeValue outline = new ModeValue("Outline", new String[]{"Right", "Left", "None"}, "None", this, () -> elements.isEnabled("Module List"));
    public final ModeValue armorMode = new ModeValue("Armor Mode", new String[]{"Default"}, "Default", this, () -> elements.isEnabled("Armor"));
    public final ModeValue infoMode = new ModeValue("Info Mode", new String[]{"Exhi"}, "Exhi", this, () -> elements.isEnabled("Info"));
    public final ModeValue potionHudMode = new ModeValue("Potion Mode", new String[]{"Exhi", "Sexy"}, "Sexy", this, () -> elements.isEnabled("Potion HUD"));
    public final ModeValue targetHudMode = new ModeValue("TargetHUD Mode", new String[]{"Moon", "Demise"}, "Demise", this, () -> elements.isEnabled("Target HUD"));
    public final BoolValue targetHudParticle = new BoolValue("TargetHUD Particle", true, this, () -> elements.isEnabled("Target HUD"));
    public final ModeValue notificationMode = new ModeValue("Notification Mode", new String[]{"Default", "Exhi"}, "Default", this, () -> elements.isEnabled("Notification"));
    public final ModeValue sessionInfoMode = new ModeValue("Session Info Mode", new String[]{"Default", "Exhi", "Rise", "Moon"}, "Moon", this, () -> elements.isEnabled("Session Info"));
    public final BoolValue centerNotif = new BoolValue("Center Notification", true, this, () -> notificationMode.is("Exhi"));
    public final ModeValue color = new ModeValue("Color Setting", new String[]{"Custom", "Rainbow", "Dynamic", "Fade", "Astolfo", "NeverLose"}, "Fade", this);
    private final ColorValue mainColor = new ColorValue("Main Color", new Color(255, 255, 255), this, () -> !color.is("NeverLose"));
    private final ColorValue secondColor = new ColorValue("Second Color", new Color(71, 71, 71), this, () -> color.is("Fade"));
    public final SliderValue fadeSpeed = new SliderValue("Fade Speed", 1, 1, 10, 1, this, () -> color.is("Dynamic") || color.is("Fade"));
    public final BoolValue background = new BoolValue("Background", true, this, () -> elements.isEnabled("Module List"));
    public final ModeValue bgColor = new ModeValue("Background Color", new String[]{"Dark", "Synced", "Custom", "NeverLose"}, "Dark", this, background::get);
    private final ColorValue bgCustomColor = new ColorValue("Background Custom Color", new Color(0, 0, 0), this, () -> bgColor.canDisplay() && bgColor.is("Custom"));
    private final SliderValue bgAlpha = new SliderValue("Background Alpha", 100, 1, 255, 1, this);
    public final BoolValue hideScoreboard = new BoolValue("Hide Scoreboard", false, this);
    public final BoolValue hideScoreRed = new BoolValue("Hide Scoreboard Red Points", true, this, () -> !hideScoreboard.get());
    public final BoolValue chatCombine = new BoolValue("Chat Combine", true, this);

    private final DecimalFormat bpsFormat = new DecimalFormat("0.00");
    private final DecimalFormat xyzFormat = new DecimalFormat("0");
    public final Map<EntityPlayer, DecelerateAnimation> animationEntityPlayerMap = new HashMap<>();
    public int lost = 0, killed = 0, won = 0;
    public int prevMatchKilled = 0, matchKilled = 0, match;

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (watemarkMode.canDisplay()) {
            switch (watemarkMode.get()) {
                case "Text":
                    Fonts.interBold.get(30).drawStringWithShadow(clientName.get(), 10, 10, color(0));
                    break;
                case "Exhi":
                    boolean shouldChange = RenderUtils.COLOR_PATTERN.matcher(clientName.get()).find();
                    String text = shouldChange ? "§r" + clientName.get() : clientName.get().charAt(0) + "§r§f" + clientName.get().substring(1) +
                            "§7[§f" + Minecraft.getDebugFPS() + " FPS§7]§r ";
                    mc.fontRendererObj.drawStringWithShadow(text, 2.0f, 2.0f, color());
                    break;
            }
        }

        if (infoMode.canDisplay()) {
            if (infoMode.get().equals("Exhi")) {
                float textY = (event.getScaledResolution().getScaledHeight() - 9) + (mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f);
                mc.fontRendererObj.drawStringWithShadow("XYZ: " + EnumChatFormatting.WHITE +
                                xyzFormat.format(mc.thePlayer.posX) + " " +
                                xyzFormat.format(mc.thePlayer.posY) + " " +
                                xyzFormat.format(mc.thePlayer.posZ) + " " + EnumChatFormatting.RESET + "BPS: " + EnumChatFormatting.WHITE + this.bpsFormat.format(MoveUtil.getBPS())
                        , 2, textY, color(0));
            }
        }

        if (armorMode.canDisplay()) {
            if (armorMode.get().equals("Default")) {
                ArrayList<ItemStack> stuff = new ArrayList<>();
                boolean onWater = mc.thePlayer.isEntityAlive() && mc.thePlayer.isInsideOfMaterial(Material.water);
                int split = -3;
                for (int index = 3; index >= 0; --index) {
                    ItemStack armor = mc.thePlayer.inventory.armorInventory[index];
                    if (armor == null) continue;
                    stuff.add(armor);
                }
                if (mc.thePlayer.getCurrentEquippedItem() != null) {
                    stuff.add(mc.thePlayer.getCurrentEquippedItem());
                }
                for (ItemStack everything : stuff) {
                    split += 16;
                    RenderUtils.renderItemStack(everything, split + (double) event.getScaledResolution().getScaledWidth() / 2 - 4, event.getScaledResolution().getScaledHeight() - (onWater ? 65 : 55) + (mc.thePlayer.capabilities.isCreativeMode ? 14 : 0), 1, true, 0.5f);
                }
            }
        }

        if (elements.isEnabled("Module List")) {
            int count = 1;
            int screenWidth = event.getScaledResolution().getScaledWidth();
            float y = this.y.get();
            Comparator<Module> sort = (m1, m2) -> {
                double ab = cFont.get() ? getFr().getStringWidth(m1.getName() + m1.getTag()) : mc.fontRendererObj.getStringWidth(m1.getName() + m1.getTag());
                double bb = cFont.get() ? getFr().getStringWidth(m2.getName() + m2.getTag()) : mc.fontRendererObj.getStringWidth(m2.getName() + m2.getTag());
                return Double.compare(bb, ab);
            };

            ArrayList<Module> enabledMods = new ArrayList<>(INSTANCE.getModuleManager().getModules());

            enabledMods.sort(sort);
            for (Module module : enabledMods) {
                if (module.isHidden() || module.getCategory() == ModuleCategory.Visual)
                    continue;
                Animation moduleAnimation = module.getAnimation();
                moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
                float moduleWidth = cFont.get() ? getFr().getStringWidth(module.getName() + module.getTag()) : mc.fontRendererObj.getStringWidth(module.getName() + module.getTag());
                float x = (arrayPosition.is("Right") ? screenWidth - moduleWidth - 1.0f : 2) + this.x.get();
                float alphaAnimation = 1.0f;

                x += (float) Math.abs((moduleAnimation.getOutput() - 1.0) * (2.0 + moduleWidth));


                final float leftSide = x - 2f;
                final float bottom = (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();

                if (background.get()) {
                    RenderUtils.drawRect(leftSide, y, moduleWidth + 3, bottom, bgColor(count));
                }

                if (outline.is("Left")) {
                    RenderUtils.drawRect(leftSide - 1, y, 1, bottom, color(count));
                }

                if (outline.is("Right")) {
                    RenderUtils.drawRect(x + moduleWidth, y, 1, bottom, color(count));
                }

                if (cFont.get()) {
                    getFr().drawStringWithShadow(module.getName() + module.getTag(), x - 1, y + 2f, ColorUtils.swapAlpha(color(count), (int) alphaAnimation * 255));
                } else {
                    mc.fontRendererObj.drawStringWithShadow(module.getName() + module.getTag(), x - 1, y + 2f, ColorUtils.swapAlpha(color(count), (int) alphaAnimation * 255));
                }

                y += (float) (moduleAnimation.getOutput() * ((cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get()));
                count -= 2;
            }
        }

        if (elements.isEnabled("Potion HUD") && potionHudMode.is("Exhi")) {
            ArrayList<PotionEffect> potions = new ArrayList<>(mc.thePlayer.getActivePotionEffects());
            potions.sort(Comparator.comparingDouble(effect -> -mc.fontRendererObj.getStringWidth(I18n.format(Potion.potionTypes[effect.getPotionID()].getName()))));
            float y = mc.currentScreen instanceof GuiChat ? -14.0f : -3.0f;
            for (PotionEffect potionEffect : potions) {
                Potion potionType = Potion.potionTypes[potionEffect.getPotionID()];
                String potionName = I18n.format(potionType.getName());
                String type = "";
                if (potionEffect.getAmplifier() == 1) {
                    potionName = potionName + " II";
                } else if (potionEffect.getAmplifier() == 2) {
                    potionName = potionName + " III";
                } else if (potionEffect.getAmplifier() == 3) {
                    potionName = potionName + " IV";
                }
                if (potionEffect.getDuration() < 600 && potionEffect.getDuration() > 300) {
                    type = type + " §6" + Potion.getDurationString(potionEffect);
                } else if (potionEffect.getDuration() < 300) {
                    type = type + " §c" + Potion.getDurationString(potionEffect);
                } else if (potionEffect.getDuration() > 600) {
                    type = type + " §7" + Potion.getDurationString(potionEffect);
                }
                GlStateManager.pushMatrix();
                mc.fontRendererObj.drawString(potionName, (float) event.getScaledResolution().getScaledWidth() - mc.fontRendererObj.getStringWidth(type + potionName) - 2.0f, (event.getScaledResolution().getScaledHeight() - 9) + y, new Color(potionType.getLiquidColor()).getRGB(), true);
                mc.fontRendererObj.drawString(type, (float) event.getScaledResolution().getScaledWidth() - mc.fontRendererObj.getStringWidth(type) - 2.0f, (event.getScaledResolution().getScaledHeight() - 9) + y, new Color(255, 255, 255).getRGB(), true);

                GlStateManager.popMatrix();
                y -= 9.0f;
            }
        }

        if (elements.isEnabled("Session Info") && sessionInfoMode.is("Exhi")) {
            mc.fontRendererObj.drawStringWithShadow(RenderUtils.sessionTime(), event.getScaledResolution().getScaledWidth() / 2.0f - mc.fontRendererObj.getStringWidth(RenderUtils.sessionTime()) / 2.0f, BossStatus.bossName != null && BossStatus.statusBarTime > 0 ? 47 : 30.0f, -1);
        }

        if (elements.isEnabled("Notification")) {
            Demise.INSTANCE.getNotificationManager().publish(new ScaledResolution(mc),false);
        }
    }

    @EventTarget
    public void onShader2D(Shader2DEvent event) {
        if (watemarkMode.get().equals("Text")) {
            if (event.getShaderType() == Shader2DEvent.ShaderType.SHADOW || event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                Fonts.interBold.get(30).drawStringWithShadow(clientName.get(), 10, 10, color(0));
            }
        }

        if (elements.isEnabled("Module List")) {
            int count = 1;
            int screenWidth = new ScaledResolution(mc).getScaledWidth();
            float y = this.y.get();
            Comparator<Module> sort = (m1, m2) -> {
                double ab = cFont.get() ? getFr().getStringWidth(m1.getName() + m1.getTag()) : mc.fontRendererObj.getStringWidth(m1.getName() + m1.getTag());
                double bb = cFont.get() ? getFr().getStringWidth(m2.getName() + m2.getTag()) : mc.fontRendererObj.getStringWidth(m2.getName() + m2.getTag());
                return Double.compare(bb, ab);
            };
            ArrayList<Module> enabledMods = new ArrayList<>(INSTANCE.getModuleManager().getModules());

            enabledMods.sort(sort);
            for (Module module : enabledMods) {
                if (module.isHidden() || module.getCategory() == ModuleCategory.Visual)
                    continue;
                Animation moduleAnimation = module.getAnimation();
                moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
                float moduleWidth = cFont.get() ? getFr().getStringWidth(module.getName() + module.getTag()) : mc.fontRendererObj.getStringWidth(module.getName() + module.getTag());
                float x = (arrayPosition.is("Right") ? screenWidth - moduleWidth - 1.0f : 2) + this.x.get();

                x += (float) Math.abs((moduleAnimation.getOutput() - 1.0) * (2.0 + moduleWidth));

                final float leftSide = x - 2f;
                final float bottom = (cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get();

                if (background.get()) {
                    if (event.getShaderType() == Shader2DEvent.ShaderType.BLUR || event.getShaderType() == Shader2DEvent.ShaderType.SHADOW) {
                        RenderUtils.drawRect(leftSide, y, moduleWidth + 3, bottom, color(count));
                    }

                    if (event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                        RenderUtils.drawRect(leftSide, y, moduleWidth + 3, bottom, bgColor(count));
                    }
                }

                if (event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                    if (outline.is("Left")) {
                        RenderUtils.drawRect(leftSide - 1, y, 1, bottom, color(count));
                    }

                    if (outline.is("Right")) {
                        RenderUtils.drawRect(x + moduleWidth, y, 1, bottom, color(count));
                    }

                }

                if (event.getShaderType() == Shader2DEvent.ShaderType.GLOW) {
                    if (cFont.get()) {
                        getFr().drawStringWithShadow(module.getName() + module.getTag(), x - 1, y + 2f, ColorUtils.swapAlpha(color(count), 255));
                    } else {
                        mc.fontRendererObj.drawStringWithShadow(module.getName() + module.getTag(), x - 1, y + 2f, ColorUtils.swapAlpha(color(count), 255));
                    }
                }

                y += (float) (moduleAnimation.getOutput() * ((cFont.get() ? getFr().getHeight() : mc.fontRendererObj.FONT_HEIGHT) + textHeight.get()));
                count -= 2;
            }
        }

        if (elements.isEnabled("Notification")) {
            Demise.INSTANCE.getNotificationManager().publish(new ScaledResolution(mc),false);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        mainColor.setRainbow(color.is("Rainbow"));
        KillAura aura = getModule(KillAura.class);

        if (aura.isEnabled()) {
            animationEntityPlayerMap.entrySet().removeIf(entry -> KillAura.currentTarget != null && KillAura.currentTarget != entry.getKey());
        }

        if (!aura.isEnabled() && !(mc.currentScreen instanceof GuiChat)) {
            Iterator<Map.Entry<EntityPlayer, DecelerateAnimation>> iterator = animationEntityPlayerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<EntityPlayer, DecelerateAnimation> entry = iterator.next();
                DecelerateAnimation animation = entry.getValue();

                animation.setDirection(Direction.BACKWARDS);
                if (animation.finished(Direction.BACKWARDS)) {
                    iterator.remove();
                }
            }
        }

        if (KillAura.currentTarget instanceof EntityPlayer) {
            if (!(mc.currentScreen instanceof GuiChat)) {
                animationEntityPlayerMap.putIfAbsent((EntityPlayer) KillAura.currentTarget, new DecelerateAnimation(175, 1));
                animationEntityPlayerMap.get(KillAura.currentTarget).setDirection(Direction.FORWARDS);
            }
        }

        if (aura.isEnabled() && KillAura.currentTarget == null && !(mc.currentScreen instanceof GuiChat)) {
            Iterator<Map.Entry<EntityPlayer, DecelerateAnimation>> iterator = animationEntityPlayerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<EntityPlayer, DecelerateAnimation> entry = iterator.next();
                DecelerateAnimation animation = entry.getValue();

                animation.setDirection(Direction.BACKWARDS);
                if (animation.finished(Direction.BACKWARDS)) {
                    iterator.remove();
                }
            }
        }

        if (mc.currentScreen instanceof GuiChat && KillAura.currentTarget == null) {
            animationEntityPlayerMap.putIfAbsent(mc.thePlayer, new DecelerateAnimation(175, 1));
            animationEntityPlayerMap.get(mc.thePlayer).setDirection(Direction.FORWARDS);
        }
    }

    @EventTarget
    public void onWorld(WorldChangeEvent event) {
        prevMatchKilled = matchKilled;
        matchKilled = 0;
        match += 1;

        if (match > 6)
            match = 6;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();

        if (packet instanceof S02PacketChat) {
            S02PacketChat s02 = (S02PacketChat) event.getPacket();
            String xd = s02.getChatComponent().getUnformattedText();
            if (xd.contains("was killed by " + mc.thePlayer.getName())) {
                ++this.killed;
                prevMatchKilled = matchKilled;
                ++matchKilled;
            }

            if (xd.contains("You Died! Want to play again?")) {
                ++lost;
            }
        }

        if (packet instanceof S45PacketTitle && ((S45PacketTitle) packet).getType().equals(S45PacketTitle.Type.TITLE)) {
            String unformattedText = ((S45PacketTitle) packet).getMessage().getUnformattedText();
            if (unformattedText.contains("VICTORY!")) {
                ++this.won;
            }
            if (unformattedText.contains("GAME OVER!") || unformattedText.contains("DEFEAT!") || unformattedText.contains("YOU DIED!")) {
                ++this.lost;
            }
        }
    }

    public FontRenderer getFr() {

        return switch (fontMode.get()) {
            case "Bold" -> Fonts.interBold.get(fontSize.get());
            case "Semi Bold" -> Fonts.interSemiBold.get(fontSize.get());
            case "Regular" -> Fonts.interRegular.get(fontSize.get());
            case "Tahoma" -> Fonts.Tahoma.get(fontSize.get());
            default -> null;
        };
    }

    public Color getMainColor() {
        return mainColor.get();
    }

    public Color getSecondColor() {
        return secondColor.get();
    }

    public int getRainbow(int counter) {
        return Color.HSBtoRGB(getRainbowHSB(counter)[0], getRainbowHSB(counter)[1], getRainbowHSB(counter)[2]);
    }

    public static int astolfoRainbow(final int offset, final float saturation, final float brightness) {
        double currentColor = Math.ceil((double) (System.currentTimeMillis() + offset * 20L)) / 6.0;
        return Color.getHSBColor(((float) ((currentColor %= 360.0) / 360.0) < 0.5) ? (-(float) (currentColor / 360.0)) : ((float) (currentColor / 360.0)), saturation, brightness).getRGB();
    }

    public float[] getRainbowHSB(int counter) {
        final int width = 20;

        double rainbowState = Math.ceil(System.currentTimeMillis() - (long) counter * width) / 8;
        rainbowState %= 360;

        float hue = (float) (rainbowState / 360);
        float saturation = mainColor.getSaturation();
        float brightness = mainColor.getBrightness();

        return new float[]{hue, saturation, brightness};
    }

    public int color() {
        return color(0);
    }

    public int color(int counter, int alpha) {
        int colors = getMainColor().getRGB();
        colors = switch (color.get()) {
            case "Rainbow" -> ColorUtils.swapAlpha(getRainbow(counter), alpha);
            case "Dynamic" ->
                    ColorUtils.swapAlpha(ColorUtils.colorSwitch(getMainColor(), new Color(ColorUtils.darker(getMainColor().getRGB(), 0.25F)), 2000.0F, counter, 75L, fadeSpeed.get()).getRGB(), alpha);
            case "Fade" ->
                    ColorUtils.swapAlpha((ColorUtils.colorSwitch(getMainColor(), getSecondColor(), 2000.0F, counter, 75L, fadeSpeed.get()).getRGB()), alpha);
            case "Astolfo" ->
                    ColorUtils.swapAlpha(astolfoRainbow(counter, mainColor.getSaturation(), mainColor.getBrightness()), alpha);
            case "NeverLose" -> ColorUtils.swapAlpha(iconRGB, alpha);
            default -> colors;
        };
        return colors;
    }

    public int color(int counter) {
        return color(counter, 255);
    }

    public int bgColor(int counter, int alpha) {
        int colors = getMainColor().getRGB();
        colors = switch (bgColor.get()) {
            case "Dark" -> (new Color(21, 21, 21, alpha)).getRGB();
            case "Synced" ->
                    new Color(ColorUtils.applyOpacity(color(counter, alpha), alpha / 255f), true).darker().darker().getRGB();
            case "None" -> new Color(0, 0, 0, 0).getRGB();
            case "Custom" -> ColorUtils.swapAlpha(bgCustomColor.get().getRGB(), alpha);
            case "NeverLose" -> ColorUtils.swapAlpha(NeverLose.bgColor.getRGB(), alpha);
            default -> colors;
        };
        return colors;
    }

    public int bgColor(int counter) {
        return bgColor(counter, (int) bgAlpha.get());
    }

    public int bgColor() {
        return bgColor(0);
    }
}