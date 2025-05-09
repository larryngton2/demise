package wtf.demise.features.modules.impl.visual;

import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;
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
import wtf.demise.gui.font.FontRenderer;
import wtf.demise.gui.font.Fonts;
import wtf.demise.userinfo.CurrentUser;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.DecelerateAnimation;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.*;

@ModuleInfo(name = "Interface", category = ModuleCategory.Visual)
public class Interface extends Module {
    public final MultiBoolValue elements = new MultiBoolValue("Elements", Arrays.asList(
            new BoolValue("Watermark", true),
            new BoolValue("Module List", true),
            new BoolValue("Armor", true),
            new BoolValue("Potion HUD", true),
            new BoolValue("Target HUD", true),
            new BoolValue("Notification", false),
            new BoolValue("BPS counter", false)
    ), this);

    public final ModeValue watemarkMode = new ModeValue("Watermark Mode", new String[]{"Text", "Modern"}, "Modern", this, () -> elements.isEnabled("Watermark"));
    public final SliderValue textHeight = new SliderValue("Text Height", 0, 0, 10, this, () -> elements.isEnabled("Module List"));
    public final ModeValue tags = new ModeValue("Suffix", new String[]{"None", "Simple", "Bracket", "Dash"}, "Simple", this, () -> elements.isEnabled("Module List"));
    public final BoolValue hideRender = new BoolValue("Hide render", true, this, () -> elements.isEnabled("Module List"));
    public final ModeValue armorMode = new ModeValue("Armor Mode", new String[]{"Default"}, "Default", this, () -> elements.isEnabled("Armor"));
    public final BoolValue advancedBPS = new BoolValue("Advanced BPS", true, this, () -> elements.isEnabled("BPS counter"));
    public final ModeValue color = new ModeValue("Color Setting", new String[]{"Custom", "Rainbow", "Dynamic", "Fade", "Astolfo"}, "Fade", this);
    private final ColorValue mainColor = new ColorValue("Main Color", new Color(255, 255, 255), this);
    private final ColorValue secondColor = new ColorValue("Second Color", new Color(71, 71, 71), this, () -> color.is("Fade"));
    public final SliderValue fadeSpeed = new SliderValue("Fade Speed", 1, 1, 10, 1, this, () -> color.is("Dynamic") || color.is("Fade"));
    public final BoolValue background = new BoolValue("Background", true, this, () -> elements.isEnabled("Module List"));
    public final SliderValue bgAlpha = new SliderValue("Background Alpha", 100, 1, 255, 1, this);
    public final BoolValue chatCombine = new BoolValue("Chat Combine", true, this);
    public final BoolValue healthFix = new BoolValue("Health fix", true, this);

