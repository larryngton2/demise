import net.minecraft.client.main.Main;

import java.util.Arrays;

public class Start {

    public static void main(String[] args) {
        //System.setProperty("org.lwjgl.librarypath", new File("../natives/" + (System.getProperty("os.name").startsWith("Windows") ? "windows" : "linux")).getAbsolutePath());

        Main.main(concat(new String[]{"--version", "demise", "--accessToken", "0", "--assetsDir", "assets", "--assetIndex", "1.8", "--userProperties", "{}"}, args));
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}