package wtf.demise.gui.click.components;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.Value;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.PanelGui;
import wtf.demise.gui.click.components.impl.*;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseInOutQuad;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.StringUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class ModuleComponent implements IComponent {
    private Module module;
    private float x, y;
    private boolean isHovered, isExpanded;
    private float height;
    private Color interpolatedColor = new Color(20, 20, 20, 150);
    private Color interpolatedColor1 = new Color(0, 0, 0, 0);
    public boolean visible;
    private boolean visibleSetting;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();
    private final EaseInOutQuad openAnimation = new EaseInOutQuad(250, 1);
    private float slideProgress = 0f;
    private Component cachedComponent;
    private int alpha;
    private float interpolatedY;
    private final TimerUtils alphaTimer = new TimerUtils();

    public ModuleComponent(Module module) {
        openAnimation.setDirection(Direction.BACKWARDS);
        this.module = module;
        this.height = 35;
        for (Value value : module.getValues()) {
            if (value instanceof BoolValue boolValue) {
                settings.add(new BooleanComponent(boolValue));
            }
            if (value instanceof ColorValue colorValue) {
                settings.add(new ColorPickerComponent(colorValue));
            }
            if (value instanceof SliderValue sliderValue) {
                settings.add(new SliderComponent(sliderValue));
            }
            if (value instanceof ModeValue modeValue) {
                settings.add(new ModeComponent(modeValue));
            }
            if (value instanceof MultiBoolValue multiBoolValue) {
                settings.add(new MultiBooleanComponent(multiBoolValue));
            }
            if (value instanceof TextValue textValue) {
                settings.add(new StringComponent(textValue));
            }
        }
    }

    public void initCategory() {
        slideProgress = 0;
    }

    public void render(boolean shader) {
        float width = 375;
        if (!shader) {
            slideProgress = MathUtils.interpolate(slideProgress, visibleSetting ? 1 : 0, 0.2f);
        }
        float slideOffset = (width / 4) * (1.0f - slideProgress);

        if (!shader) {
            if (isHovered) {
                interpolatedColor = ColorUtils.interpolateColorC(interpolatedColor, new Color(35, 35, 35, 190), 0.1f);
            } else {
                interpolatedColor = ColorUtils.interpolateColorC(interpolatedColor, new Color(20, 20, 20, 150), 0.1f);
            }

            if (module.isEnabled()) {
                interpolatedColor1 = ColorUtils.interpolateColorC(interpolatedColor1, new Color(50, 50, 50, 150), 0.1f);
            } else {
                interpolatedColor1 = ColorUtils.interpolateColorC(interpolatedColor1, new Color(0, 0, 0, 0), 0.1f);
            }

            RoundedUtils.drawRound(x + slideOffset, y, width, height, 8, interpolatedColor);
            RoundedUtils.drawRound(x + slideOffset, y, width, height, 8, interpolatedColor1);

            Fonts.interRegular.get(18).drawString(module.getName(), x + 7 + slideOffset, y + 9, Color.white.getRGB());
            Fonts.interRegular.get(14).drawString(module.getDescription(), x + 7 + slideOffset, y + 21, new Color(200, 200, 200).getRGB());

            String keyName = module.getKeyBind() == 0 ? "None" : StringUtils.upperSnakeCaseToPascal(Keyboard.getKeyName(module.getKeyBind()));
            Fonts.interRegular.get(14).drawString(keyName, x + width - 8 - Fonts.interRegular.get(14).getStringWidth(keyName) + slideOffset, y + 10, new Color(150, 150, 150, 150).getRGB());

        } else {
            RoundedUtils.drawShaderRound(x + slideOffset, y, width, height, 8, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.isHovered = MouseUtils.isHovered(x, y, 375, 35, mouseX, mouseY);

        float yOffset = 35;
        float width = 375;

        openAnimation.setDirection(isExpanded ? Direction.FORWARDS : Direction.BACKWARDS);

        RenderUtils.scissor(x, PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight(), width, 255, PanelGui.interpolatedScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        float slideOffset = (width / 4) * (1.0f - slideProgress);

        Component hoveredComponent = null;

        for (Component component : settings) {
            if (!component.isVisible()) continue;

            component.setX((component.isChild() ? x + 5 : x) + slideOffset);
            component.setY((float) (y + yOffset * openAnimation.getOutput()) + 1);
            component.setWidth(component.isChild() ? width - 5 : width);

            if (openAnimation.getOutput() > 0.7f) {
                component.drawScreen(mouseX, mouseY);

                if (component.isChild()) {
                    RenderUtils.drawRect(x + 3.5f + slideOffset, component.getY() - 2.8f, 1, component.getHeight(), Color.gray.getRGB());
                }

                if (component.isHovered(mouseX, mouseY)) {
                    hoveredComponent = component;
                }
            }

            yOffset += (float) (component.getHeight() * openAnimation.getOutput());
            this.height = yOffset;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (hoveredComponent != null && hoveredComponent.isVisible() && mouseY <= PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight() + 255 && mouseY >= PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight() && !hoveredComponent.getDescription().isEmpty()) {
            cachedComponent = hoveredComponent;

            if (alphaTimer.hasTimeElapsed(10)) {
                alpha = Math.min(alpha + 10, 255);
                alphaTimer.reset();
            }

            interpolatedY = MathUtils.interpolate(interpolatedY, mouseY + 3 - (Fonts.interRegular.get(15).getHeight() + 5) / 2f, 0.15f);
        } else {
            if (alphaTimer.hasTimeElapsed(10)) {
                alpha = Math.max(alpha - 10, 0);
                alphaTimer.reset();
            }

            if (alpha == 0) {
                cachedComponent = null;
            }
        }

        if (alpha > 0 && cachedComponent != null) {
            String str = cachedComponent.getDescription();
            float x = this.x + width + 15;
            float floatA = (float) alpha;
            int maxWidth = Math.min(Fonts.interRegular.get(15).getStringWidth(str), 200);

            Color i = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgColor(), true);
            Color finalColor = new Color(i.getRed(), i.getGreen(), i.getBlue(), (int) ((floatA / 255) * i.getAlpha()));

            ArrayList<String> lines = new ArrayList<>();

            StringBuilder currentLine = new StringBuilder();
            for (String word : str.split(" ")) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (Fonts.interRegular.get(15).getStringWidth(testLine) <= maxWidth) {
                    currentLine.setLength(0);
                    currentLine.append(testLine);
                } else {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                    currentLine.append(word);
                }
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            float lineHeight = Fonts.interRegular.get(15).getHeight() + 2;
            float totalHeight = lines.size() * lineHeight + 5;
            RoundedUtils.drawRound(x, interpolatedY - 2.5f, maxWidth + 6, totalHeight - 2.5f, 5, finalColor);

            for (int j = 0; j < lines.size(); j++) {
                Fonts.interRegular.get(15).drawString(lines.get(j), x + 3, interpolatedY + 2.5f + j * lineHeight, new Color(255, 255, 255, alpha).getRGB());
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered) {
            if (visible && mouseY > PanelGui.posY + 12 + Fonts.urbanist.get(35).getHeight()) {
                switch (mouseButton) {
                    case 0 -> module.toggle();
                    case 1 -> isExpanded = !isExpanded;
                }
            }
        } else if (isExpanded) {
            settings.forEach(setting -> setting.mouseClicked(mouseX, mouseY, mouseButton));
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (isExpanded && !isHovered) {
            settings.forEach(setting -> setting.mouseReleased(mouseX, mouseY, state));
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (isExpanded && !isHovered) {
            settings.forEach(setting -> setting.keyTyped(typedChar, keyCode));
        }
    }
}