package wtf.demise.features.modules.impl.visual;

import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.font.FontRenderer;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.DecelerateAnimation;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.RenderUtils;

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
            new BoolValue("Notification", true),
            new BoolValue("BPS counter", true),
            new BoolValue("Keystrokes", true),
            new BoolValue("Info", true)
    ), this);

    public final SliderValue textHeight = new SliderValue("Text Height", 0, 0, 10, this, () -> elements.isEnabled("Module List"));
    public final BoolValue hideRender = new BoolValue("Hide render", true, this, () -> elements.isEnabled("Module List"));
    public final BoolValue background = new BoolValue("Background", true, this, () -> elements.isEnabled("Module List"));
    public final BoolValue advancedBPS = new BoolValue("Advanced BPS", true, this, () -> elements.isEnabled("BPS counter"));
    public final BoolValue targetHUDTracking = new BoolValue("TargetHUD tracking", false, this, () -> elements.isEnabled("Target HUD"));
    private final ModeValue color = new ModeValue("Color Setting", new String[]{"Custom", "Rainbow", "Dynamic", "Fade", "Astolfo"}, "Fade", this);
    private final ColorValue mainColor = new ColorValue("Main Color", new Color(255, 255, 255), this);
    private final ColorValue secondColor = new ColorValue("Second Color", new Color(71, 71, 71), this, () -> color.is("Fade"));
    private final SliderValue fadeSpeed = new SliderValue("Fade Speed", 1, 1, 10, 1, this, () -> color.is("Dynamic") || color.is("Fade"));
    private final ModeValue bgStyle = new ModeValue("Background style", new String[]{"Transparent", "Opaque", "Custom"}, "Transparent", this, () -> elements.isEnabled("Module List"));
    private final ColorValue bgColor = new ColorValue("Background Color", new Color(21, 21, 21), this, () -> bgStyle.is("Custom"));
    public final SliderValue bgAlpha = new SliderValue("Background Alpha", 204, 1, 255, 1, this, () -> bgStyle.is("Custom"));
    public final BoolValue chatCombine = new BoolValue("Chat Combine", true, this);
    public final BoolValue funy = new BoolValue("funy", false, this);

    public final DecelerateAnimation decelerateAnimation = new DecelerateAnimation(175, 1);
    public EntityLivingBase target;
    private ScaledResolution sr;

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        sr = event.scaledResolution();

        if (elements.isEnabled("Watermark")) {
            Fonts.urbanist.get(38).drawString(Demise.INSTANCE.getClientName(), 10, 10, new Color(255, 255, 255, 208).getRGB());
            Fonts.urbanist.get(27).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(38).getStringWidth(Demise.INSTANCE.getClientName()) + 11.5f, Fonts.urbanist.get(38).getHeight() + 10 - Fonts.urbanist.get(27).getHeight() * 1.1f, new Color(245, 245, 245, 208).getRGB());
        }

        if (elements.isEnabled("Armor")) {
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

        if (elements.isEnabled("Notification")) {
            Demise.INSTANCE.getNotificationManager().publish(false);
        }
    }

    @EventTarget
    public void onShader2D(Shader2DEvent event) {
        if (elements.isEnabled("Watermark")) {
            Fonts.urbanist.get(38).drawString(Demise.INSTANCE.getClientName(), 10, 10, Color.black.getRGB());
            Fonts.urbanist.get(27).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(38).getStringWidth(Demise.INSTANCE.getClientName()) + 11.5f, Fonts.urbanist.get(38).getHeight() + 10 - Fonts.urbanist.get(27).getHeight() * 1.1f, Color.black.getRGB());
        }

        if (elements.isEnabled("Armor")) {
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

                RenderUtils.renderItemStack(everything, split + (double) sr.getScaledWidth() / 2 - 4, sr.getScaledHeight() - (onWater ? 67 : 57) + (mc.thePlayer.capabilities.isCreativeMode ? 10 : 0), 1, true, 0.5f);
            }
        }

        if (elements.isEnabled("Notification")) {
            Demise.INSTANCE.getNotificationManager().publish(true);
        }
    }

    @EventTarget
    public void onTick(TickEvent e) {
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
        return switch (bgStyle.get()) {
            case "Transparent" -> new Color(0, 0, 0, 120).getRGB();
            case "Opaque" -> new Color(21, 21, 21, 204).getRGB();
            case "Custom" -> new Color(bgColor.get().getRGB(), true).getRGB();
            default -> -1;
        };
    }
}