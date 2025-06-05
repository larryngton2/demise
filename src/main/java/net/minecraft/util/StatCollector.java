package net.minecraft.util;

import java.io.IOException;

public class StatCollector {
    private static final StringTranslate localizedName = StringTranslate.getInstance();
    private static final StringTranslate fallbackTranslator;

    static {
        try {
            fallbackTranslator = new StringTranslate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String translateToLocal(String key) {
        return localizedName.translateKey(key);
    }

    public static String translateToLocalFormatted(String key, Object... format) {
        return localizedName.translateKeyFormat(key, format);
    }

    public static String translateToFallback(String key) {
        return fallbackTranslator.translateKey(key);
    }

    public static boolean canTranslate(String key) {
        return localizedName.isKeyTranslated(key);
    }

    public static long getLastTranslationUpdateTimeInMilliseconds() {
        return localizedName.getLastUpdateTimeInMilliseconds();
    }
}