    public final DecelerateAnimation decelerateAnimation = new DecelerateAnimation(175, 1);
    public EntityLivingBase target;
    public int lost = 0, killed = 0, won = 0;
    public int prevMatchKilled = 0, matchKilled = 0, match;

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (watemarkMode.canDisplay()) {
            switch (watemarkMode.get()) {
                case "Text":
                    Fonts.urbanist.get(38).drawString(Demise.INSTANCE.getClientName(), 10, 10, new Color(255, 255, 255, 208).getRGB());
                    Fonts.urbanist.get(27).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(38).getStringWidth(Demise.INSTANCE.getClientName()) + 11.5f, Fonts.urbanist.get(38).getHeight() + 10 - Fonts.urbanist.get(27).getHeight() * 1.1f, new Color(245, 245, 245, 208).getRGB());
                    break;
                case "Modern":
                    String name = Demise.INSTANCE.getClientName().toLowerCase() + EnumChatFormatting.WHITE +
                            " | " + CurrentUser.FINAL_USER +
                            " | " + PlayerUtils.getCurrServer();

                    int x = 7;
                    int y = 7;
                    int width = Fonts.interSemiBold.get(17).getStringWidth("") + Fonts.interSemiBold.get(17).getStringWidth(name) + 5;
                    int height = Fonts.interSemiBold.get(17).getHeight() + 3;

                    RoundedUtils.drawRound(x, y, width, height, 4, new Color(getModule(Interface.class).bgColor(), true));
                    Fonts.interSemiBold.get(17).drawStringWithShadow(name, Fonts.interBold.get(17).getStringWidth("") + x + 2, y + 4.5f, new Color(color(1)).getRGB());
                    break;
            }
        }

        if (armorMode.canDisplay()) {
            if (armorMode.is("Default")) {
                ArrayList<ItemStack> stuff = new ArrayList<>();
                boolean onWater = mc.thePlayer.isEntityAlive() && mc.thePlayer.isInsideOfMaterial(Material.water);
                int split = -3;
                for (int index = 3; index >= 0; --index) {
                    ItemStack armor = mc.thePlayer.inventory.armorInventory[index];
                    if (armor == null) continue;
                    stuff.add(armor);
                }

                if (SpoofSlotUtils.getSpoofedStack() != null) {
                    stuff.add(SpoofSlotUtils.getSpoofedStack());
                }

                for (ItemStack everything : stuff) {
                    split += 16;

                    RenderUtils.renderItemStack(everything, split + (double) event.scaledResolution().getScaledWidth() / 2 - 4, event.scaledResolution().getScaledHeight() - (onWater ? 67 : 57) + (mc.thePlayer.capabilities.isCreativeMode ? 10 : 0), 1, true, 0.5f);
                }
            }
        }

        if (elements.isEnabled("Notification")) {
            Demise.INSTANCE.getNotificationManager().publish(new ScaledResolution(mc), false);
        }
    }

    @EventTarget
    public void onShader2D(Shader2DEvent event) {
        if (elements.isEnabled("Watermark")) {
            switch (watemarkMode.get()) {
                case "Text":
                    Fonts.urbanist.get(38).drawString(Demise.INSTANCE.getClientName(), 10, 10, Color.black.getRGB());
                    Fonts.urbanist.get(27).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(38).getStringWidth(Demise.INSTANCE.getClientName()) + 11.5f, Fonts.urbanist.get(38).getHeight() + 10 - Fonts.urbanist.get(27).getHeight() * 1.1f, Color.black.getRGB());
                    break;
                case "Modern":
                    String name = Demise.INSTANCE.getClientName().toLowerCase() + EnumChatFormatting.WHITE +
                            " | " + CurrentUser.FINAL_USER +
                            " | " + PlayerUtils.getCurrServer();

                    int x = 7;
                    int y = 7;
                    int width = Fonts.interSemiBold.get(17).getStringWidth("") + Fonts.interSemiBold.get(17).getStringWidth(name) + 5;
                    int height = Fonts.interSemiBold.get(17).getHeight() + 3;

                    RoundedUtils.drawShaderRound(x, y, width, height, 4, Color.black);
                    break;
            }
        }

        if (elements.isEnabled("Notification")) {
            Demise.INSTANCE.getNotificationManager().publish(new ScaledResolution(mc), false);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        mainColor.setRainbow(color.is("Rainbow"));
        KillAura aura = getModule(KillAura.class);

        if (!(mc.currentScreen instanceof GuiChat)) {
            if (aura.isEnabled()) {
                if (KillAura.currentTarget instanceof EntityPlayer) {
                    decelerateAnimation.setDirection(Direction.FORWARDS);
                    target = KillAura.currentTarget;
                }
            }

            if (!aura.isEnabled() || !(KillAura.currentTarget instanceof EntityPlayer)) {
                decelerateAnimation.setDirection(Direction.BACKWARDS);
                if (decelerateAnimation.finished(Direction.BACKWARDS)) {
                    target = null;
                }
            }
        } else if (target == null) {
            decelerateAnimation.setDirection(Direction.FORWARDS);
            target = mc.thePlayer;
        }
    }

    @EventTarget
    public void onWorld(WorldChangeEvent event) {
        prevMatchKilled = matchKilled;
        matchKilled = 0;
        match += 1;

        if (match > 6) match = 6;
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
        return Fonts.interRegular.get(15);
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
            default -> colors;
        };
        return colors;
    }

    public int color(int counter) {
        return color(counter, 255);
    }

    public int bgColor() {
        return new Color(0, 0, 0, (int) bgAlpha.get()).getRGB();
    }
}