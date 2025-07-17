package wtf.demise.gui.widget.impl;

import wtf.demise.Demise;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.impl.visual.Shaders;
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
        List<Module> enabledModules = getEnabledModules();
        int middle = sr.getScaledWidth() / 2;
        float offset = 0;

        switch (setting.moduleListMode.get()) {
            case "New":
                currX = renderX;
                currY = renderY;

                this.height = getEnabledModules().size() * getModuleHeight();

                for (int i = 0; i < enabledModules.size(); i++) {
                    Module module = enabledModules.get(i);
                    int width = getModuleWidth(module);
                    int height = getModuleHeight();

                    RenderPosition position = calculateRenderPosition(module, width, middle);

                    renderModule(module, position.x, position.y, offset, width, height, middle, i, false, false);
                    offset = calculateNextOffset(module, height, offset);
                }
                break;
            case "Old":
                int margin = 2;

                for (int i = 0; i < enabledModules.size(); i++) {
                    Module module = enabledModules.get(i);
                    RenderPosition pos = calculateRenderPosition(module, getModuleWidth(module), middle);
                    float moduleX = (renderX > middle) ? pos.x + (width - mc.fontRendererObj.getStringWidth(module.getName() + module.getTag())) : pos.x;
                    float y = pos.y + offset;

                    mc.fontRendererObj.drawStringWithShadow(module.getName(), moduleX, y, setting.color(i));
                    mc.fontRendererObj.drawStringWithShadow(module.getTag(), moduleX + mc.fontRendererObj.getStringWidth(module.getName()), y, Color.lightGray.getRGB());
                    offset += mc.fontRendererObj.FONT_HEIGHT + margin;
                }

                this.height = getEnabledModules().size() * (mc.fontRendererObj.FONT_HEIGHT + margin);
                break;
        }
    }

    @Override
    public void onShader(ShaderEvent event) {
        if (!setting.moduleListMode.is("New")) {
            return;
        }

        int middle = sr.getScaledWidth() / 2;
        List<Module> enabledModules = getEnabledModules();

        float offset = 0;

        for (int i = 0; i < enabledModules.size(); i++) {
            Module module = enabledModules.get(i);
            int width = getModuleWidth(module);
            int height = getModuleHeight();

            RenderPosition position = calculateRenderPosition(module, width, middle);

            renderModule(module, position.x, position.y, offset, width, height, middle, i, true, event.getShaderType() == ShaderEvent.ShaderType.GLOW);

            if (!module.isHidden()) {
                if (!setting.hideRender.get() || !Demise.INSTANCE.getModuleManager().getModulesByCategory().get(ModuleCategory.Visual).contains(module)) {
                    offset = calculateNextOffset(module, height, offset);
                }
            }
        }
    }

    public static List<Module> getEnabledModules() {
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : INSTANCE.getModuleManager().getModules()) {
            if (module.isHidden() || (setting.hideRender.get() && Demise.INSTANCE.getModuleManager().getModulesByCategory().get(ModuleCategory.Visual).contains(module))) {
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
        return switch (setting.moduleListMode.get()) {
            case "New" -> setting.getFr().getStringWidth(module.getName() + module.getTag()) + xPadding;
            case "Old" -> mc.fontRendererObj.getStringWidth(module.getName() + module.getTag());
            default -> 0;
        };
    }

    public static int getModuleHeight() {
        return switch (setting.moduleListMode.get()) {
            case "New" -> setting.getFr().getHeight() + yPadding;
            case "Old" -> mc.fontRendererObj.FONT_HEIGHT + 2;
            default -> 0;
        };
    }

    private void renderModule(Module module, float localX, float localY, float offset, int width, int height, int middle, int index, boolean shader, boolean isGlow) {
        renderBackground(localX, localY, offset, width, height, middle, shader, isGlow);
        renderText(module, localX, localY, offset, width - xPadding, middle, index, shader);
    }

    private void renderBackground(float localX, float localY, float offset, int width, int height, int middle, boolean shader, boolean isGlow) {
        if (!shader) {
            if (localX < middle) {
                RenderUtils.drawRect(localX, localY + offset, width, height, setting.bgColor());
            } else {
                RenderUtils.drawRect(localX + this.width - width, localY + offset, width, height, setting.bgColor());
            }
        } else {
            if (!isGlow) {
                if (localX < middle) {
                    RenderUtils.drawRect(localX, localY + offset, width, height, Color.black.getRGB());
                } else {
                    RenderUtils.drawRect(localX + this.width - width, localY + offset, width, height, Color.black.getRGB());
                }
            } else {
                int color = Demise.INSTANCE.getModuleManager().getModule(Shaders.class).syncColor.get() ? setting.color((int) localY) : Demise.INSTANCE.getModuleManager().getModule(Shaders.class).bloomColor.get().getRGB();

                if (localX < middle) {
                    RenderUtils.drawRect(localX, localY + offset, width, height, color);
                } else {
                    RenderUtils.drawRect(localX + this.width - width, localY + offset, width, height, color);
                }
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