package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static wtf.demise.gui.click.PanelGui.posX;
import static wtf.demise.gui.click.PanelGui.posY;

@Getter
@Setter
public class SearchCategory implements IComponent {
    private ModuleCategory category;
    private boolean isSelected;
    private float interpolatedX;
    private float interpolatedLineWidth;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private float scrollOffset = 0;
    private float targetScrollOffset = 0;
    private float maxScroll = 0;
    private String filter = "";
    private boolean inputting;

    public SearchCategory() {
        this.isSelected = false;
    }

    public void initCategory() {
        moduleComponents.clear();
        for (Module module : Demise.INSTANCE.getModuleManager().getModules()) {
            if (filter.isEmpty() || module.getName().toLowerCase().contains(filter.toLowerCase())) {
                moduleComponents.add(new ModuleComponent(module));
            }
        }

        moduleComponents.forEach(ModuleComponent::initCategory);
    }

    public void render(boolean shader) {
        if (isSelected) {
            String thing = (System.currentTimeMillis() % 1000 > 500 ? "|" : "");

            if (inputting) {
                if (!filter.isEmpty()) {
                    drawText(filter + thing);
                } else {
                    drawText("Search..." + thing);
                }
            } else {
                drawText("Search...");
            }

            handleScroll();

            float componentStartY = posY + 12 + Fonts.urbanist.get(35).getHeight();
            float viewHeight = 255;

            float totalHeight = 0;

            for (ModuleComponent module : moduleComponents) {
                totalHeight += module.getHeight() + 10;
            }

            maxScroll = Math.max(0, totalHeight - viewHeight);
            scrollOffset = MathUtils.interpolate(scrollOffset, targetScrollOffset, 0.1f);

            RenderUtils.scissor(0, componentStartY, posX + 450, viewHeight, PanelGui.interpolatedScale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            float componentOffsetY = componentStartY + 2;
            for (ModuleComponent module : moduleComponents) {
                float moduleY = componentOffsetY - scrollOffset;
                module.setX(posX + 67);
                module.setY(moduleY);
                module.render(shader);
                module.setVisible(moduleY + 35 >= componentStartY && moduleY <= componentStartY + viewHeight);
                module.setVisibleSetting(moduleY + module.getHeight() >= componentStartY && moduleY <= componentStartY + viewHeight);

                componentOffsetY += module.getHeight() + 10;
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void drawText(String text) {
        float watermarkWidth = Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + Fonts.urbanist.get(24).getStringWidth(Demise.INSTANCE.getVersion());
        int color = inputting ? new Color(193, 193, 193).getRGB() : new Color(119, 119, 119, 255).getRGB();
        Fonts.interRegular.get(18).drawString(text, posX + watermarkWidth + 18, posY + 7 + Fonts.interRegular.get(15).getHeight() - 2, color);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.drawScreen(mouseX, mouseY));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float watermarkWidth = Fonts.urbanist.get(35).getStringWidth(Demise.INSTANCE.getClientName()) + 2 + Fonts.urbanist.get(24).getStringWidth(Demise.INSTANCE.getVersion());
        float calcWidth = 450 - watermarkWidth - 19;
        inputting = MouseUtils.isHovered(posX + watermarkWidth + 13, posY + 7, calcWidth, 20, mouseX, mouseY) && mouseButton == 0;

        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (inputting) {
            String lastFilter = filter;

            if (keyCode == Keyboard.KEY_BACK) {
                deleteLastCharacter();
            }

            if (Character.isLetterOrDigit(typedChar) || keyCode == Keyboard.KEY_SPACE) {
                filter += typedChar;
            }

            if (!lastFilter.equals(filter)) {
                SoundUtil.playSound("demise.tick");
                initCategory();
            }
        }

        moduleComponents.forEach(moduleComponent -> moduleComponent.keyTyped(typedChar, keyCode));
    }

    private void deleteLastCharacter() {
        if (!filter.isEmpty()) {
            filter = filter.substring(0, filter.length() - 1);
        }
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