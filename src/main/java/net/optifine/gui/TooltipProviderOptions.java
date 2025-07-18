package net.optifine.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.optifine.Lang;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TooltipProviderOptions implements TooltipProvider {
    public Rectangle getTooltipBounds(GuiScreen guiScreen, int x, int y) {
        int i = GuiScreen.width / 2 - 150;
        int j = GuiScreen.height / 6 - 7;

        if (y <= j + 98) {
            j += 105;
        }

        int k = i + 150 + 150;
        int l = j + 84 + 10;
        return new Rectangle(i, j, k - i, l - j);
    }

    public boolean isRenderBorder() {
        return false;
    }

    public String[] getTooltipLines(GuiButton btn, int width) {
        if (!(btn instanceof IOptionControl ioptioncontrol)) {
            return null;
        } else {
            GameSettings.Options gamesettings$options = ioptioncontrol.getOption();
            return getTooltipLines(gamesettings$options.getEnumString());
        }
    }

    public static String[] getTooltipLines(String key) {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < 10; ++i) {
            String s = key + ".tooltip." + (i + 1);
            String s1 = Lang.get(s, null);

            if (s1 == null) {
                break;
            }

            list.add(s1);
        }

        if (list.size() <= 0) {
            return null;
        } else {
            return list.toArray(new String[0]);
        }
    }
}
