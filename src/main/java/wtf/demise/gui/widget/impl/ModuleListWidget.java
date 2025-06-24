package wtf.demise.gui.widget.impl;

import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleListWidget extends Widget {
    private static final int xPadding = 7;
    private static final int yPadding = 5;
    public static float currX;
    public static float currY;

    public ModuleListWidget() {
        super("ModuleList");
        this.x = 0.89062506f;
        this.y = 0.009259259f;
    }

    @Override
    public void render() {
        if (!shouldRender()) return;

        currX = renderX;
        currY = renderY;

        this.height = getEnabledModules().size() * getModuleHeight();

        int middle = sr.getScaledWidth() / 2;
        List<Module> enabledModules = getEnabledModules();

        float offset = 0;

        for (int i = 0; i < enabledModules.size(); i++) {
            Module module = enabledModules.get(i);
            int width = getModuleWidth(module);
            int height = getModuleHeight();

            RenderPosition position = calculateRenderPosition(module, width, middle);

            renderModule(module, position.x, position.y, offset, width, height, middle, i, false);

            if (!module.isHidden()) {
                if (!(setting.hideRender.get() && module.getCategory() == ModuleCategory.Visual)) {
                    offset = calculateNextOffset(module, height, offset);
                }
            }
        }
    }

    @Override
    public void onShader(Shader2DEvent event) {
        if (!shouldRender()) return;

        int middle = sr.getScaledWidth() / 2;
        List<Module> enabledModules = getEnabledModules();

        float offset = 0;

        for (int i = 0; i < enabledModules.size(); i++) {
            Module module = enabledModules.get(i);
            int width = getModuleWidth(module);
            int height = getModuleHeight();

            RenderPosition position = calculateRenderPosition(module, width, middle);

            renderModule(module, position.x, position.y, offset, width, height, middle, i, true);

            if (!module.isHidden()) {
                if (!(setting.hideRender.get() && module.getCategory() == ModuleCategory.Visual)) {
                    offset = calculateNextOffset(module, height, offset);
                }
            }
        }
    }

    public static List<Module> getEnabledModules() {
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : INSTANCE.getModuleManager().getModules()) {
            if (module.isHidden() || (setting.hideRender.get() && module.getCategory() == ModuleCategory.Visual)) {
                continue;
            }
            Animation moduleAnimation = module.getAnimation();
            moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
            enabledModules.add(module);
        }
        enabledModules.sort(Comparator.comparing(ModuleListWidget::getModuleWidth).reversed());
        return enabledModules;
    }

    private static int getModuleWidth(Module module) {
        return setting.getFr().getStringWidth(module.getName() + module.getTag()) + xPadding;
    }

    public static int getModuleHeight() {
        return setting.getFr().getHeight() + yPadding;
    }

    private void renderModule(Module module, float localX, float localY, float offset, int width, int height, int middle, int index, boolean shader) {
        renderBackground(localX, localY, offset, width, height, middle, shader);
        renderText(module, localX, localY, offset, width - xPadding, middle, index, shader);
    }

    private void renderBackground(float localX, float localY, float offset, int width, int height, int middle, boolean shader) {
        if (!shader) {
            if (localX < middle) {
                RenderUtils.drawRect(localX, localY + offset, width, height, setting.bgColor());
            } else {
                RenderUtils.drawRect(localX + this.width - width, localY + offset, width, height, setting.bgColor());
            }
        } else {
            if (localX < middle) {
                RenderUtils.drawRect(localX, localY + offset, width, height, Color.black.getRGB());
            } else {
                RenderUtils.drawRect(localX + this.width - width, localY + offset, width, height, Color.black.getRGB());
            }
        }
    }

    private void renderText(Module module, float localX, float localY, float offset, int width, int middle, int index, boolean shader) {
        String text = module.getName() + module.getTag();
        int color = setting.color(index);
        float textY = localY + offset + 4 + (yPadding / 2f) - 1.3f;
        float textX = localX - width + this.width - (xPadding / 2f) - 0.5f;

        if (!shader) {
            if (localX < middle) {
                setting.getFr().drawString(text, localX + (xPadding / 2f), textY, color);
            } else {
                setting.getFr().drawString(text, textX, textY, color);
            }
        } else {
            if (localX < middle) {
                setting.getFr().drawString(text, localX + (xPadding / 2f), textY, Color.black.getRGB());
            } else {
                setting.getFr().drawString(text, textX, textY, Color.black.getRGB());
            }
        }
    }

    private static class RenderPosition {
        float x, y;

        RenderPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private RenderPosition calculateRenderPosition(Module module, int width, int middle) {
        float localX = renderX;
        float localY = renderY;

        float MOVE_IN_SCALE = 2.0f;

        if (localX > middle) {
            localX += (float) Math.abs(module.getAnimation().getOutput() - 1.0) *
                    (MOVE_IN_SCALE + width);
        } else {
            localX -= (float) Math.abs((module.getAnimation().getOutput() - 1.0) *
                    (MOVE_IN_SCALE + width));
        }

        return new RenderPosition(localX, localY);
    }

    private float calculateNextOffset(Module module, int height, float offset) {
        return (float) (offset + ((module.getAnimation().getOutput()) * (height)));
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Module list");
    }
}