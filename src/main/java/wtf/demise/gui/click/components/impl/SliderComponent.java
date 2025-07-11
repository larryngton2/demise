package wtf.demise.gui.click.components.impl;

import net.minecraft.util.MathHelper;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    private final SliderValue setting;
    private float anim;
    private boolean dragging;
    private float previousSetting;
    private final TimerUtils soundTimer = new TimerUtils();

    public SliderComponent(SliderValue setting) {
        this.setting = setting;
        previousSetting = setting.get();
        setHeight(Fonts.interRegular.get(15).getHeight() * 2 + Fonts.interRegular.get(15).getHeight() + 2);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        Fonts.interRegular.get(15).drawString(setting.getName(), getX() + 4, getY(), -1);

        anim = RenderUtils.animate(anim, (getWidth() - 8) * (setting.get() - setting.getMin()) / (setting.getMax() - setting.getMin()), 15);
        float sliderWidth = anim;

        RoundedUtils.drawRound(getX() + 4, getY() + Fonts.interRegular.get(15).getHeight() + 2, getWidth() - 8, 2, 1, Color.white.darker().darker().darker().darker());
        RoundedUtils.drawRound(getX() + 4, getY() + Fonts.interRegular.get(15).getHeight() + 2, sliderWidth, 2, 1, Color.white.darker().darker());
        RenderUtils.drawCircle(getX() + 4 + sliderWidth, getY() + Fonts.interRegular.get(15).getHeight() + 3, 0, 360, 2, 0.1f, true, Color.white.brighter().brighter().getRGB());

        Fonts.interRegular.get(15).drawString(setting.getMin() + "", getX() + 2, getY() + Fonts.interRegular.get(15).getHeight() * 2 + 2, new Color(160, 160, 160).getRGB());
        Fonts.interRegular.get(15).drawCenteredString(setting.get() + "", getX() + getWidth() / 2, getY() + Fonts.interRegular.get(15).getHeight() * 2 + 2, -1);
        Fonts.interRegular.get(15).drawString(setting.getMax() + "", getX() - 2 + getWidth() - Fonts.interRegular.get(15).getStringWidth(setting.getMax() + ""), getY() + Fonts.interRegular.get(15).getHeight() * 2 + 2, new Color(160, 160, 160).getRGB());

        if (dragging) {
            float clampedRatio = Math.max(0, Math.min(1, (mouseX - getX()) / getWidth()));
            float rawValue = setting.getMin() + (setting.getMax() - setting.getMin()) * clampedRatio;

            BigDecimal valueBD = BigDecimal.valueOf(rawValue);
            BigDecimal incrementBD = BigDecimal.valueOf(setting.getIncrement());

            BigDecimal snapped = valueBD.divide(incrementBD, 0, RoundingMode.HALF_UP).multiply(incrementBD);
            BigDecimal finalValue = snapped.setScale(getDecimalPoints(String.valueOf(setting.getIncrement())), RoundingMode.HALF_UP);

            setting.setValue(finalValue.floatValue());

            if (previousSetting != setting.get()) {
                if (soundTimer.hasTimeElapsed(25)) {
                    SoundUtil.playSound("demise.tick");
                    soundTimer.reset();
                }
                previousSetting = setting.get();
            }
        }
    }

    public static double incValue(double value, double increment) {
        return Math.round(value / increment) * increment;
    }

    public static Integer getDecimalPoints(String n) {
        if (n.contains(".")) {
            return n.replaceAll(".*\\.(?=\\d?)", "").length();
        }

        return 0;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && MouseUtils.isHovered(getX() + 2, getY() + Fonts.interRegular.get(15).getHeight() + 2, getWidth(), 2, mouseX, mouseY))
            dragging = true;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean isVisible() {
        return this.setting.canDisplay();
    }

    @Override
    public boolean isChild() {
        return this.setting.isChild();
    }
}
