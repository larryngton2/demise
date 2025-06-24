package wtf.demise.gui.click.components.impl;

import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.SmoothStepAnimation;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;

public class BooleanComponent extends Component {
    private final BoolValue setting;
    private final SmoothStepAnimation toggleAnimation = new SmoothStepAnimation(175, 1);

    public BooleanComponent(BoolValue setting) {
        this.setting = setting;
        this.toggleAnimation.setDirection(Direction.BACKWARDS);
        setHeight(Fonts.interRegular.get(15).getHeight() + 5);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        this.toggleAnimation.setDirection(this.setting.get() ? Direction.FORWARDS : Direction.BACKWARDS);

        Fonts.interRegular.get(15).drawString(setting.getName(), getX() + 4, getY() + 2.5f, -1);

        RoundedUtils.drawRound(getX() + getWidth() - 15.5f, getY() + 2.5f, 13f, 5, 2.25f, ColorUtils.interpolateColorC(new Color(128, 128, 128, 255), Color.white, (float) toggleAnimation.getOutput()).darker());
        RoundedUtils.drawRound(getX() + getWidth() - 15.5f + 8 * (float) toggleAnimation.getOutput(), getY() + 2.5f, 5, 5, 2.25f, Color.WHITE);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(getX() + getWidth() - 17.5f, getY() + 2.5f, 12.5f, 5, mouseX, mouseY) && mouseButton == 0) {
            this.setting.set(!this.setting.get());
            SoundUtil.playSound("demise.tick");
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
