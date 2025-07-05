package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.shader.Framebuffer;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ColorValue;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.impl.Bloom;
import wtf.demise.utils.render.shader.impl.Blur;
import wtf.demise.utils.render.shader.impl.Shadow;

import java.awt.*;

@ModuleInfo(name = "Shaders", description = "Renders shaders in HUD elements.")
public class Shaders extends Module {
    public final BoolValue blur = new BoolValue("Blur", true, this);
    public final BoolValue shadow = new BoolValue("Shadow", true, this);
    private final BoolValue bloom = new BoolValue("Bloom", false, this);
    public final BoolValue syncColor = new BoolValue("Sync bloom color", true, this, bloom::get);
    public final ColorValue bloomColor = new ColorValue("Bloom color", Color.cyan, this, () -> bloom.get() && !syncColor.get());

    public static Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public void renderShaders() {
        if (!this.isEnabled()) return;

        if (blur.get()) {
            Blur.startBlur();
            INSTANCE.getEventManager().call(new ShaderEvent(ShaderEvent.ShaderType.BLUR));
            Blur.endBlur(25, 1);
            RenderUtils.resetColor();
        }

        if (bloom.get()) {
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(false);
            INSTANCE.getEventManager().call(new ShaderEvent(ShaderEvent.ShaderType.GLOW));
            stencilFramebuffer.unbindFramebuffer();

            Bloom.renderBlur(stencilFramebuffer.framebufferTexture, 3, 1);
            RenderUtils.resetColor();
        }

        if (shadow.get()) {
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            RenderUtils.resetColor();
            INSTANCE.getEventManager().call(new ShaderEvent(ShaderEvent.ShaderType.SHADOW));
            stencilFramebuffer.unbindFramebuffer();
            RenderUtils.resetColor();

            if (stencilFramebuffer.framebufferTexture > 0) {
                Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 50, 1);
            }
        }
    }
}
