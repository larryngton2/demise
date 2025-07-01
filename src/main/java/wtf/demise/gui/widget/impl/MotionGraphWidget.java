package wtf.demise.gui.widget.impl;

import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;
import wtf.demise.gui.font.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

// it can't get more Russian than this
public class MotionGraphWidget extends Widget {
    private final List<Double> speeds = new ArrayList<>();
    private static final int MAX_POINTS = 50;
    private float interpolatedMaxSpeed = 10;
    private float interpolatedTextY = renderY + 2;
    private double prevSpeed;

    public MotionGraphWidget() {
        super("Motion graph");
        this.width = 150;
        this.height = 50;
        Demise.INSTANCE.getEventManager().unregister(this);
        Demise.INSTANCE.getEventManager().register(this);
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            prevSpeed = MathUtils.interpolateNoUpdateCheck((float) prevSpeed, (float) MoveUtil.getBPS(), 0.5f);
            speeds.add(prevSpeed);

            if (speeds.size() > MAX_POINTS) {
                speeds.remove(0);
            }
        }
    }

    @Override
    public void render() {
        float lastY = interpolatedTextY;

        RoundedUtils.drawRound(renderX, renderY, width, height, 3, new Color(setting.bgColor(), true));

        float gridSpacing = width / 20;
        for (int i = 1; i < 20; i++) {
            float x = renderX + (gridSpacing * i);
            RenderUtils.drawLine(x, renderY, x, renderY + height, 0.5f, new Color(100, 100, 100, 75).getRGB());
        }

        gridSpacing = height / 10;
        for (int i = 1; i < 10; i++) {
            float y = renderY + (gridSpacing * i);
            RenderUtils.drawLine(renderX, y, renderX + width, y, 0.5f, new Color(100, 100, 100, 75).getRGB());
        }

        if (speeds.size() > 1) {
            float xStep = width / (MAX_POINTS - 1f);
            float maxSpeed = 10;

            for (double speed : speeds) {
                maxSpeed = Math.max(maxSpeed, (float) speed + 2);
            }
            maxSpeed = Math.max(maxSpeed, 1f);

            interpolatedMaxSpeed = MathUtils.interpolate(interpolatedMaxSpeed, maxSpeed, 0.75f);

            for (int i = 0; i < speeds.size() - 1; i++) {
                float x1 = renderX + (i * xStep);
                float x2 = renderX + ((i + 1) * xStep);
                float y1 = renderY + height - (float) (speeds.get(i) / interpolatedMaxSpeed * height);
                float y2 = renderY + height - (float) (speeds.get(i + 1) / interpolatedMaxSpeed * height);

                lastY = y2;

                RenderUtils.drawLine(x1, y1, x2, y2, 1, setting.color(i));
            }
        }

        interpolatedTextY = MathUtils.interpolate(interpolatedTextY, lastY, 0.25f);

        String speedText = "- " + String.format("%.2f bps", MoveUtil.getBPS());
        Fonts.interRegular.get(15).drawString(speedText, renderX + width + 2, interpolatedTextY - 2.1, setting.color());
    }

    @Override
    public void onShader(Shader2DEvent e) {
        if (e.getShaderType() != Shader2DEvent.ShaderType.GLOW) {
            RoundedUtils.drawShaderRound(renderX, renderY, width, height, 3, Color.BLACK);
        } else {
            RoundedUtils.drawGradientPreset(renderX, renderY, width, height, 3);
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Motion graph");
    }
}