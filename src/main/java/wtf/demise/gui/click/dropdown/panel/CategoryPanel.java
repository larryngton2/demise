package wtf.demise.gui.click.dropdown.panel;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjglx.input.Mouse;
import wtf.demise.Demise;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.click.dropdown.component.ModuleComponent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseInOutQuad;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class CategoryPanel implements IComponent {
    private float x, y;
    private float width = 110, height;
    private boolean opened = true;
    private final EaseInOutQuad openAnimation = new EaseInOutQuad(250, 1);
    private final ModuleCategory category;
    private final CopyOnWriteArrayList<ModuleComponent> moduleComponents = new CopyOnWriteArrayList<>();
    public static boolean shader;
    private int scroll = 60;

    public CategoryPanel(ModuleCategory category) {
        this.category = category;
        this.openAnimation.setDirection(Direction.BACKWARDS);

        for (Module module : Demise.INSTANCE.getModuleManager().getModulesByCategory(category)) {
            moduleComponents.add(new ModuleComponent(module));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.openAnimation.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);

        if (MouseUtils.isHovered(x, 0, width, new ScaledResolution(mc).getScaledHeight(), mouseX, mouseY)) {
            if (Mouse.hasWheel() && INSTANCE.getDropdownGUI().getOpeningAnimation().isDone()) {
                final float wheel = Mouse.getDWheel();

                if (wheel != 0) {
                    scroll += wheel > 0 ? 50 : -50;
                }
            }
        }

        GlStateManager.translate(0, scroll, 0);

        mouseY -= scroll;

        RenderUtils.scaleStart((float) new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth() / 2, (float) new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight() / 2, (float) INSTANCE.getDropdownGUI().getOpeningAnimation().getOutput());

        if (!shader) {
            RoundedUtils.drawRound(x, y - 2, width, (float) (19 + ((height - 19) * openAnimation.getOutput())), 7, new Color(0, 0, 0, (int) Math.min(Demise.INSTANCE.getModuleManager().getModule(Interface.class).bgAlpha.get() + 50, 255)));
            Fonts.interBold.get(18).drawCenteredStringWithShadow(category.getName(), x + width / 2, y + 4.5f, -1);
        } else {
            RoundedUtils.drawShaderRound(x, y - 2, width, (float) (19 + ((height - 19) * openAnimation.getOutput())), 7, Color.black);
        }

        float componentOffsetY = 17;

        if (!shader) {
            for (ModuleComponent component : moduleComponents) {
                component.setX(x);
                component.setY(y + componentOffsetY);
                component.setWidth(width);
                if (openAnimation.getOutput() > 0.7f) {
                    component.drawScreen(mouseX, mouseY);
                }
                componentOffsetY += (float) (component.getHeight() * openAnimation.getOutput());
            }
        } else {
            for (ModuleComponent component : moduleComponents) {
                component.setX(x);
                component.setY(y + componentOffsetY);
                component.setWidth(width);
                componentOffsetY += (float) (component.getHeight() * openAnimation.getOutput());
            }
        }

        height = componentOffsetY;

        RenderUtils.scaleEnd();
        GlStateManager.translate(0, -scroll, 0);
        IComponent.super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        mouseY -= scroll;
        GlStateManager.translate(0, scroll, 0);

        if (!shader) {
            if (MouseUtils.isHovered(x, y - 2, width, 19, mouseX, mouseY)) {
                if (mouseButton == 1) {
                    opened = !opened;
                }
            }
            if (opened && !MouseUtils.isHovered(x, y - 2, width, 19, mouseX, mouseY)) {
                int finalMouseY = mouseY;
                moduleComponents.forEach(component -> component.mouseClicked(mouseX, finalMouseY, mouseButton));
            }
        }

        GlStateManager.translate(0, -scroll, 0);
        IComponent.super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!shader) {
            moduleComponents.forEach(component -> component.keyTyped(typedChar, keyCode));
        }
        IComponent.super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        mouseY -= scroll;
        GlStateManager.translate(0, scroll, 0);
        if (!shader) {
            int finalMouseY = mouseY;
            moduleComponents.forEach(component -> component.mouseReleased(mouseX, finalMouseY, state));
        }
        GlStateManager.translate(0, -scroll, 0);
        IComponent.super.mouseReleased(mouseX, mouseY, state);
    }
}
