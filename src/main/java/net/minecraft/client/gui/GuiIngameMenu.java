package net.minecraft.client.gui;

import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridge;
import wtf.demise.gui.mainmenu.GuiMainMenu;

public class GuiIngameMenu extends GuiScreen {
    public void initGui() {
        this.buttonList.clear();
        int i = -16;

        float initialY = ((float) height / 2) - ((float) 98 / 2) - 5;

        this.buttonList.add(new GuiButton(1, (float) width / 2 - 100, initialY + 96 + i, I18n.format("menu.returnToMenu")));

        if (!mc.isIntegratedServerRunning()) {
            this.buttonList.get(0).displayString = I18n.format("menu.disconnect");
        }

        this.buttonList.add(new GuiButton(4, (float) width / 2 - 100, initialY + 24 + i, I18n.format("menu.returnToGame")));
        this.buttonList.add(new GuiButton(5, (float) width / 2 - 100, initialY + 48 + i, 98, 20, I18n.format("gui.achievements")));
        this.buttonList.add(new GuiButton(6, (float) width / 2 + 2, initialY + 48 + i, 98, 20, I18n.format("gui.stats")));
        this.buttonList.add(new GuiButton(0, (float) width / 2 - 100, initialY + 72 + i, 98, 20, I18n.format("menu.options")));
        GuiButton guibutton;
        this.buttonList.add(guibutton = new GuiButton(7, (float) width / 2 + 2, initialY + 72 + i, 98, 20, I18n.format("menu.shareToLan")));
        guibutton.enabled = mc.isSingleplayer() && !mc.getIntegratedServer().getPublic();
    }

    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;

            case 1:
                boolean flag = mc.isIntegratedServerRunning();
                boolean flag1 = mc.isConnectedToRealms();
                button.enabled = false;
                mc.theWorld.sendQuittingDisconnectingPacket();
                mc.loadWorld(null);

                if (flag) {
                    mc.displayGuiScreen(new GuiMainMenu());
                } else if (flag1) {
                    RealmsBridge realmsbridge = new RealmsBridge();
                    realmsbridge.switchToRealms(new GuiMainMenu());
                } else {
                    mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
                }

            case 2:
            case 3:
            default:
                break;

            case 4:
                mc.displayGuiScreen(null);
                mc.setIngameFocus();
                break;

            case 5:
                mc.displayGuiScreen(new GuiAchievements(this, mc.thePlayer.getStatFileWriter()));
                break;

            case 6:
                mc.displayGuiScreen(new GuiStats(this, mc.thePlayer.getStatFileWriter()));
                break;

            case 7:
                mc.displayGuiScreen(new GuiShareToLan(this));
        }
    }

    public void updateScreen() {
        super.updateScreen();
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, I18n.format("menu.game"), (float) width / 2, (float) height / 2 - 65, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
