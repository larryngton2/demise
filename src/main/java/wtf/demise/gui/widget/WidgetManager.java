package wtf.demise.gui.widget;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.ChatGUIEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.gui.widget.impl.*;
import wtf.demise.utils.InstanceAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WidgetManager implements InstanceAccess {
    public List<Widget> widgetList = new ArrayList<>();

    public WidgetManager() {
        INSTANCE.getEventManager().register(this);

        register(
                new MotionGraphWidget(),
                new ModuleListWidget(),
                new TargetHUDWidget(),
                new PotionHUDWidget(),
                new KeystrokeWidget(),
                new InfoWidget(),
                new RadarWidget()
        );
    }

    public boolean loaded;

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        for (Widget widget : widgetList) {
            if (widget.shouldRender()) {
                widget.updatePos();
                widget.render();
            }
        }
    }

    @EventTarget
    public void onShader2D(ShaderEvent event) {
        for (Widget widget : widgetList) {
            if (widget.shouldRender()) {
                widget.onShader(event);
            }
        }
    }

    @EventTarget
    public void onChatGUI(ChatGUIEvent event) {
        Widget draggingWidget = null;
        for (Widget widget : widgetList) {
            if (widget.shouldRender() && widget.dragging) {
                draggingWidget = widget;
                break;
            }
        }

        for (Widget widget : widgetList) {
            if (widget.shouldRender()) {
                widget.onChatGUI(event.mouseX, event.mouseY, (draggingWidget == null || draggingWidget == widget));
                if (widget.dragging) draggingWidget = widget;
            }
        }
    }

    private void register(Widget... widgets) {
        this.widgetList.addAll(Arrays.asList(widgets));
    }

    public Widget get(String name) {
        for (Widget widget : widgetList) {
            if (widget.name.equalsIgnoreCase(name)) {
                return widget;
            }
        }
        return null;
    }

    public <module extends Widget> module get(Class<? extends module> moduleClass) {
        Iterator<Widget> var2 = this.widgetList.iterator();
        Widget module;
        do {
            if (!var2.hasNext()) {
                return null;
            }
            module = var2.next();
        } while (module.getClass() != moduleClass);

        return (module) module;
    }
}