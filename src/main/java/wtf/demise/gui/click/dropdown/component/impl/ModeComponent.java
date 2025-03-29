package wtf.demise.gui.click.dropdown.component.impl;

import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.render.MouseUtils;

import java.awt.*;

public class ModeComponent extends Component {
    private final ModeValue setting;

    public ModeComponent(ModeValue setting) {
        this.setting = setting;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float heightoff = -1;
        float lineHeight = Fonts.interRegular.get(13).getHeight() + 2;

        Fonts.interSemiBold.get(15).drawString(setting.getName(), getX() + 4, getY() + 1, -1);

        for (String text : setting.getModes()) {
            if (text.equals(setting.get())) {
                Fonts.interRegular.get(13).drawString(text, getX() + 8, getY() + Fonts.interRegular.get(15).getHeight() + 4 + heightoff, Demise.INSTANCE.getModuleManager().getModule(Interface.class).getMainColor().getRGB());
            } else {
                Fonts.interRegular.get(13).drawString(text, getX() + 8, getY() + Fonts.interRegular.get(15).getHeight() + 4 + heightoff, Color.GRAY.getRGB());
            }

            heightoff += lineHeight;
        }

        setHeight(Fonts.interRegular.get(15).getHeight() + 6 + heightoff);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouse) {
        float heightoff = 0;
        float lineHeight = Fonts.interRegular.get(13).getHeight() + 2;

        for (String text : setting.getModes()) {
            if (MouseUtils.isHovered(getX() + 8, getY() + Fonts.interRegular.get(15).getHeight() + 4 + heightoff,
                    Fonts.interRegular.get(13).getStringWidth(text), Fonts.interRegular.get(13).getHeight(), mouseX, mouseY) && mouse == 0) {
                setting.set(text);
            }
            heightoff += lineHeight;
        }
        super.mouseClicked(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.canDisplay();
    }
}