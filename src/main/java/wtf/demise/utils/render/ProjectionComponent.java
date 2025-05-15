package wtf.demise.utils.render;

import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vector3d;
import org.joml.Vector4d;
import org.lwjglx.opengl.Display;
import org.lwjglx.util.glu.GLU;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.utils.InstanceAccess;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ProjectionComponent implements InstanceAccess {
    private static final HashMap<Entity, Projection> nextProjections = new HashMap<>();
    private static HashMap<Entity, Projection> currentProjections = new HashMap<>();
    Executor threadPool = Executors.newFixedThreadPool(Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() - 1)));

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        threadPool.execute(() -> {
            final HashMap<Entity, Projection> newProjections = new HashMap<>();

            synchronized (nextProjections) {
                for (Map.Entry<Entity, Projection> map : nextProjections.entrySet()) {
                    Projection projection = map.getValue();
                    projection.position = project(map.getKey());

                    newProjections.put(map.getKey(), projection);
                }

                nextProjections.clear();
            }

            currentProjections = newProjections;
        });
    }

    public static Vector4d get(Entity entity) {
        if (entity == null) return null;

        if (!nextProjections.containsKey(entity)) {
            nextProjections.put(entity, new Projection());
        }

        Projection projection = currentProjections.get(entity);

        if (projection == null) {
            return null;
        }

        return projection.getPosition();
    }

    private Vector3d project(final int factor, final double x, final double y, final double z) {
        if (GLU.gluProject((float) x, (float) y, (float) z, ActiveRenderInfo.MODELVIEW, ActiveRenderInfo.PROJECTION, ActiveRenderInfo.VIEWPORT, ActiveRenderInfo.OBJECTCOORDS)) {
            return new Vector3d((ActiveRenderInfo.OBJECTCOORDS.get(0) / factor), ((Display.getHeight() - ActiveRenderInfo.OBJECTCOORDS.get(1)) / factor), ActiveRenderInfo.OBJECTCOORDS.get(2));
        }

        return null;
    }

    public Vector4d project(Entity entity) {
        final double renderX = mc.getRenderManager().renderPosX;
        final double renderY = mc.getRenderManager().renderPosY;
        final double renderZ = mc.getRenderManager().renderPosZ;

        final double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - renderX;
        final double y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks) - renderY;
        final double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - renderZ;
        final double width = (entity.width + 0.14) / 2;
        final double height = entity.height + (entity.isSneaking() ? -0.1D : 0.2D) + 0.01D;
        final AxisAlignedBB aabb = new AxisAlignedBB(x - width, y, z - width, x + width, y + height, z + width);
        final List<Vector3d> vectors = Arrays.asList(new Vector3d(aabb.minX, aabb.minY, aabb.minZ), new Vector3d(aabb.minX, aabb.maxY, aabb.minZ), new Vector3d(aabb.maxX, aabb.minY, aabb.minZ), new Vector3d(aabb.maxX, aabb.maxY, aabb.minZ), new Vector3d(aabb.minX, aabb.minY, aabb.maxZ), new Vector3d(aabb.minX, aabb.maxY, aabb.maxZ), new Vector3d(aabb.maxX, aabb.minY, aabb.maxZ), new Vector3d(aabb.maxX, aabb.maxY, aabb.maxZ));

        Vector4d position = null;
        for (Vector3d vector : vectors) {
            ScaledResolution scaledResolution = new ScaledResolution(mc);

            vector = project(scaledResolution.getScaleFactor(), vector.x, vector.y, vector.z);

            if (vector != null && vector.z >= 0.0D && vector.z < 1.0D) {
                if (position == null) {
                    position = new Vector4d(vector.x, vector.y, vector.z, 0.0D);
                }

                position = new Vector4d(Math.min(vector.x, position.x), Math.min(vector.y, position.y), Math.max(vector.x, position.z), Math.max(vector.y, position.w));
            }
        }

        return position;
    }

    @Getter
    private static class Projection {
        private Vector4d position;
    }
}