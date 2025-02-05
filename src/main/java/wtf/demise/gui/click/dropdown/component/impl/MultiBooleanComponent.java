package wtf.demise.gui.click.dropdown.component.impl;

import wtf.demise.features.modules.impl.visual.ClickGUI;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseOutSine;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class MultiBooleanComponent extends Component {
    private final MultiBoolValue setting;
    private final Map<BoolValue, EaseOutSine> select = new HashMap<>();

    public MultiBooleanComponent(MultiBoolValue setting) {
        this.setting = setting;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float offset = 8;
        float heightoff = 0;

        RoundedUtils.drawRound(getX() + offset, getY() + Fonts.interRegular.get(15).getHeight() + 2, getWidth() - 5, heightoff, 4, INSTANCE.getModuleManager().getModule(ClickGUI.class).color.get());
        Fonts.interRegular.get(15).drawString(setting.getName(), getX() + 4, getY(), -1);

        for (BoolValue boolValue : setting.getValues()) {
            float off = Fonts.interRegular.get(13).getStringWidth(boolValue.getName()) + 4;
            if (offset + off >= getWidth() - 5) {
                offset = 8;
                heightoff += Fonts.interRegular.get(13).getHeight() + 2;
            }
            select.putIfAbsent(boolValue, new EaseOutSine(250, 1));
            select.get(boolValue).setDirection(boolValue.get() ? Direction.FORWARDS : Direction.BACKWARDS);

            Fonts.interRegular.get(13).drawString(boolValue.getName(), getX() + offset, getY() + Fonts.interRegular.get(15).getHeight() + 2 + heightoff, ColorUtils.interpolateColor2(new Color(128, 128, 128), INSTANCE.getModuleManager().getModule(ClickGUI.class).color.get(), (float) select.get(boolValue).getOutput()));

            offset += off;
        }

        setHeight(Fonts.interRegular.get(15).getHeight() + 10 + heightoff);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouse) {
        float offset = 8;
        float heightoff = 0;
        for (BoolValue boolValue : setting.getValues()) {
            float off = Fonts.interRegular.get(13).getStringWidth(boolValue.getName()) + 4;
            if (offset + off >= getWidth() - 5) {
                offset = 8;
                heightoff += Fonts.interRegular.get(13).getHeight() + 2;
            }
            if (MouseUtils.isHovered2(getX() + offset, getY() + Fonts.interRegular.get(15).getHeight() + 2 + heightoff, Fonts.interRegular.get(13).getStringWidth(boolValue.getName()), Fonts.interRegular.get(13).getHeight(), mouseX, mouseY) && mouse == 0) {
                boolValue.set(!boolValue.get());
            }
            offset += off;
        }
        super.mouseClicked(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.canDisplay();
    }
}
