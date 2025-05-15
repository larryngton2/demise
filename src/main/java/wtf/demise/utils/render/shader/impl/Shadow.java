package wtf.demise.utils.render.shader.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.ShaderUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUniform1fv;

public class Shadow implements InstanceAccess {
    public static ShaderUtils bloomShader = new ShaderUtils("shadow");
    public static Framebuffer bloomFramebuffer = new Framebuffer(1, 1, false);
    public static float prevRadius;
    private static float[] cachedWeights;
    private static int cachedRadius = -1;
    private static final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(256);

    public static void renderBloom(int sourceTexture, int radius, int offset) {
        bloomFramebuffer = RenderUtils.createFrameBuffer(bloomFramebuffer, true);

        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, 0);
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);

        bloomFramebuffer.framebufferClear();
        bloomFramebuffer.bindFramebuffer(true);
        bloomShader.init();
        setupUniforms(radius, offset, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sourceTexture);
        ShaderUtils.drawQuads();

        mc.getFramebuffer().bindFramebuffer(true);
        setupUniforms(radius, 0, offset);
        GL13.glActiveTexture(GL13.GL_TEXTURE16);
        glBindTexture(GL_TEXTURE_2D, sourceTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, bloomFramebuffer.framebufferTexture);
        ShaderUtils.drawQuads();

        bloomShader.unload();
        GlStateManager.bindTexture(0);
    }

    public static void setupUniforms(int radius, int directionX, int directionY) {
        if (radius != prevRadius) {
            weightBuffer.clear();
            for (int i = 0; i <= radius; i++) {
                weightBuffer.put(MathUtils.calculateGaussianValue(i, radius));
            }
            weightBuffer.flip();

            bloomShader.setUniformi("inTexture", 0);
            bloomShader.setUniformi("textureToCheck", 16);
            bloomShader.setUniformf("radius", radius);
            bloomShader.setUniformf("weights", generateGaussianWeights(radius, 4));
            glUniform1fv(bloomShader.getUniform("weights"), weightBuffer);
            prevRadius = radius;
        }

        bloomShader.setUniformf("texelSize", 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);
        bloomShader.setUniformf("direction", directionX, directionY);
    }

    public static float[] generateGaussianWeights(int radius, float sigma) {
        if (radius == cachedRadius && cachedWeights != null) {
            return cachedWeights;
        }

        float[] weights = new float[256];
        float sum = 0.0f;

        float twoSigmaSquared = 2 * sigma * sigma;

        for (int i = 0; i <= radius; i++) {
            float weight = (float) Math.exp(-(i * i) / twoSigmaSquared);
            weights[i] = weight;
            sum += (i == 0) ? weight : 2 * weight;
        }

        float invSum = 1.0f / sum;
        for (int i = 0; i <= radius; i++) {
            weights[i] *= invSum;
        }

        cachedWeights = weights;
        cachedRadius = radius;
        return weights;
    }
}