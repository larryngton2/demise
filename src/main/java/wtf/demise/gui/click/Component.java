package wtf.demise.gui.click;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

@Getter
@Setter
public class Component implements IComponent {
    private float x, y, width, height;
    private Color color = new Color(INSTANCE.getModuleManager().getModule(Interface.class).color());
    private int colorRGB = color.getRGB();
    private String description = "";

    public void drawBackground(Color color) {
        RenderUtils.drawRect(x, y, width, height, color.getRGB());
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isHovered(float mouseX, float mouseY, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean isChild() {
        return false;
    }
}
