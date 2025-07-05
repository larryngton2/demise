package wtf.demise.gui.click;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.click.components.Category;
import wtf.demise.gui.click.components.SearchCategory;
import wtf.demise.gui.click.components.config.ConfigCategoryComponent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PanelGui extends GuiScreen {
    private final List<Category> categories = new ArrayList<>();
    public static Category selectedCategory;
    // I cant set selectedCategory to be a ConfigCategoryComponent, so this is a workaround for that
    public static ConfigCategoryComponent selectedConfigCategory;
    public static SearchCategory selectedSearchCategory;
    public static boolean dragging;
    private float dragX, dragY;
    public static float posX = 255, posY = 120;
    private final ConfigCategoryComponent configCategoryComponent;
    private final SearchCategory searchCategoryComponent;
    public static float interpolatedScale;
    private boolean closing;

    public PanelGui() {
        Demise.INSTANCE.getEventManager().unregister(this);
        Demise.INSTANCE.getEventManager().register(this);
        float height = 15 + Fonts.urbanist.get(35).getHeight();

        for (ModuleCategory category : ModuleCategory.values()) {
            categories.add(new Category(category, posX + 7, posY + height));
            height += Fonts.interRegular.get(18).getHeight() + 7;
        }

        configCategoryComponent = new ConfigCategoryComponent(posX + 7, posY + height);
        searchCategoryComponent = new SearchCategory();

        if (selectedCategory == null) {
            selectedCategory = categories.get(0);
        }
    }

    @Override
    public void initGui() {
        closing = false;
        interpolatedScale = 0;

        if (selectedConfigCategory != null) {
            selectedConfigCategory.initGui();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        interpolatedScale = MathUtils.interpolate(interpolatedScale, !closing ? 1 : 0, 0.25f);

        if (interpolatedScale < 0.01f && closing) {
            mc.displayGuiScreen(null);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        RenderUtils.scaleStart(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f, interpolatedScale);

        if (dragging) {
            float deltaX = mouseX - (dragX + posX);
            float deltaY = mouseY - (dragY + posY);
            posX = mouseX - dragX;
            posY = mouseY - dragY;

            for (Category category : categories) {
                category.setX(category.getX() + deltaX);
                category.setY(category.getY() + deltaY);
            }

            configCategoryComponent.setX(configCategoryComponent.getX() + deltaX);
            configCategoryComponent.setY(configCategoryComponent.getY() + deltaY);
        }

        boolean skipped = true;

        for (Category category : categories) {
            boolean hovered = MouseUtils.isHovered(category.getX(), category.getY(), Fonts.interRegular.get(18).getStringWidth(category.getCategory().getName()), Fonts.interRegular.get(18).getHeight(), mouseX, mouseY);

            if (hovered && Mouse.isButtonDown(0)) {
                if (selectedCategory != category) {
                    category.initCategory();
                }

                selectedCategory = category;
                selectedConfigCategory = null;
                selectedSearchCategory = null;

                skipped = false;
            }

            category.setHovered(hovered);
            category.setSelected(selectedCategory != null && selectedCategory == category);
        }

        boolean skipped1 = true;

        if (skipped) {
            boolean hovered = MouseUtils.isHovered(configCategoryComponent.getX(), configCategoryComponent.getY(), Fonts.interRegular.get(18).getStringWidth("Configs"), Fonts.interRegular.get(18).getHeight(), mouseX, mouseY);

            if (hovered && Mouse.isButtonDown(0)) {
                if (selectedConfigCategory == null) {
                    configCategoryComponent.initCategory();
                }

                selectedConfigCategory = configCategoryComponent;
                selectedCategory = null;
                selectedSearchCategory = null;
                skipped1 = false;
            }

            configCategoryComponent.setHovered(hovered);
            configCategoryComponent.setSelected(selectedConfigCategory != null);
        }

        RoundedUtils.drawRound(posX, posY, 450, 300, 7, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true));

        float x = posX + 7;
        float y = posY + 7;

        Fonts.urbanist.get(35).drawString(Demise.INSTANCE.getClientName(), x, y, new Color(255, 255, 255, 208).getRGB());
        Fonts.urbanist.get(24).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + x, Fonts.urbanist.get(35).getHeight() + y - Fonts.urbanist.get(24).getHeight() * 1.1f, new Color(245, 245, 245, 208).getRGB());

        float watermarkWidth = Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + Fonts.urbanist.get(24).getStringWidth(Demise.INSTANCE.getVersion());
        float calcWidth = 450 - watermarkWidth - 19;

        RoundedUtils.drawRound(posX + watermarkWidth + 13, posY + 7, calcWidth, 20, 7, new Color(0, 0, 0, 100));

        boolean searchHovered = MouseUtils.isHovered(posX + watermarkWidth + 13, posY + 7, calcWidth, 20, mouseX, mouseY);
        if (searchHovered && Mouse.isButtonDown(0) && skipped1) {
            if (selectedSearchCategory == null) {
                searchCategoryComponent.initCategory();
            }

            selectedSearchCategory = searchCategoryComponent;
            selectedCategory = null;
            selectedConfigCategory = null;
        }

        searchCategoryComponent.setSelected(selectedSearchCategory != null);

        if (selectedSearchCategory == null) {
            Fonts.interRegular.get(18).drawString("Search...", posX + watermarkWidth + 18, posY + 7 + Fonts.interRegular.get(15).getHeight() - 2, new Color(147, 147, 147, 255).getRGB());
        }

        configCategoryComponent.render(false);
        if (selectedConfigCategory != null) {
            selectedConfigCategory.drawScreen(mouseX, mouseY);
        }

        searchCategoryComponent.render(false);
        if (selectedSearchCategory != null) {
            selectedSearchCategory.drawScreen(mouseX, mouseY);
        }

        categories.forEach(category -> category.render(false));
        if (selectedCategory != null) {
            selectedCategory.drawScreen(mouseX, mouseY);
        }

        String str = "Total modules: " + Demise.INSTANCE.getModuleManager().getAllModules().size() + ", Enabled: " + Demise.INSTANCE.getModuleManager().getEnabledModules().size();

        Fonts.interRegular.get(14).drawString(str, posX + 450 - Fonts.interRegular.get(14).getStringWidth(str) - 4, posY + 300 - Fonts.interRegular.get(14).getHeight(), new Color(255, 255, 255, 208).getRGB());
        Fonts.interRegular.get(14).drawString(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), posX + 3.5, posY + 300 - Fonts.interRegular.get(14).getHeight(), new Color(255, 255, 255, 208).getRGB());

        RenderUtils.scaleEnd();
    }

    @EventPriority(100)
    @EventTarget
    public void onShader2D(ShaderEvent e) {
        if (mc.currentScreen != this) return;

        ScaledResolution sr = new ScaledResolution(mc);
        RenderUtils.scaleStart(sr.getScaledWidth() / 2f, sr.getScaledHeight() / 2f, interpolatedScale);
        if (e.getShaderType() != ShaderEvent.ShaderType.GLOW) {
            RoundedUtils.drawShaderRound(posX, posY, 450, 300, 7, Color.black);
        } else {
            RoundedUtils.drawGradientPreset(posX, posY, 450, 300, 7);
        }
        categories.forEach(category -> category.render(true));
        configCategoryComponent.render(true);
        searchCategoryComponent.render(true);
        RenderUtils.scaleEnd();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && MouseUtils.isHovered(posX, posY, 450, 35, mouseX, mouseY)) {
            dragging = true;
            dragX = mouseX - posX;
            dragY = mouseY - posY;
        }

        if (selectedSearchCategory != null) {
            selectedSearchCategory.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (selectedConfigCategory != null) {
            selectedConfigCategory.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (selectedCategory != null) {
            selectedCategory.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;

        if (selectedSearchCategory != null) {
            selectedSearchCategory.mouseReleased(mouseX, mouseY, state);
            return;
        }
        if (selectedConfigCategory != null) {
            selectedConfigCategory.mouseReleased(mouseX, mouseY, state);
            return;
        }
        if (selectedCategory != null) {
            selectedCategory.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RSHIFT) {
            closing = !closing;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            closing = true;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            selectedCategory = categories.get((categories.indexOf(selectedCategory) + 1) % categories.size());
        }

        if (closing) {
            return;
        }

        if (selectedSearchCategory != null) {
            selectedSearchCategory.keyTyped(typedChar, keyCode);
            return;
        }
        if (selectedConfigCategory != null) {
            selectedConfigCategory.keyTyped(typedChar, keyCode);
            return;
        }
        if (selectedCategory != null) {
            selectedCategory.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}