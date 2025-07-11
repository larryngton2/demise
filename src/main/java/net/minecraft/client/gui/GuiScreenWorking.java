package net.minecraft.client.gui;

import net.minecraft.util.IProgressUpdate;
import net.optifine.CustomLoadingScreen;
import net.optifine.CustomLoadingScreens;

public class GuiScreenWorking extends GuiScreen implements IProgressUpdate {
    private String field_146591_a = "";
    private String field_146589_f = "";
    private int progress;
    private boolean doneWorking;
    private final CustomLoadingScreen customLoadingScreen = CustomLoadingScreens.getCustomLoadingScreen();

    public void displaySavingString(String message) {
        this.resetProgressAndMessage(message);
    }

    public void resetProgressAndMessage(String message) {
        this.field_146591_a = message;
        this.displayLoadingString("Working...");
    }

    public void displayLoadingString(String message) {
        this.field_146589_f = message;
        this.setLoadingProgress(0);
    }

    public void setLoadingProgress(int progress) {
        this.progress = progress;
    }

    public void setDoneWorking() {
        this.doneWorking = true;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.doneWorking) {
            if (!mc.isConnectedToRealms()) {
                mc.displayGuiScreen(null);
            }
        } else {
            if (this.customLoadingScreen != null && mc.theWorld == null) {
                this.customLoadingScreen.drawBackground(width, height);
            } else {
                this.drawDefaultBackground();
            }

            if (this.progress > 0) {
                drawCenteredString(this.fontRendererObj, this.field_146591_a, (float) width / 2, 70, 16777215);
                drawCenteredString(this.fontRendererObj, this.field_146589_f + " " + this.progress + "%", (float) width / 2, 90, 16777215);
            }

            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }
}
