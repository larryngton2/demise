package wtf.demise.gui.altmanager.repository;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import wtf.demise.gui.altmanager.group.GuiRoundedGroupWithLines;
import wtf.demise.gui.font.Fonts;

import java.awt.*;

public final class GuiGroupAltLogin extends GuiRoundedGroupWithLines<Object> {

    private String status = EnumChatFormatting.GRAY + "Idling...";

    public GuiGroupAltLogin(final GuiAddAlt gui, final String text) {
        super(text, 0, 15, 200, 30, 15, () -> null, Fonts.interMedium.get(20), Fonts.interSemiBold.get(22));

        this.xPosition = (int) ((GuiScreen.width - this.width) / 2F);
        addLine(t -> this.status);
    }

    @Override
    public void drawGroup(final Minecraft mc, final int mouseX, final int mouseY) {
        if (this.hidden) return;

        if (this.title != null) {
            this.titleFontRenderer.drawString(this.title,
                    this.xPosition + (this.width - this.titleFontRenderer.getStringWidth(this.title)) / 2.0F,
                    this.yPosition + 5,
                    new Color(198, 198, 198).getRGB());
        }

        drawLines();
    }

    @Override
    protected void drawLines() {
        final String text = this.lines.get(0).getText(null);
        this.lineFontRenderer.drawString(text,
                this.xPosition + (this.width - this.lineFontRenderer.getStringWidth(text)) / 2F,
                this.yPosition + this.lineFontRenderer.getHeight() + 11,
                new Color(198, 198, 198).getRGB());
    }

    public void updateStatus(String status) {
        this.status = status;
    }

}
