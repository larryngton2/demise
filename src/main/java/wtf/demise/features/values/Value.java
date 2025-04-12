package wtf.demise.features.values;

import lombok.Getter;
import wtf.demise.features.modules.Module;

import java.awt.*;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
public abstract class Value {
    private final String name;
    @Getter
    public Supplier<Boolean> visible;
    public Color color = Color.WHITE;
    public boolean child;

    public Value(String name, Module module, Supplier<Boolean> visible, boolean child) {
        this.name = name;
        this.visible = visible;
        this.child = child;
        Optional.ofNullable(module).ifPresent(m -> m.addValue(this));
    }

    public Boolean canDisplay() {
        return this.visible.get();
    }
}