package wtf.demise.gui.click.dropdown.component.impl;

import net.minecraft.util.MathHelper;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
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

    public SliderComponent(SliderValue setting) {
        this.setting = setting;
        setHeight(Fonts.interRegular.get(15).getHeight() * 2 + Fonts.interRegular.get(15).getHeight() + 2);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        Fonts.interRegular.get(15).drawString(setting.getName(), getX() + 4, getY(), -1);

        anim = RenderUtils.animate(anim, (getWidth() - 8) * (setting.get() - setting.getMin()) / (setting.getMax() - setting.getMin()), 15);
        float sliderWidth = anim;

        RoundedUtils.drawRound(getX() + 4, getY() + Fonts.interRegular.get(15).getHeight() + 2, getWidth() - 8, 2, 2, Demise.INSTANCE.getModuleManager().getModule(Interface.class).getMainColor().darker().darker().darker().darker());
        RoundedUtils.drawRound(getX() + 4, getY() + Fonts.interRegular.get(15).getHeight() + 2, sliderWidth, 2, 2, Demise.INSTANCE.getModuleManager().getModule(Interface.class).getMainColor().darker().darker());
        RenderUtils.drawCircle(getX() + 4 + sliderWidth, getY() + Fonts.interRegular.get(15).getHeight() + 3, 0, 360, 2, 0.1f, true, Demise.INSTANCE.getModuleManager().getModule(Interface.class).getMainColor().brighter().brighter().getRGB());

        Fonts.interRegular.get(15).drawString(setting.getMin() + "", getX() + 2, getY() + Fonts.interRegular.get(15).getHeight() * 2 + 2, new Color(160, 160, 160).getRGB());
        Fonts.interRegular.get(15).drawCenteredString(setting.get() + "", getX() + getWidth() / 2, getY() + Fonts.interRegular.get(15).getHeight() * 2 + 2, -1);
        Fonts.interRegular.get(15).drawString(setting.getMax() + "", getX() - 2 + getWidth() - Fonts.interRegular.get(15).getStringWidth(setting.getMax() + ""), getY() + Fonts.interRegular.get(15).getHeight() * 2 + 2, new Color(160, 160, 160).getRGB());

        if (dragging) {
            final double difference = setting.getMax() - setting.getMin(), value = setting.getMin() + MathHelper.clamp_float((mouseX - getX()) / getWidth(), 0, 1) * difference;
            setting.setValue(BigDecimal.valueOf(MathUtils.incValue(value, setting.getIncrement())).setScale(getDecimalPoints(String.valueOf(setting.getIncrement())), RoundingMode.FLOOR).floatValue());
        }
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
}
