package wtf.demise.features.command.impl;

import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.features.config.impl.ModuleConfig;
import wtf.demise.utils.misc.ChatUtils;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigCommand extends Command {
    private enum Action {
        LOAD, SAVE, LIST, CREATE, REMOVE, OPENFOLDER, CURRENT;

        public static Optional<Action> fromString(String action) {
            try {
                return Optional.of(Action.valueOf(action.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
    }

    private record ConfigRequest(Action action, String configName) {
        public static ConfigRequest parse(String[] args) throws ConfigurationException {
            if (args.length < 2) {
                throw new ConfigurationException("Insufficient arguments");
            }

            Action action = Action.fromString(args[1]).orElseThrow(() -> new ConfigurationException("Invalid action"));

            String configName = args.length > 2 ? args[2] : null;
            if (requiresConfigName(action) && configName == null) {
                throw new ConfigurationException("Config name required for " + action.name().toLowerCase());
            }

            return new ConfigRequest(action, configName);
        }

        private static boolean requiresConfigName(Action action) {
            return action != Action.LIST && action != Action.OPENFOLDER && action != Action.CURRENT;
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
        try {
            ConfigRequest request = ConfigRequest.parse(args);
            handleRequest(request);
        } catch (ConfigurationException e) {
            ChatUtils.sendMessageClient(e.getMessage() + ". Usage: " + getUsage());
        }
    }

    private void handleRequest(ConfigRequest request) {
        switch (request.action()) {
            case LIST -> handleList();
            case OPENFOLDER -> handleOpenFolder();
            case CURRENT -> handleCurrent();
            case LOAD -> handleLoad(request.configName());
            case SAVE -> handleSave(request.configName(), true);
            case CREATE -> handleCreate(request.configName());
            case REMOVE -> handleRemove(request.configName());
        }
    }

    private void handleList() {
        List<String> configs = getConfigList();
        String message = configs.isEmpty() ? "No configurations found." : "Configs: " + String.join(", ", configs);
        ChatUtils.sendMessageClient(message);
    }

    private void handleOpenFolder() {
        if (!Desktop.isDesktopSupported()) {
            ChatUtils.sendMessageClient("Opening folder is not supported on this system.");
            return;
        }

        try {
            Desktop.getDesktop().open(Demise.INSTANCE.getMainDir());
            ChatUtils.sendMessageClient("Opened config folder.");
        } catch (IOException e) {
            ChatUtils.sendMessageClient("Failed to open config folder.");
            e.printStackTrace();
        }
    }

    private void handleCurrent() {
        String currentConfig = Demise.INSTANCE.getConfigManager().getCurrentConfig();
        String message = currentConfig != null ? "Current config: " + currentConfig : "No config is currently loaded.";
        ChatUtils.sendMessageClient(message);
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

    private void handleSave(String configName, boolean notify) {
        ModuleConfig cfg = new ModuleConfig(configName);
        boolean success = Demise.INSTANCE.getConfigManager().saveConfig(cfg);
        if (notify) {
            ChatUtils.sendMessageClient(success ? "Saved config: " + configName : "Failed to save config: " + configName);
        }
    }

    private void handleCreate(String configName) {
        File configFile = new File(Demise.INSTANCE.getMainDir(), configName + ".json");
        try {
            if (!configFile.createNewFile()) {
                ChatUtils.sendMessageClient("Config already exists: " + configName);
                return;
            }

            Demise.INSTANCE.getConfigManager().setCurrentConfig(configName);
            handleSave(configName, false);
            ChatUtils.sendMessageClient("Created and saved config: " + configName);
        } catch (IOException e) {
            ChatUtils.sendMessageClient("Failed to create config: " + configName);
            e.printStackTrace();
        }
    }

    private void handleRemove(String configName) {
        File configFile = new File(Demise.INSTANCE.getMainDir(), configName + ".json");
        if (!configFile.exists()) {
            ChatUtils.sendMessageClient("Config does not exist: " + configName);
            return;
        }

        String message = configFile.delete() ? "Removed config: " + configName : "Failed to remove config: " + configName;
        ChatUtils.sendMessageClient(message);
    }

    private List<String> getConfigList() {
        File directory = Demise.INSTANCE.getMainDir();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return List.of();
        }

        return Arrays.stream(files).filter(File::isFile).map(file -> file.getName().replace(".json", "")).collect(Collectors.toList());
    }

    private static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }
    }
}