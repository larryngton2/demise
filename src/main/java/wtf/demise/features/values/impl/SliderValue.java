package wtf.demise.features.values.impl;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.features.modules.Module;
import wtf.demise.features.values.Value;

import java.util.function.Supplier;

public class SliderValue extends Value {
    @Setter
    private float value;
    @Getter
    private final float min;
    @Getter
    private final float max;
    @Getter
    private final float increment;

    public SliderValue(String name, float value, float min, float max, float increment, Module module, Supplier<Boolean> visible) {
        super(name, module, visible, true);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public SliderValue(String name, float value, float min, float max, Module module, Supplier<Boolean> visible) {
        super(name, module, visible, true);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = 1;
    }

    public SliderValue(String name, float value, float min, float max, float increment, Module module) {
        super(name, module, () -> true, false);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public SliderValue(String name, float value, float min, float max, Module module) {
        super(name, module, () -> true, false);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = 1;
    }

    public float get() {
        return value;
    }
}
