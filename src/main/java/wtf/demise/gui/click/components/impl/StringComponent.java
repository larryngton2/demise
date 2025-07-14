package wtf.demise.gui.click.components.impl;

import org.apache.commons.lang3.math.NumberUtils;
import org.lwjglx.input.Keyboard;
import wtf.demise.features.values.impl.TextValue;
import wtf.demise.gui.click.Component;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.impl.DecelerateAnimation;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;

import java.awt.*;

public class StringComponent extends Component {
    private final TextValue setting;
    private final Animation input = new DecelerateAnimation(250, 1);
    private boolean inputting;
    private String text = "";

    public StringComponent(TextValue setting) {
        this.setting = setting;
        setHeight(Fonts.interRegular.get(14).getHeight() * 2 + 4);
        input.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        setDescription(setting.getDescription());

        input.setDirection(inputting ? Direction.FORWARDS : Direction.BACKWARDS);
        text = setting.get();
        if (setting.isOnlyNumber() && !NumberUtils.isNumber(text)) {
            text = text.replaceAll("[a-zA-Z]", "");
        }
        String textToDraw = setting.get().isEmpty() && !inputting ? "Empty..." : setting.getText();
        Fonts.interRegular.get(14).drawString(setting.getName(), getX() + 4, getY(), -1);
        drawTextWithLineBreaks(textToDraw + (inputting && text.length() < 59 && System.currentTimeMillis() % 1000 > 500 ? "|" : ""), getX() + 6, getY() + Fonts.interRegular.get(14).getHeight() + 2);
        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(getX(), getY() + Fonts.interRegular.get(14).getHeight() + 4, getWidth(), 4, mouseX, mouseY) && mouseButton == 0) {
            inputting = !inputting;
        } else {
            inputting = false;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (setting.isOnlyNumber() && !NumberUtils.isNumber(String.valueOf(typedChar))) {
            return;
        }
        if (inputting) {
            if (keyCode == Keyboard.KEY_BACK) {
                deleteLastCharacter();
            }

            if (Character.isLetterOrDigit(typedChar) || keyCode == Keyboard.KEY_SPACE) {
                text += typedChar;
                setting.setText(text);
            }

            SoundUtil.playSound("demise.tick");
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void drawTextWithLineBreaks(String text, float x, float y) {
        String[] lines = text.split("\n");
        float currentY = y;

        for (String line : lines) {
            Fonts.interRegular.get(15).drawString(line, x, currentY, ColorUtils.interpolateColor2(new Color(-1).darker(), new Color(-1), (float) input.getOutput()));
            currentY += Fonts.interRegular.get(15).getHeight();
        }
    }

    private void deleteLastCharacter() {
        if (!text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
            setting.setText(text);
        }
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }

    @Override
    public boolean isChild() {
        return this.setting.isChild();
    }
}
