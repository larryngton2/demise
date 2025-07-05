package wtf.demise.gui.widget;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjglx.input.Mouse;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public abstract class Widget implements InstanceAccess {
    @Expose
    @SerializedName("name")
    public String name;
    @Expose
    @SerializedName("x")
    public float x;
    @Expose
    @SerializedName("y")
    public float y;
    protected float renderX, renderY;
    public float width;
    public float height;
    public boolean dragging;
    private int dragX, dragY;
    public int align;
    protected ScaledResolution sr;
    protected static Interface setting = INSTANCE.getModuleManager().getModule(Interface.class);

    public Widget(String name) {
        this.name = name;
        this.x = 0f;
        this.y = 0f;
        this.width = 100f;
        this.height = 100f;
        this.align = WidgetAlign.LEFT | WidgetAlign.TOP;
    }

    public Widget(String name, int align) {
        this(name);
        this.align = align;
    }

    public abstract void onShader(ShaderEvent event);

    public abstract void render();

    public void updatePos() {
        sr = new ScaledResolution(mc);

        renderX = x * sr.getScaledWidth();
        renderY = y * sr.getScaledHeight();

        if (dragging) {
            if (renderX < 0f) x = 0f;
            if (renderX > sr.getScaledWidth() - width) x = (sr.getScaledWidth() - width) / sr.getScaledWidth();
            if (renderY < 0f) y = 0f;
            if (renderY > sr.getScaledHeight() - height) y = (sr.getScaledHeight() - height) / sr.getScaledHeight();
        }

        if (align == (WidgetAlign.LEFT | WidgetAlign.TOP)) return;

        if ((align & WidgetAlign.RIGHT) != 0) {
            renderX -= width;
        } else if ((align & WidgetAlign.CENTER) != 0) {
            renderX -= width / 2f;
        }

        if ((align & WidgetAlign.BOTTOM) != 0) {
            renderY -= height;
        } else if ((align & WidgetAlign.MIDDLE) != 0) {
            renderY -= height / 2f;
        }
    }

    public final void onChatGUI(int mouseX, int mouseY, boolean drag) {
        boolean hovering = MouseUtils.isHovered(renderX, renderY, width, height, mouseX, mouseY);

        if (dragging) {
            RoundedUtils.drawRoundOutline(renderX - 1, renderY - 1, width + 2, height + 2, 4, 0.05f, new Color(0, 0, 0, 0), Color.WHITE);
        }

        if (hovering && Mouse.isButtonDown(0) && !dragging && drag) {
            dragging = true;
            dragX = mouseX;
            dragY = mouseY;
        }

        if (!Mouse.isButtonDown(0)) dragging = false;

        if (dragging) {
            float deltaX = (float) (mouseX - dragX) / sr.getScaledWidth();
            float deltaY = (float) (mouseY - dragY) / sr.getScaledHeight();

            x += deltaX;
            y += deltaY;

            dragX = mouseX;
            dragY = mouseY;
        }
    }

    public abstract boolean shouldRender();
}
