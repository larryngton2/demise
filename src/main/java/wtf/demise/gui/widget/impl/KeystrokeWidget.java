package wtf.demise.gui.widget.impl;

import lombok.Getter;
import lombok.Setter;
import org.lwjglx.input.Keyboard;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.widget.Widget;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.StringUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class KeystrokeWidget extends Widget {
    private final Key w = new Key(new float[]{20, 0}, mc.gameSettings.keyBindForward.getKeyCode());
    private final Key a = new Key(new float[]{0, 20}, mc.gameSettings.keyBindLeft.getKeyCode());
    private final Key s = new Key(new float[]{20, 20}, mc.gameSettings.keyBindBack.getKeyCode());
    private final Key d = new Key(new float[]{40, 20}, mc.gameSettings.keyBindRight.getKeyCode());
    private final Key jump = new Key(new float[]{0, 40}, 57, mc.gameSettings.keyBindJump.getKeyCode());

    private final List<Key> keyList = Arrays.asList(w, a, s, d, jump);

    public KeystrokeWidget() {
        super("Keystrokes");

        this.width = 57;
        this.height = 57;
        this.x = 0.9342179f;
        this.y = 0.84583354f;
    }

    @Override
    public void render() {
        drawKeystrokes(false);
    }

    @Override
    public void onShader(Shader2DEvent event) {
        drawKeystrokes(true);
    }

    private void drawKeystrokes(boolean shader) {
        w.setPressed(mc.thePlayer.movementInput.moveForward > 0);
        a.setPressed(mc.thePlayer.movementInput.moveStrafe > 0);
        s.setPressed(mc.thePlayer.movementInput.moveForward < 0);
        d.setPressed(mc.thePlayer.movementInput.moveStrafe < 0);
        jump.setPressed(mc.thePlayer.movementInput.jump);

        for (Key key : keyList) {
            float x = renderX + key.getPos()[0];
            float y = renderY + key.getPos()[1];
            float width = key.getWidth();
            float height = key.getHeight();
            Color color;

            if (key.isPressed()) {
                x += 0.85f;
                y += 0.85f;
                width -= 1.7f;
                height -= 1.7f;

                Color c = new Color(setting.color()).darker().darker().darker().darker().darker();
                color = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) setting.bgAlpha.get());
            } else {
                color = new Color(setting.bgColor(), true);
            }

            key.setInterpolatedX(MathUtils.interpolate(key.getInterpolatedX(), x, 0.25f));
            key.setInterpolatedY(MathUtils.interpolate(key.getInterpolatedY(), y, 0.25f));
            key.setInterpolatedWidth(MathUtils.interpolate(key.getInterpolatedWidth(), width, 0.25f));
            key.setInterpolatedHeight(MathUtils.interpolate(key.getInterpolatedHeight(), height, 0.25f));

            key.setColor(ColorUtils.interpolateColorC(key.getColor(), color, 0.25f));

            if (!shader) {
                RoundedUtils.drawRound(key.getInterpolatedX(), key.getInterpolatedY(), key.getInterpolatedWidth(), key.getInterpolatedHeight(), 3, key.getColor());
                Fonts.interRegular.get(14).drawCenteredString(key.getName(), (key.getInterpolatedX() + key.getInterpolatedWidth() / 2), (key.getInterpolatedY() + key.getInterpolatedHeight() / 2) - 1.5, setting.color());
            } else {
                RoundedUtils.drawShaderRound(key.getInterpolatedX(), key.getInterpolatedY(), key.getInterpolatedWidth(), key.getInterpolatedHeight(), 3, Color.black);
            }
        }
    }

    @Override
    public boolean shouldRender() {
        return setting.isEnabled() && setting.elements.isEnabled("Keystrokes");
    }
}

@Getter
@Setter
class Key {
    private boolean isPressed;
    private int keyCode;
    private String name;
    private float[] pos;
    private float width;
    private float height;
    private float interpolatedWidth;
    private float interpolatedHeight;
    private float interpolatedX;
    private float interpolatedY;
    private float renderX;
    private float renderY;
    private Color color = new Color(0, 0, 0);

    public Key(float[] pos, int keyCode) {
        this.pos = pos;
        this.keyCode = keyCode;
        this.name = StringUtils.capitalizeWords(Keyboard.getKeyName(keyCode));
        this.width = 17;
        this.height = 17;
    }

    public Key(float[] pos, float width, int keyCode) {
        this.pos = pos;
        this.keyCode = keyCode;
        this.name = StringUtils.capitalizeWords(Keyboard.getKeyName(keyCode));
        this.width = width;
        this.height = 17;
    }
}