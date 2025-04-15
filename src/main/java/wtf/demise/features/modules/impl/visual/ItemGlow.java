package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.shader.Framebuffer;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.impl.Shadow;

@ModuleInfo(name = "ItemGlow", category = ModuleCategory.Visual)
public class ItemGlow extends Module {
    public static Framebuffer handFramebuffer = new Framebuffer(1, 1, false);

    private void aa() {
        handFramebuffer = RenderUtils.createFrameBuffer(handFramebuffer, true);
        handFramebuffer.framebufferClear();
        handFramebuffer.bindFramebuffer(true);

        //renderHand(partialTicks, pass);

        handFramebuffer.unbindFramebuffer();
        Shadow.renderBloom(handFramebuffer.framebufferTexture, 50, 1);
    }
}
