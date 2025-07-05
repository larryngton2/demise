package wtf.demise.features.modules.impl.visual;

import net.minecraft.util.ResourceLocation;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "MotionBlur", description = "Blurs your motion. (??)")
public class MotionBlur extends Module {
    public final SliderValue amount = new SliderValue("Amount", 1, 1, 10, 1, this);

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (mc.theWorld != null) {
            if (mc.entityRenderer.getShaderGroup() == null) {
                mc.entityRenderer.loadShader(new ResourceLocation("minecraft", "shaders/post/motion_blur.json"));
            } else {
                float uniform = 1F - Math.min(amount.get() / 10F, 0.9f);
                mc.entityRenderer.getShaderGroup().listShaders.get(0).getShaderManager().getShaderUniform("Phosphor").set(uniform, 0F, 0F);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.entityRenderer.isShaderActive()) {
            mc.entityRenderer.stopUseShader();
        }
    }
}