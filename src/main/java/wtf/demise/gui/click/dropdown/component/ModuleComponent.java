package wtf.demise.gui.click.dropdown.component;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.impl.visual.ClickGUI;
import wtf.demise.features.values.Value;
import wtf.demise.features.values.impl.*;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.dropdown.component.impl.*;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseInOutQuad;
import wtf.demise.utils.animations.impl.EaseOutSine;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class ModuleComponent implements IComponent {
    private float x, y, width, height = 19;
    private final Module module;
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();
    private boolean opened;
    private final EaseInOutQuad openAnimation = new EaseInOutQuad(250, 1);
    private final EaseOutSine toggleAnimation = new EaseOutSine(300, 1);
    private final EaseOutSine hoverAnimation = new EaseOutSine(200, 1);
    public static boolean shader;

    public ModuleComponent(Module module) {
        this.module = module;
        openAnimation.setDirection(Direction.BACKWARDS);
        toggleAnimation.setDirection(Direction.BACKWARDS);
        hoverAnimation.setDirection(Direction.BACKWARDS);
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

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float yOffset = 20;
        openAnimation.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);
        toggleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
        hoverAnimation.setDirection(isHovered(mouseX, mouseY) ? Direction.FORWARDS : Direction.BACKWARDS);

        Fonts.interRegular.get(14).drawCenteredString(module.getName(), x + getWidth() / 2, y + yOffset / 2 - 3, ColorUtils.interpolateColor2(Color.GRAY, INSTANCE.getModuleManager().getModule(ClickGUI.class).color.get(), (float) toggleAnimation.getOutput()));

        for (Component component : settings) {
            if (!component.isVisible()) continue;
            component.setX(x);
            component.setY((float) (y + yOffset * openAnimation.getOutput()) + 1);
            component.setWidth(width);
            if (openAnimation.getOutput() > .7f) {
                component.drawScreen(mouseX, mouseY);
            }
            yOffset += (float) (component.getHeight() * openAnimation.getOutput());
            this.height = yOffset + 1.45f;
        }

        if (settings.isEmpty()) {
            this.height = yOffset + 1.45f;
        }

        IComponent.super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHovered(mouseX, mouseY)) {
            switch (mouseButton) {
                case 0 -> module.toggle();
                case 1 -> opened = !opened;
            }
        }
        if (opened && !isHovered(mouseX, mouseY)) {
            settings.forEach(setting -> setting.mouseClicked(mouseX, mouseY, mouseButton));
        }
        IComponent.super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened && !isHovered(mouseX, mouseY)) {
            settings.forEach(setting -> setting.mouseReleased(mouseX, mouseY, state));
        }
        IComponent.super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            settings.forEach(setting -> setting.keyTyped(typedChar, keyCode));
        }
        IComponent.super.keyTyped(typedChar, keyCode);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return MouseUtils.isHovered(x + 2, y, width - 2, 17, mouseX, mouseY);
    }
}
