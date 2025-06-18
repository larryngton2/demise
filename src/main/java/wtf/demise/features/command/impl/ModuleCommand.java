package wtf.demise.features.command.impl;

import wtf.demise.features.command.Command;
import wtf.demise.features.command.CommandExecutionException;
import wtf.demise.features.modules.Module;
import wtf.demise.features.values.Value;
import wtf.demise.features.values.impl.*;
import wtf.demise.utils.misc.ChatUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ModuleCommand extends Command {
    private final Module module;
    private final Map<Class<? extends Value>, ValueHandler> valueHandlers;

    public ModuleCommand(Module module, List<Value> values) {
        this.module = module;
        this.valueHandlers = initializeValueHandlers();
    }

    private Map<Class<? extends Value>, ValueHandler> initializeValueHandlers() {
        Map<Class<? extends Value>, ValueHandler> handlers = new HashMap<>();
        handlers.put(BoolValue.class, this::handleBoolValue);
        handlers.put(ColorValue.class, this::handleColorValue);
        handlers.put(SliderValue.class, this::handleSliderValue);
        handlers.put(MultiBoolValue.class, this::handleMultiBoolValue);
        handlers.put(ModeValue.class, this::handleModeValue);
        handlers.put(TextValue.class, this::handleTextValue);
        return handlers;
    }

    @Override
    public String getUsage() {
        return String.format("%s <setting> <value>", module.getName().toLowerCase(Locale.getDefault()));
    }

    @Override
    public String[] getAliases() {
        return new String[]{module.getName()};
    }

    @Override
    public void execute(String[] args) throws CommandExecutionException {
        if (args.length == 1) {
            ChatUtils.sendMessageClient("Usage: " + getUsage());
            return;
        }

        Value value = module.getValue(args[1]);
        if (value == null) return;

        ValueHandler handler = valueHandlers.get(value.getClass());
        if (handler != null) {
            handler.handle(value, args);
        }
    }

    private void handleBoolValue(Value value, String[] args) {
        BoolValue boolValue = (BoolValue) value;
        boolean newValue = !boolValue.get();
        boolValue.set(newValue);
        sendToggleMessage(args[1], newValue);
    }

    private void handleColorValue(Value value, String[] args) {
        if (requiresAdditionalValue(args)) {
            ColorValue colorValue = (ColorValue) value;
            colorValue.set(new Color(Integer.parseInt(args[2])));
            sendValueSetMessage(args[1], colorValue.get());
        }
    }

    private void handleSliderValue(Value value, String[] args) {
        if (requiresAdditionalValue(args)) {
            SliderValue sliderValue = (SliderValue) value;
            sliderValue.setValue(Float.parseFloat(args[2]));
            sendValueSetMessage(args[1], sliderValue.get());
        }
    }

    private void handleMultiBoolValue(Value value, String[] args) {
        if (requiresAdditionalValue(args)) {
            MultiBoolValue multiBoolValue = (MultiBoolValue) value;
            multiBoolValue.getValues().stream().filter(
                    boolValue -> Objects.equals(boolValue.getName(), args[2])).findFirst().ifPresent(
                    boolValue -> {
                        boolean newValue = !boolValue.get();
                        boolValue.set(newValue);
                        sendValueSetMessage(args[1], boolValue.get());
                    });
        }
    }

    private void handleModeValue(Value value, String[] args) {
        if (requiresAdditionalValue(args)) {
            ModeValue modeValue = (ModeValue) value;
            modeValue.set(args[2]);
            sendValueSetMessage(args[1], modeValue.get());
        }
    }

    private void handleTextValue(Value value, String[] args) {
        if (requiresAdditionalValue(args)) {
            TextValue textValue = (TextValue) value;
            textValue.setText(args[2]);
            sendValueSetMessage(args[1], textValue.get());
        }
    }

    private boolean requiresAdditionalValue(String[] args) {
        if (args.length < 3) {
            ChatUtils.sendMessageClient(args[1].toLowerCase() + " <value>");
            return false;
        }
        return true;
    }

    private void sendToggleMessage(String setting, boolean newValue) {
        String message = String.format("%s %s was set to %s.", module.getName(), setting, newValue ? "§8true" : "§8false");
        ChatUtils.sendMessageClient(message);
    }

    private void sendValueSetMessage(String setting, Object value) {
        String message = String.format("%s %s was set to %s.", module.getName(), setting, value);
        ChatUtils.sendMessageClient(message);
    }

    @FunctionalInterface
    private interface ValueHandler {
        void handle(Value value, String[] args);
    }
}