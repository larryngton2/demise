package wtf.demise.features.command.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import wtf.demise.Demise;
import wtf.demise.features.command.Command;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.misc.HttpUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

public class OnlineConfigCommand extends Command {

    @Override
    public String getUsage() {
        return "onlineconfig/ocf <load> <config>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"onlineconfig", "ocf"};
    }

    @Override
    public void execute(String[] args) {
        if (!validateArgs(args)) {
            ChatUtils.sendMessageClient("Usage: " + getUsage());
            return;
        }

        if (args[1].equalsIgnoreCase("load")) {
            loadConfig(args[2]);
        }
    }

    private boolean validateArgs(String[] args) {
        return args.length >= 3 && args[1].equalsIgnoreCase("load");
    }

    private void loadConfig(String configName) {
        try {
            String normalizedConfigName = configName.toLowerCase(Locale.getDefault());
            Optional<JsonObject> config = fetchConfig(normalizedConfigName);

            config.ifPresent(jsonConfig -> {
                if (Demise.INSTANCE.getConfigManager().loadOnlineConfig(
                        Demise.INSTANCE.getConfigManager().getSetting(), jsonConfig)) {
                    ChatUtils.sendMessageClient("Loaded config: " + configName);
                } else {
                    ChatUtils.sendMessageClient("Invalid config: " + configName);
                }
            });
        } catch (IOException e) {
            ChatUtils.sendMessageClient("Failed to load config: " + e.getMessage());
        }
    }

    private Optional<JsonObject> fetchConfig(String configName) throws IOException {
        String url = Demise.INSTANCE.getCloud() + "/configs/" + configName + ".json";
        String response = HttpUtils.get(url);
        return Optional.of(new JsonParser().parse(response).getAsJsonObject());
    }
}