package net.minecraft.client.gui;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.IChatComponent;

import java.util.List;

public class GuiDisconnected extends GuiScreen {
    private final String reason;
    private final IChatComponent message;
    private List<String> multilineMessage;
    private final GuiScreen parentScreen;
    private int field_175353_i;

    public GuiDisconnected(GuiScreen screen, String reasonLocalizationKey, IChatComponent chatComp) {
        this.parentScreen = screen;
        this.reason = I18n.format(reasonLocalizationKey);
        this.message = chatComp;
    }

    protected void keyTyped(char typedChar, int keyCode) {
    }

    public void initGui() {
        this.buttonList.clear();
        this.multilineMessage = this.fontRendererObj.listFormattedStringToWidth(this.message.getFormattedText(), width - 50);
        this.field_175353_i = this.multilineMessage.size() * this.fontRendererObj.FONT_HEIGHT;
        this.buttonList.add(new GuiButton(0, (float) width / 2 - 100, (float) height / 2 + (float) this.field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT, I18n.format("gui.toMenu")));
    }

    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(this.parentScreen);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, this.reason, (float) width / 2, (float) height / 2 - (float) this.field_175353_i / 2 - this.fontRendererObj.FONT_HEIGHT * 2, 11184810);
        int i = height / 2 - this.field_175353_i / 2;

        if (this.multilineMessage != null) {
            for (String s : this.multilineMessage) {
                drawCenteredString(this.fontRendererObj, s, (float) width / 2, i, 16777215);
                i += this.fontRendererObj.FONT_HEIGHT;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
