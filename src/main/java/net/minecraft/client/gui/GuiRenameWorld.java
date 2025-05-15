package net.minecraft.client.gui;

import net.minecraft.client.resources.I18n;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import org.lwjglx.input.Keyboard;

import java.io.IOException;

public class GuiRenameWorld extends GuiScreen {
    private final GuiScreen parentScreen;
    private GuiTextField field_146583_f;
    private final String saveName;

    public GuiRenameWorld(GuiScreen parentScreenIn, String saveNameIn) {
        this.parentScreen = parentScreenIn;
        this.saveName = saveNameIn;
    }

    public void updateScreen() {
        this.field_146583_f.updateCursorCounter();
    }

    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, (float) width / 2 - 100, (float) height / 4 + 96 + 12, I18n.format("selectWorld.renameButton")));
        this.buttonList.add(new GuiButton(1, (float) width / 2 - 100, (float) height / 4 + 120 + 12, I18n.format("gui.cancel")));
        ISaveFormat isaveformat = mc.getSaveLoader();
        WorldInfo worldinfo = isaveformat.getWorldInfo(this.saveName);
        String s = worldinfo.getWorldName();
        this.field_146583_f = new GuiTextField(2, this.fontRendererObj, width / 2 - 100, 60, 200, 20);
        this.field_146583_f.setFocused(true);
        this.field_146583_f.setText(s);
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    protected void actionPerformed(GuiButton button) {
        if (button.enabled) {
            if (button.id == 1) {
                mc.displayGuiScreen(this.parentScreen);
            } else if (button.id == 0) {
                ISaveFormat isaveformat = mc.getSaveLoader();
                isaveformat.renameWorld(this.saveName, this.field_146583_f.getText().trim());
                mc.displayGuiScreen(this.parentScreen);
            }
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.field_146583_f.textboxKeyTyped(typedChar, keyCode);
        this.buttonList.get(0).enabled = !this.field_146583_f.getText().trim().isEmpty();

        if (keyCode == 28 || keyCode == 156) {
            this.actionPerformed(this.buttonList.get(0));
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.field_146583_f.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, I18n.format("selectWorld.renameTitle"), (float) width / 2, 20, 16777215);
        this.drawString(this.fontRendererObj, I18n.format("selectWorld.enterName"), width / 2 - 100, 47, 10526880);
        this.field_146583_f.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
