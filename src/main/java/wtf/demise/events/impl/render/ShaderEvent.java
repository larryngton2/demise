package wtf.demise.events.impl.render;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.events.impl.Event;

@Getter
@Setter
public class ShaderEvent implements Event {
    private ShaderType shaderType;

    public ShaderEvent(ShaderType shaderType) {
        this.shaderType = shaderType;
    }

    public enum ShaderType {
        BLUR, SHADOW, GLOW
    }
}
