package wtf.demise.features.command.impl;

import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.features.config.impl.ModuleConfig;
import wtf.demise.utils.misc.ChatUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigCommand extends Command {

    private enum Action {
        LOAD, SAVE, LIST, CREATE, REMOVE, OPENFOLDER, CURRENT;

        public static Action fromString(String action) {
            try {
                return Action.valueOf(action.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    @Override
    public String getUsage() {
        return "config/cf/preset <load/save/list/create/remove/openfolder/current> <config>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"config", "cf", "preset"};
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            ChatUtils.sendMessageClient("Usage: " + getUsage());
            return;
        }

        Action action = Action.fromString(args[1]);
        if (action == null) {
            ChatUtils.sendMessageClient("Invalid action. Usage: " + getUsage());
            return;
        }

        switch (action) {
            case LIST:
                handleList();
                break;
            case OPENFOLDER:
                handleOpenFolder();
                break;
            case CURRENT:
                handleCurrent();
                break;
            default:
                if (args.length < 3) {
                    ChatUtils.sendMessageClient("Action '" + action.name().toLowerCase() + "' requires an additional argument. Usage: " + getUsage());
                    return;
                }
                String configName = args[2];
                switch (action) {
                    case LOAD:
                        handleLoad(configName);
                        break;
                    case SAVE:
                        handleSave(configName, true);
                        break;
                    case CREATE:
                        handleCreate(configName);
                        break;
                    case REMOVE:
                        handleRemove(configName);
                        break;
                    default:
                        ChatUtils.sendMessageClient("Unknown action. Usage: " + getUsage());
                }
                break;
        }
    }

    private void handleList() {
        List<String> configs = getConfigList();
        if (configs.isEmpty()) {
            ChatUtils.sendMessageClient("No configurations found.");
        } else {
            ChatUtils.sendMessageClient("Configs: " + String.join(", ", configs));
        }
    }

    private void handleOpenFolder() {
        File directory = Demise.INSTANCE.getMainDir();
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(directory);
                ChatUtils.sendMessageClient("Opened config folder.");
            } catch (IOException e) {
                ChatUtils.sendMessageClient("Failed to open config folder.");
                e.printStackTrace();
            }
        } else {
            ChatUtils.sendMessageClient("Opening folder is not supported on this system.");
        }
    }

    private void handleCurrent() {
        String currentConfig = Demise.INSTANCE.getConfigManager().getCurrentConfig();
        if (currentConfig != null) {
            ChatUtils.sendMessageClient("Current config: " + currentConfig);
        } else {
            ChatUtils.sendMessageClient("No config is currently loaded.");
        }
    }

    private void handleLoad(String configName) {
        ModuleConfig cfg = new ModuleConfig(configName);
        if (Demise.INSTANCE.getConfigManager().loadConfig(cfg)) {
            Demise.INSTANCE.getConfigManager().setCurrentConfig(configName);
            ChatUtils.sendMessageClient("Loaded config: " + configName);
        } else {
            ChatUtils.sendMessageClient("Invalid config: " + configName);
        }
    }

    private void handleSave(String configName) {
        handleSave(configName, true);
    }

    /**
     * Saves the current configuration.
     *
     * @param configName The name of the configuration to save.
     * @param notify     Whether to send a success/failure message.
     */
    private void handleSave(String configName, boolean notify) {
        ModuleConfig cfg = new ModuleConfig(configName);
        if (Demise.INSTANCE.getConfigManager().saveConfig(cfg)) {
            if (notify) {
                ChatUtils.sendMessageClient("Saved config: " + configName);
            }
        } else {
            if (notify) {
                ChatUtils.sendMessageClient("Failed to save config: " + configName);
            }
        }
    }

    private void handleCreate(String configName) {
        File configFile = new File(Demise.INSTANCE.getMainDir(), configName + ".json");
        try {
            if (configFile.createNewFile()) {
                Demise.INSTANCE.getConfigManager().setCurrentConfig(configName);
                ChatUtils.sendMessageClient("Created config and set as current: " + configName);
                // Automatically save the newly created config
                handleSave(configName, false); // Pass false to avoid duplicate messages
                ChatUtils.sendMessageClient("Automatically saved config: " + configName);
            } else {
                ChatUtils.sendMessageClient("Config already exists: " + configName);
            }
        } catch (IOException e) {
            ChatUtils.sendMessageClient("Failed to create config: " + configName);
            e.printStackTrace();
        }
    }

    private void handleRemove(String configName) {
        File configFile = new File(Demise.INSTANCE.getMainDir(), configName + ".json");
        if (configFile.exists()) {
            if (configFile.delete()) {
                ChatUtils.sendMessageClient("Removed config: " + configName);
            } else {
                ChatUtils.sendMessageClient("Failed to remove config: " + configName);
            }
        } else {
            ChatUtils.sendMessageClient("Config does not exist: " + configName);
        }
    }

    private List<String> getConfigList() {
        File directory = Demise.INSTANCE.getMainDir();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return List.of();
        }
        return Arrays.stream(files)
                .filter(File::isFile)
                .map(file -> file.getName().replaceFirst("\\.json$", ""))
                .collect(Collectors.toList());
    }
}