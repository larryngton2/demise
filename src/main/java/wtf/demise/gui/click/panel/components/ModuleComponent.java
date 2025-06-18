package wtf.demise.gui.click.panel.components;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import wtf.demise.features.modules.Module;
import wtf.demise.features.values.Value;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.panel.PanelGui;
import wtf.demise.gui.click.panel.components.impl.*;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseInOutQuad;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.StringUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
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
        slideProgress = MathUtils.interpolate(slideProgress, visibleSetting ? 1 : 0, 0.1f);
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

        RenderUtils.scissor(x, PanelGui.posY + 17 + Fonts.urbanist.get(35).getHeight(), width, 250, PanelGui.interpolatedScale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        float slideOffset = (width / 4) * (1.0f - slideProgress);

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
            }

            yOffset += (float) (component.getHeight() * openAnimation.getOutput());
            this.height = yOffset;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered) {
            if (visible) {
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