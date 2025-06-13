package wtf.demise.gui.click.panel.components.config;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.Demise;
import wtf.demise.features.config.impl.ModuleConfig;
import wtf.demise.gui.click.IComponent;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.render.ColorUtils;
import wtf.demise.utils.render.MouseUtils;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.Color;
import java.io.File;
import java.util.Objects;

@Getter
@Setter
public class ConfigComponent implements IComponent {
    private float x, y;
    private boolean isHovered, saveHovered, deleteHovered;
    private Color interpolatedColor = new Color(20, 20, 20, 150);
    private Color interpolatedColor1 = new Color(0, 0, 0, 0);
    public boolean visible;
    private float slideProgress = 0f;
    private String name;

    public ConfigComponent(String name) {
        this.name = name;
    }

    public void initCategory() {
        slideProgress = 0;
    }

    public void render(boolean shader) {
        float width = 375;
        slideProgress = MathUtils.interpolate(slideProgress, visible ? 1 : 0, 0.1f);
        float slideOffset = (width / 4) * (1.0f - slideProgress);

        if (!shader) {
            if (isHovered) {
                interpolatedColor = ColorUtils.interpolateColorC(interpolatedColor, new Color(35, 35, 35, 190), 0.1f);
            } else {
                interpolatedColor = ColorUtils.interpolateColorC(interpolatedColor, new Color(20, 20, 20, 150), 0.1f);
            }

            if (Objects.equals(Demise.INSTANCE.getConfigManager().getCurrentConfig(), name)) {
                interpolatedColor1 = ColorUtils.interpolateColorC(interpolatedColor1, new Color(50, 50, 50, 150), 0.1f);
            } else {
                interpolatedColor1 = ColorUtils.interpolateColorC(interpolatedColor1, new Color(0, 0, 0, 0), 0.1f);
            }

            RoundedUtils.drawRound(x + slideOffset, y, width, 30, 8, interpolatedColor);
            RoundedUtils.drawRound(x + slideOffset, y, width, 30, 8, interpolatedColor1);

            Fonts.interRegular.get(20).drawString(name, x + 7 + slideOffset, y + 11, Color.white.getRGB());

            float saveWidth = Fonts.interRegular.get(14).getStringWidth("save");
            float deleteWidth = Fonts.interRegular.get(14).getStringWidth("delete");

            Color saveColor = saveHovered ? new Color(255, 255, 255) : new Color(179, 179, 179);
            Color deleteColor = deleteHovered ? new Color(255, 255, 255) : new Color(179, 179, 179);

            Fonts.interRegular.get(14).drawString("save", x + width - 10 - deleteWidth - 5 - saveWidth + slideOffset, y + 13, saveColor.getRGB());
            Fonts.interRegular.get(14).drawString("delete", x + width - 10 - deleteWidth + slideOffset, y + 13, deleteColor.getRGB());
        } else {
            RoundedUtils.drawShaderRound(x + slideOffset, y, width, 30, 8, Color.black);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        float width = 375;
        float slideOffset = (width / 4) * (1.0f - slideProgress);
        float saveWidth = Fonts.interRegular.get(14).getStringWidth("save");
        float deleteWidth = Fonts.interRegular.get(14).getStringWidth("delete");

        this.isHovered = MouseUtils.isHovered(x + slideOffset, y, width - 15 - deleteWidth - 15 - saveWidth, 30, mouseX, mouseY);
        this.saveHovered = MouseUtils.isHovered(x + width - 10 - deleteWidth - 5 - saveWidth + slideOffset, y, saveWidth, 30, mouseX, mouseY);
        this.deleteHovered = MouseUtils.isHovered(x + width - 10 - deleteWidth + slideOffset, y, deleteWidth, 30, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (visible && mouseButton == 0) {
            if (isHovered) {
                if (Demise.INSTANCE.getConfigManager().loadConfig(new ModuleConfig(name))) {
                    Demise.INSTANCE.getConfigManager().setCurrentConfig(name);
                    ChatUtils.sendMessageClient("Loaded config " + name);
                } else {
                    ChatUtils.sendMessageClient("Failed to load config " + name + "!");
                }
            } else if (saveHovered) {
                if (Demise.INSTANCE.getConfigManager().saveConfig(new ModuleConfig(name))) {
                    ChatUtils.sendMessageClient("Saved config " + name);
                } else {
                    ChatUtils.sendMessageClient("Failed to save config " + name + "!");
                }
            } else if (deleteHovered) {
                File configFile = new File(Demise.INSTANCE.getMainDir(), name + ".json");
                if (!configFile.exists()) {
                    ChatUtils.sendMessageClient("Config does not exist: " + name);
                    return;
                }

                String message = configFile.delete() ? "Removed config: " + name : "Failed to remove config: " + name;
                ChatUtils.sendMessageClient(message);
            }
        }
    }
}