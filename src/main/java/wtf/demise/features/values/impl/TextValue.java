package wtf.demise.features.values.impl;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.features.modules.Module;
import wtf.demise.features.values.Value;

import java.util.function.Supplier;

@Getter
@Setter
public class TextValue extends Value {
    private String text;
    private boolean onlyNumber;

    public TextValue(String name, String text, boolean onlyNumber, Module module, Supplier<Boolean> visible) {
        super(name, module, visible, true);
        this.text = text;
        this.onlyNumber = onlyNumber;
    }

    public TextValue(String name, String text, Module module, Supplier<Boolean> visible) {
        super(name, module, visible, true);
        this.text = text;
        this.onlyNumber = false;
    }

    public TextValue(String name, String text, boolean onlyNumber, Module module) {
        super(name, module, () -> true, false);
        this.text = text;
        this.onlyNumber = onlyNumber;
    }

    public TextValue(String name, String text, Module module) {
        super(name, module, () -> true, false);
        this.text = text;
    }

    public String get() {
        return text;
    }
}
