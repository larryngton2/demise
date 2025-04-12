package wtf.demise.features.values.impl;

import wtf.demise.features.modules.Module;
import wtf.demise.features.values.Value;

import java.util.function.Supplier;

public class BoolValue extends Value {
    private boolean value;
    public float anim;

    public BoolValue(String name, boolean value, Module module, Supplier<Boolean> visible) {
        super(name, module, visible, true);
        this.value = value;
    }

    public BoolValue(String name, boolean value, Module module) {
        super(name, module, () -> true, false);
        this.value = value;
    }

    public BoolValue(String name, boolean value) {
        super(name, null, () -> true, false);
        this.value = value;
    }

    public boolean get() {
        return value;
    }

    public void toggle() {
        value = !value;
    }

    public void set(boolean value) {
        this.value = value;
    }
}
