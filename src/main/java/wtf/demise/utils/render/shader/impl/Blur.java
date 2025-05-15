package wtf.demise.utils.render.shader.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.StencilUtils;
import wtf.demise.utils.render.shader.ShaderUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.glUniform1fv;

public class Blur implements InstanceAccess {
    private static final ShaderUtils gaussianBlur = new ShaderUtils("gaussianBlur");
    private static Framebuffer framebuffer = new Framebuffer(1, 1, false);
    private static final Map<Float, FloatBuffer> gaussianWeightCache = new HashMap<>();

    private static FloatBuffer getGaussianWeights(float radius) {
        return gaussianWeightCache.computeIfAbsent(radius, r -> {
            FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(256);
            for (int i = 0; i <= r; i++) {
                weightBuffer.put(MathUtils.calculateGaussianValue(i, r / 2));
            }
            weightBuffer.rewind();
            return weightBuffer;
        });
    }

    private static void setupUniforms(float dir1, float dir2, float radius) {
        gaussianBlur.setUniformi("textureIn", 0);
        gaussianBlur.setUniformf("texelSize", 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);
        gaussianBlur.setUniformf("direction", dir1, dir2);
        gaussianBlur.setUniformf("radius", radius);

        glUniform1fv(gaussianBlur.getUniform("weights"), getGaussianWeights(radius));
    }

    public static void startBlur() {
        StencilUtils.initStencilToWrite();
    }

    public static void endBlur(float radius, float compression) {
        StencilUtils.readStencilBuffer(1);
        framebuffer = RenderUtils.createFrameBuffer(framebuffer);

        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);
        applyBlurPass(compression, 0, radius, mc.getFramebuffer().framebufferTexture);
        framebuffer.unbindFramebuffer();

        mc.getFramebuffer().bindFramebuffer(false);
        applyBlurPass(0, compression, radius, framebuffer.framebufferTexture);

        StencilUtils.uninitStencilBuffer();
        RenderUtils.resetColor();
        GlStateManager.bindTexture(0);
    }

    private static void applyBlurPass(float dir1, float dir2, float radius, int texture) {
        gaussianBlur.init();
        setupUniforms(dir1, dir2, radius);
        RenderUtils.bindTexture(texture);
        ShaderUtils.drawQuads();
        gaussianBlur.unload();
    }
}