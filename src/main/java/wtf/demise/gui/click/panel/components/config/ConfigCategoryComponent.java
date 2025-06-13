package wtf.demise.gui.click.panel.components.config;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.panel.PanelGui;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConfigCategoryComponent implements IComponent {
    private float x, y;
    private boolean isHovered, isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;
    private String name = "Configs";
    private final List<ConfigComponent> configs = new ArrayList<>();

    public ConfigCategoryComponent(float x, float y) {
        this.x = x;
        this.y = y;
        this.isSelected = false;
        this.isHovered = false;
        this.interpolatedX = x;

        for (String config : Demise.INSTANCE.getConfigManager().getConfigList()) {
            configs.add(new ConfigComponent(config));
        }
    }

    public void initCategory() {
        configs.forEach(ConfigComponent::initCategory);
    }

    public void initGui() {
        configs.clear();
        for (String config : Demise.INSTANCE.getConfigManager().getConfigList()) {
            configs.add(new ConfigComponent(config));
        }
    }

    public void render(boolean shader) {
        float x = this.x;

        if (isSelected) {
            x += 3;
            float width = Fonts.interRegular.get(18).getStringWidth(name);
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
            Fonts.interRegular.get(18).drawString(name, interpolatedX, y, Color.white.getRGB());
            RenderUtils.drawRect(interpolatedX, y + Fonts.interRegular.get(18).getHeight() - 2.6f, interpolatedLineWidth, 0.5f, Color.white.getRGB());
        }

        if (isSelected) {
            handleScroll();

            float componentStartY = PanelGui.posY + 17 + Fonts.urbanist.get(35).getHeight();
            float viewHeight = 250;

            float totalHeight = 0;

            for (ConfigComponent ignored : configs) {
                totalHeight += 40;
            }

            maxScroll = Math.max(0, totalHeight - viewHeight);
            scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.1f);

            RenderUtils.scissor(0, componentStartY - 2, PanelGui.posX + 450, viewHeight);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            float componentOffsetY = componentStartY;
            for (ConfigComponent config : configs) {
                float moduleY = componentOffsetY - scrollOffset;
                config.setX(this.x + 60);
                config.setY(moduleY);
                config.render(shader);
                config.setVisible(moduleY + 35 >= componentStartY && moduleY <= componentStartY + viewHeight);

                componentOffsetY += 35;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        configs.forEach(moduleComponent -> moduleComponent.drawScreen(mouseX, mouseY));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        configs.forEach(moduleComponent -> moduleComponent.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        configs.forEach(moduleComponent -> moduleComponent.keyTyped(typedChar, keyCode));
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        configs.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, state));
    }

    public void handleScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            float scrollAmount = wheel > 0 ? -25 : 25;
            targetScrollOffset = MathHelper.clamp_float(targetScrollOffset + scrollAmount, 0, maxScroll);
        }
    }
}