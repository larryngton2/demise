package wtf.demise.gui.font;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.floats.Float2ObjectArrayMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;
import wtf.demise.Demise;

import java.awt.*;
import java.io.InputStream;

public enum Fonts {
    interBold("inter/Inter_Bold"),
    interMedium("inter/Inter_Medium"),
    interRegular("inter/Inter_Regular"),
    interSemiBold("inter/Inter_SemiBold"),
    nursultan("others/Nursultan"),
    urbanist("urbanist/Urbanist");

    private final String file;
    private final Float2ObjectMap<FontRenderer> fontMap = new Float2ObjectArrayMap<>();

    public FontRenderer get(float size) {
        return this.fontMap.computeIfAbsent(size, font -> {
            try {
                return create(this.file, size, true);
            } catch (Exception var5) {
                throw new RuntimeException("Unable to load font: " + this, var5);
            }
        });
    }

    public FontRenderer get(float size, boolean antiAlias) {
        return this.fontMap.computeIfAbsent(size, font -> {
            try {
                return create(this.file, size, antiAlias);
            } catch (Exception var5) {
                throw new RuntimeException("Unable to load font: " + this, var5);
            }
        });
    }

    public FontRenderer create(String file, float size, boolean antiAlias) {
        Font font = null;

        try {
            InputStream in = Preconditions.checkNotNull(
                    Demise.class.getResourceAsStream("/assets/minecraft/" + Demise.INSTANCE.getClientName().toLowerCase() + "/font/" + file + ".ttf"), "Font resource is null"
            );
            font = Font.createFont(0, in).deriveFont(Font.PLAIN, size);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create font", ex);
        }
        if (font != null) {
            return new FontRenderer(font, antiAlias);
        } else {
            throw new RuntimeException("Failed to create font");
        }
    }

    Fonts(String file) {
        this.file = file;
    }
}
