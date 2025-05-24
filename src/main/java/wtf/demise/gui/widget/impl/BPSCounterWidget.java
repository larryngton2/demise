package wtf.demise.gui.widget.impl;

import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class BPSCounterWidget extends Widget {
    public BPSCounterWidget() {
        super("BPS counter");
        this.x = 0.40520814f;
        this.y = 0.83316565f;
        this.height = 20;
    }

    private final ArrayList<Double> avgBPSList = new ArrayList<>();
    private final TimerUtils avgBPSTimer = new TimerUtils();
    private double avgBPS = 0;
    private final ArrayList<Double> maxBPSList = new ArrayList<>();
    private final TimerUtils maxBPSTimer = new TimerUtils();
    private double maxBPS = 0;
    float localX;
    float localY;

    @Override
    public void render() {
        draw(false);
    }

    @Override
    public void onShader(Shader2DEvent event) {
        draw(true);
    }

    private void draw(boolean shader) {
        double currBPS = BigDecimal.valueOf(MoveUtil.getBPS()).setScale(2, RoundingMode.FLOOR).doubleValue();
        String finalString;

        localX = renderX;
        localY = renderY;

        if (setting.advancedBPS.get()) {
            if (!avgBPSTimer.hasTimeElapsed(1000)) {
                avgBPSList.add(MoveUtil.getBPS());
            } else {
                double sum = 0;

                for (double num : avgBPSList) {
                    sum += num;
                }

                avgBPS = sum / avgBPSList.toArray().length;

                avgBPSList.clear();
                avgBPSTimer.reset();
            }

            maxBPSList.add(currBPS);

            if (maxBPSTimer.hasTimeElapsed(1000)) {
                maxBPSList.clear();
                maxBPSTimer.reset();
            } else {
                double max = maxBPSList.get(0);

                for (int i = 1; i < maxBPSList.toArray().length; i++) {
                    if (maxBPSList.get(i) > max) {
                        max = maxBPSList.get(i);
                    }
                }

                maxBPS = max;
            }

            if (mc.thePlayer.ticksExisted > 20) {
                avgBPS = BigDecimal.valueOf(avgBPS).setScale(2, RoundingMode.FLOOR).doubleValue();
            } else {
                avgBPS = 0;
            }

            finalString = "Curr: " + currBPS + " | Avg: " + avgBPS + " | Max: " + maxBPS;
        } else {
            finalString = currBPS + "bps";
        }

        float x;
        float width = Fonts.interSemiBold.get(18).getStringWidth(finalString) + 10;

        if (localX < sr.getScaledWidth() / 2) {
            x = localX;
        } else {
            x = localX + this.width - width;
        }

        if (!shader) {
            RoundedUtils.drawRound(x, localY, width, height, 7, new Color(setting.bgColor(), true));
            Fonts.interSemiBold.get(18).drawString(finalString, x + (width / 2) - ((float) Fonts.interSemiBold.get(18).getStringWidth(finalString) / 2), localY + (height / 2) - Fonts.interSemiBold.get(18).getHeight() + 8.5f, -1);
        } else {
            RoundedUtils.drawShaderRound(x, localY, width, height, 7, Color.black);
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("BPS counter");
    }
}
