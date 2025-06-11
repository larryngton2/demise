package wtf.demise.gui.click.panel;

import net.minecraft.client.gui.GuiScreen;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.ModuleCategory;

import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.click.panel.components.Category;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PanelGui extends GuiScreen {
    private final List<Category> categories = new ArrayList<>();
    public static Category selectedCategory;
    public static boolean dragging;
    private float dragX, dragY;
    public static float posX = 255, posY = 120;

    public PanelGui() {
        Demise.INSTANCE.getEventManager().unregister(this);
        Demise.INSTANCE.getEventManager().register(this);

        float height = 15 + Fonts.urbanist.get(35).getHeight();

        for (ModuleCategory category : ModuleCategory.values()) {
            categories.add(new Category(category, posX + 7, posY + height));

            height += Fonts.interRegular.get(18).getHeight() + 7;
        }

        if (selectedCategory == null) {
            selectedCategory = categories.get(0);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (dragging) {
            float deltaX = mouseX - (dragX + posX);
            float deltaY = mouseY - (dragY + posY);
            posX = mouseX - dragX;
            posY = mouseY - dragY;

            for (Category category : categories) {
                category.setX(category.getX() + deltaX);
                category.setY(category.getY() + deltaY);
            }
        }

        for (Category category : categories) {
            boolean hovered = MouseUtils.isHovered(category.getX(), category.getY(), Fonts.interRegular.get(18).getStringWidth(category.getCategory().getName()), Fonts.interRegular.get(18).getHeight(), mouseX, mouseY);

            if (hovered && Mouse.isButtonDown(0)) {
                selectedCategory = category;
            }

            category.setHovered(hovered);
            category.setSelected(selectedCategory == category);
        }

        selectedCategory.drawScreen(mouseX, mouseY);
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (mc.currentScreen != this) return;

        RoundedUtils.drawRound(posX, posY, 450, 300, 7, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true));

        float x = posX + 7;
        float y = posY + 7;

        Fonts.urbanist.get(35).drawString(Demise.INSTANCE.getClientName(), x, y, new Color(255, 255, 255, 208).getRGB());
        Fonts.urbanist.get(24).drawString(Demise.INSTANCE.getVersion(), Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + x, Fonts.urbanist.get(35).getHeight() + y - Fonts.urbanist.get(24).getHeight() * 1.1f, new Color(245, 245, 245, 208).getRGB());

        categories.forEach(category -> category.render(false));
    }

    @EventTarget
    public void onShader2D(Shader2DEvent e) {
        if (mc.currentScreen != this) return;

        RoundedUtils.drawShaderRound(posX, posY, 450, 300, 7, Color.black);

        categories.forEach(category -> category.render(true));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && MouseUtils.isHovered(posX, posY, 450, 35, mouseX, mouseY)) {
            dragging = true;
            dragX = mouseX - posX;
            dragY = mouseY - posY;
        }

        selectedCategory.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;

        selectedCategory.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        selectedCategory.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}