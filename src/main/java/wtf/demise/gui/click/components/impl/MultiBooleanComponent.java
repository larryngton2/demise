package wtf.demise.gui.click.components.impl;

import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.EaseOutSine;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;

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
        float heightoff = 4;
        float lineHeight = Fonts.interRegular.get(13).getHeight() + 2;

        Fonts.interSemiBold.get(15).drawString(setting.getName(), getX() + 4, getY() + 2, -1);

        for (BoolValue boolValue : setting.getValues()) {
            select.putIfAbsent(boolValue, new EaseOutSine(100, 1));
            select.get(boolValue).setDirection(boolValue.get() ? Direction.FORWARDS : Direction.BACKWARDS);

            Fonts.interRegular.get(13).drawString(boolValue.getName(), getX() + 8, getY() + Fonts.interRegular.get(15).getHeight() + 1 + heightoff, ColorUtils.interpolateColor2(new Color(128, 128, 128), Color.white, (float) select.get(boolValue).getOutput()));

            heightoff += lineHeight;
        }

        setHeight(Fonts.interRegular.get(15).getHeight() + 4 + heightoff);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouse) {
        float heightoff = 4;
        float lineHeight = Fonts.interRegular.get(13).getHeight() + 2;

        for (BoolValue boolValue : setting.getValues()) {
            if (MouseUtils.isHovered(getX() + 8, getY() + Fonts.interRegular.get(15).getHeight() + 1 + heightoff, Fonts.interRegular.get(13).getStringWidth(boolValue.getName()), Fonts.interRegular.get(13).getHeight(), mouseX, mouseY) && mouse == 0) {
                boolValue.set(!boolValue.get());
                SoundUtil.playSound("demise.tick");
            }
            heightoff += lineHeight;
        }
        super.mouseClicked(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.canDisplay();
    }

    @Override
    public boolean isChild() {
        return this.setting.isChild();
    }
}
