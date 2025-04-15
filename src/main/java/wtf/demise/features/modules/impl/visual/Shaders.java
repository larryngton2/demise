package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.shader.Framebuffer;
import wtf.demise.events.impl.render.Shader2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.impl.Blur;
import wtf.demise.utils.render.shader.impl.Shadow;

@ModuleInfo(name = "Shaders", category = ModuleCategory.Visual)
public class Shaders extends Module {
    public final BoolValue blur = new BoolValue("Blur", true, this);
    public final BoolValue shadow = new BoolValue("Shadow", true, this);

    public static Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    public void renderShaders() {
        if (!this.isEnabled()) return;

        if (this.blur.get()) {
            RenderUtils.resetColor();
            Blur.startBlur();
            INSTANCE.getEventManager().call(new Shader2DEvent(Shader2DEvent.ShaderType.BLUR));
            Blur.endBlur(25, 1);
            RenderUtils.resetColor();
        }

        if (shadow.get()) {
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            RenderUtils.resetColor();
            INSTANCE.getEventManager().call(new Shader2DEvent(Shader2DEvent.ShaderType.SHADOW));
            stencilFramebuffer.unbindFramebuffer();
            RenderUtils.resetColor();

            Shadow.renderBloom(stencilFramebuffer.framebufferTexture, 50, 1);
        }
    }
}
