package wtf.demise.gui.click.panel.components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.panel.PanelGui;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.List;

@Getter
@Setter
public class Category implements IComponent {
    private ModuleCategory category;
    private float x, y;
    private boolean isHovered, isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private final List<ModuleComponent> moduleComponents = new java.util.ArrayList<>();
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;

    public Category(ModuleCategory category, float x, float y) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.isSelected = false;
        this.isHovered = false;
        this.interpolatedX = x;

        for (Module module : Demise.INSTANCE.getModuleManager().getModulesByCategory(category)) {
            moduleComponents.add(new ModuleComponent(module));
        }
    }

    public void render(boolean shader) {
        float x = this.x;

        if (isSelected) {
            x += 3;
            float width = Fonts.interRegular.get(18).getStringWidth(category.getName());
            interpolatedLineWidth = MathUtils.interpolate(interpolatedLineWidth, width, 0.05f);
        } else {
            interpolatedLineWidth = MathUtils.interpolate(interpolatedLineWidth, 0, 0.05f);
        }

        if (isHovered) {
            x += 2.5f;
        }

        if (!PanelGui.dragging) {
            interpolatedX = MathUtils.interpolate(interpolatedX, x, 0.15f);
        } else {
            interpolatedX = x;
        }

        if (!shader) {
            Fonts.interRegular.get(18).drawString(category.getName(), interpolatedX, y, Color.white.getRGB());
            RenderUtils.drawRect(interpolatedX, y + Fonts.interRegular.get(18).getHeight() - 2.6f, interpolatedLineWidth, 0.5f, Color.white.getRGB());
        }

        if (isSelected) {
            handleScroll();

            float componentStartY = PanelGui.posY + 17 + Fonts.urbanist.get(35).getHeight();
            float viewHeight = 265;

            float totalHeight = 0;

            for (ModuleComponent module : moduleComponents) {
                totalHeight += module.getHeight() + 10;
            }

            maxScroll = Math.max(0, totalHeight - viewHeight);
            scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.1f);

            RenderUtils.prepareScissorBox(0, (int) componentStartY - 2, PanelGui.width, (int) viewHeight);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            float componentOffsetY = componentStartY;
            for (ModuleComponent module : moduleComponents) {
                float moduleY = componentOffsetY - scrollOffset;
                module.setX(this.x + 60);
                module.setY(moduleY);
                module.render(shader);
                module.setVisible(moduleY + 40 >= componentStartY && moduleY <= componentStartY + viewHeight);

                componentOffsetY += module.getHeight() + 10;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.drawScreen(mouseX, mouseY));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.keyTyped(typedChar, keyCode));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, state));
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25 : 25;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0, maxScroll);
        }
    }
}