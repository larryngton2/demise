package wtf.demise.utils.fun;

import lombok.experimental.UtilityClass;
import java.io.IOException;

@UtilityClass
public class ShitUtils {
    public void crashJVM() {
        Object[] o = null;
        try {
            while (true) {
                Object[] newO = new Object[1];
                newO[0] = o;
                o = newO;
            }
        } finally {
            crashJVM();
        }
    }

    public void shutDown() throws RuntimeException, IOException {
        String shutdownCommand;
        String operatingSystem = System.getProperty("os.name");

        if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
            shutdownCommand = "shutdown -h now";
        } else if (operatingSystem.contains("Windows")) {
            shutdownCommand = "shutdown.exe -s -t 0";
        } else {
            throw new RuntimeException("Unsupported operating system.");
        }

        Runtime.getRuntime().exec(shutdownCommand);
        System.exit(0);
    }
}