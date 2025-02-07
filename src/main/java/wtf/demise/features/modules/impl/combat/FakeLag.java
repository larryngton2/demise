package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

@ModuleInfo(name = "FakeLag (no worky)", category = ModuleCategory.Combat)
public class FakeLag extends Module {
    private final SliderValue rangeMin = new SliderValue("Range (min)", 3, 0, 8, 0.1f, this);
    private final SliderValue rangeMax = new SliderValue("Range (max)", 4.5f, 0, 8, 0.1f, this);
    private final SliderValue msMin = new SliderValue("Ms (min)", 100, 0, 1000, 1, this);
    private final SliderValue msMax = new SliderValue("Ms (max)", 250, 0, 1000, 1, this);
    private final BoolValue botCheck = new BoolValue("Bot check", true, this);

    private boolean lagging = false;
    private long lastLagTime;
    private Vec3 pos;

    @Override
    public void onEnable() {
        lastLagTime = System.currentTimeMillis();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        EntityLivingBase target = findTarget();

        if (System.currentTimeMillis() - lastLagTime >= MathUtils.nextDouble(msMin.get(), msMax.get())) {
            if (lagging) {
                PingSpoofComponent.dispatch();
                pos = null;
                lagging = false;
                PingSpoofComponent.blink();
                pos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                lagging = true;
                lastLagTime = System.currentTimeMillis();
                return;
            }
        }

        if (target != null) {
            if (!lagging) {
                PingSpoofComponent.blink();
                pos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                lagging = true;
                lastLagTime = System.currentTimeMillis();
            }
        } else {
            PingSpoofComponent.dispatch();
            pos = null;
            lagging = false;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (pos != null) {
            double x = pos.xCoord - mc.getRenderManager().viewerPosX;
            double y = pos.yCoord - mc.getRenderManager().viewerPosY;
            double z = pos.zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, true, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(0)).getRGB());
        }
    }

    public EntityLivingBase findTarget() {
        EntityLivingBase target = null;
        double closestDistance = rangeMax.get() + 0.4;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            double distanceToEntity = mc.thePlayer.getDistanceToEntity(entity);

            if (entity instanceof EntityPlayer && entity != mc.thePlayer && !Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity) && distanceToEntity <= rangeMax.get() + 0.4 && distanceToEntity >= rangeMin.get() + 0.4) {
                if (botCheck.get() && getModule(AntiBot.class).isBot((EntityPlayer) entity)) {
                    continue;
                }

                if (distanceToEntity < closestDistance) {
                    target = (EntityLivingBase) entity;
                    closestDistance = distanceToEntity;
                }
            }
        }
        return target;
    }
}