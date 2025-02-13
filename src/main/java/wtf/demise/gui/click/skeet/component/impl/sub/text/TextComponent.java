package wtf.demise.gui.click.skeet.component.impl.sub.text;

import wtf.demise.gui.click.skeet.LockedResolution;
import wtf.demise.gui.click.skeet.SkeetUI;
import wtf.demise.gui.click.skeet.component.Component;
import wtf.demise.gui.font.FontRenderer;

public class TextComponent extends Component
{
    private static final FontRenderer FONT_RENDERER = SkeetUI.FONT_RENDERER;
    private final String text;
    
    public TextComponent(final Component parent, final String text, final float x, final float y) {
        super(parent, x, y, FONT_RENDERER.getStringWidth(text), FONT_RENDERER.getHeight());
        this.text = text;
    }

    @Override
    public void drawComponent(final LockedResolution resolution, final int mouseX, final int mouseY) {
        if (SkeetUI.shouldRenderText()) {
            FONT_RENDERER.drawString(this.text, this.getX() - 1.5f, this.getY() + 1.42f, SkeetUI.getSkeetColor(0xE6E6E6));
        }
    }
}
