package net.minecraft.client.renderer;

import lombok.Getter;
import net.optifine.SmartAnimations;

public class Tessellator {
    @Getter
    private final WorldRenderer worldRenderer;
    private final WorldVertexBufferUploader vboUploader = new WorldVertexBufferUploader();
    @Getter
    private static final Tessellator instance = new Tessellator(2097152);

    public Tessellator(int bufferSize) {
        this.worldRenderer = new WorldRenderer(bufferSize);
    }

    public void draw() {
        if (this.worldRenderer.animatedSprites != null) {
            SmartAnimations.spritesRendered(this.worldRenderer.animatedSprites);
        }

        this.worldRenderer.finishDrawing();
        this.vboUploader.draw(this.worldRenderer);
    }
}
