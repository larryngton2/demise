package wtf.demise.features.modules.impl.visual;

import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.font.FontRenderer;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

@ModuleInfo(name = "Interface", category = ModuleCategory.Visual)
public class Interface extends Module {
    public final MultiBoolValue elements = new MultiBoolValue("Elements", Arrays.asList(
            new BoolValue("Watermark", true),
            new BoolValue("Module list", true),
            new BoolValue("Armor", true),
            new BoolValue("Potion HUD", true),
            new BoolValue("Notification", true),
            new BoolValue("Keystrokes", true),
            new BoolValue("Info", true),
            new BoolValue("Motion graph", false)
    ), this);

    public final ModeValue watermarkMode = new ModeValue("Watermark mode", new String[]{"Text", "Image"}, "Text", this, () -> elements.isEnabled("Watermark"));
    public final BoolValue hideRender = new BoolValue("Hide render", true, this, () -> elements.isEnabled("Module list"));
    private final ModeValue colorMode = new ModeValue("Color mode", new String[]{"Winter", "Blend", "Mango", "Snowy sky", "Miko", "Satin", "Gothic"}, "Winter", this);
    private final SliderValue fadeSpeed = new SliderValue("Fade speed", 1, 1, 10, 1, this);
    public final ModeValue bgStyle = new ModeValue("Background style", new String[]{"Transparent", "Opaque", "Custom"}, "Transparent", this, () -> elements.isEnabled("Module List"));
    public final ColorValue bgColor = new ColorValue("Background color", new Color(21, 21, 21), this, () -> bgStyle.is("Custom"));
    public final SliderValue bgAlpha = new SliderValue("Background alpha", 204, 1, 255, 1, this, () -> bgStyle.is("Custom"));
    public final BoolValue chatCombine = new BoolValue("Chat combine", true, this);
    public final BoolValue funy = new BoolValue("funy", false, this);

    private ScaledResolution sr;

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        sr = event.scaledResolution();

        if (elements.isEnabled("Watermark")) {
            switch (watermarkMode.get()) {
                case "Text":
                    Fonts.urbanist.get(38).drawString(Demise.INSTANCE.getClientName(), 10, 10, new Color(255, 255, 255, 208).getRGB());
                    Fonts.urbanist.get(27).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(38).getStringWidth(Demise.INSTANCE.getClientName()) + 11.5f, Fonts.urbanist.get(38).getHeight() + 10 - Fonts.urbanist.get(27).getHeight() * 1.1f, new Color(245, 245, 245, 208).getRGB());
                    break;
                case "Image":
                    RenderUtils.drawImage(new ResourceLocation("demise/img/BAlogo/BA-base.png"), 0, 0, (int) (424 * 0.3f), (int) (250 * 0.3f));
                    RenderUtils.drawImage(new ResourceLocation("demise/img/BAlogo/BA-colored.png"), 0, 0, (int) (424 * 0.3f), (int) (250 * 0.3f), color());
                    break;
            }
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
    public void onShader2D(Shader2DEvent e) {
        if (elements.isEnabled("Watermark")) {
            switch (watermarkMode.get()) {
                case "Text":
                    Fonts.urbanist.get(38).drawString(Demise.INSTANCE.getClientName(), 10, 10, Color.black.getRGB());
                    Fonts.urbanist.get(27).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(38).getStringWidth(Demise.INSTANCE.getClientName()) + 11.5f, Fonts.urbanist.get(38).getHeight() + 10 - Fonts.urbanist.get(27).getHeight() * 1.1f, Color.black.getRGB());
                    break;
                case "Image":
                    RenderUtils.drawImage(new ResourceLocation("demise/img/BAlogo/BA-base.png"), 0, 0, (int) (424 * 0.3f), (int) (250 * 0.3f), Color.black.getRGB());
                    RenderUtils.drawImage(new ResourceLocation("demise/img/BAlogo/BA-colored.png"), 0, 0, (int) (424 * 0.3f), (int) (250 * 0.3f), Color.black.getRGB());
                    break;
            }
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

                RenderUtils.renderItemStack(everything, split + (double) sr.getScaledWidth() / 2 - 4, sr.getScaledHeight() - (onWater ? 67 : 57) + (mc.thePlayer.capabilities.isCreativeMode ? 10 : 0), 1, false, 0.5f);
            }
        }

        if (elements.isEnabled("Notification")) {
            Demise.INSTANCE.getNotificationManager().publish(true);
        }
    }

    public FontRenderer getFr() {
        return Fonts.interRegular.get(15);
    }

    public int color() {
        return color(0);
    }

    public int color(int counter, int alpha) {
        Color color1;
        Color color2;

        color2 = switch (colorMode.get()) {
            case "Blend" -> {
                color1 = new Color(71, 148, 253);
                yield new Color(71, 253, 160);
            }
            case "Mango" -> {
                color1 = new Color(255, 200, 75);
                yield new Color(255, 83, 73);
            }
            case "Snowy sky" -> {
                color1 = new Color(1, 171, 179);
                yield new Color(234, 234, 234);
            }
            // ofc I had to add her
            // and ik that Miko is a real name
            case "Miko" -> {
                color1 = new Color(255, 100, 100);
                yield new Color(255, 255, 255);
            }
            case "Satin" -> {
                color1 = new Color(215, 60, 67);
                yield new Color(140, 23, 39);
            }
            case "Gothic" -> {
                color1 = new Color(31, 30, 30);
                yield new Color(196, 190, 190);
            }
            default -> {
                color1 = Color.white;
                yield Color.white;
            }
        };

        return ColorUtils.swapAlpha((ColorUtils.colorSwitch(color1, color2, 2000.0F, counter, 75L, fadeSpeed.get()).getRGB()), alpha);
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