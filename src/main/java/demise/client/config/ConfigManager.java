package demise.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import demise.client.main.demise;
import demise.client.module.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class responsible for choosing which config is currently being used
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class ConfigManager {
    private final File configDirectory = new File(Minecraft.getMinecraft().mcDataDir + File.separator + "keystrokes" + File.separator + "configs");

    @Getter
    private Config config;
    private final ArrayList<Config> configs = new ArrayList<>();

    public ConfigManager() {
        if (!configDirectory.isDirectory()) {
            configDirectory.mkdirs();
        }

        discoverConfigs();
        File defaultFile = new File(configDirectory, "default.bplus");
        this.config = new Config(defaultFile);

        if (!defaultFile.exists()) {
            save();
        }
    }

    /**
     * Function that checks if the config in a given file can be loaded
     * Check is done by simply trying to parse the file as if it contains json data
     *
     * @param file File you want to check for being outdated
     * @return boolean whether the file is outdated
     */
    @SuppressWarnings("unused")
    public static boolean isOutdated(File file) {
        JsonParser jsonParser = new JsonParser();
        try (FileReader reader = new FileReader(file)) {
            Object obj = jsonParser.parse(reader);
            JsonObject data = (JsonObject) obj;
            return false;
        } catch (JsonSyntaxException | ClassCastException | IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Parses through all the files in the cfg dir and creates a new config class for each one
     */
    public void discoverConfigs() {
        configs.clear();
        if (configDirectory.listFiles() == null || !(Objects.requireNonNull(configDirectory.listFiles()).length > 0))
            return;  //nothing to discover if there are no files in the directory

        for (File file : Objects.requireNonNull(configDirectory.listFiles())) {
            if (file.getName().endsWith(".bplus")) {
                if (!isOutdated(file)) {
                    configs.add(new Config(
                            new File(file.getPath())
                    ));
                }
            }
        }
    }

    public void save() {
        JsonObject data = new JsonObject();
        data.addProperty("version", demise.versionManager.getClientVersion().getVersion());
        data.addProperty("author", "Unknown");
        data.addProperty("notes", "");
        data.addProperty("intendedServer", "");
        data.addProperty("usedFor", 0);
        data.addProperty("lastEditTime", System.currentTimeMillis());

        JsonObject modules = new JsonObject();
        for (Module module : demise.moduleManager.getModules()) {
            modules.add(module.getName(), module.getConfigAsJson());
        }
        data.add("modules", modules);

        config.save(data);
    }

    public void setConfig(Config config) {
        this.config = config;
        JsonObject data = config.getData().get("modules").getAsJsonObject();
        List<Module> knownModules = new ArrayList<>(demise.moduleManager.getModules());
        for (Module module : knownModules) {
            if (data.has(module.getName())) {
                module.applyConfigFromJson(
                        data.get(module.getName()).getAsJsonObject()
                );
            } else {
                module.resetToDefaults();
            }
        }
    }

    public void loadConfigByName(String replace) {
        discoverConfigs(); // re-parsing the config folder to make sure we know which configs exist
        for (Config config : configs) {
            if (config.getName().equals(replace))
                setConfig(config);
        }
    }

    public ArrayList<Config> getConfigs() {
        discoverConfigs();
        return configs;
    }

    public void copyConfig(Config config, String s) {
        File file = new File(configDirectory, s);
        Config newConfig = new Config(file);
        newConfig.save(config.getData());
    }

    public void resetConfig() {
        for (Module module : demise.moduleManager.getModules())
            module.resetToDefaults();
        save();
    }

    public void deleteConfig(Config config) {
        config.file.delete();
        if (config.getName().equals(this.config.getName())) {
            discoverConfigs();
            if (this.configs.size() < 2) {
                this.resetConfig();
                File defaultFile = new File(configDirectory, "default.bplus");
                this.config = new Config(defaultFile);
                save();
            } else {
                this.config = this.configs.get(0);
            }

            this.save();
        }
    }
}