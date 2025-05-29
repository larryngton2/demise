package wtf.demise.features.modules.impl.movement;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Vec3;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.SimulatedPlayer;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "MoveHelper", description = "Assists you with movement in combat.", category = ModuleCategory.Movement)
public class MoveHelper extends Module {
    private final List<PlayerUtils.PredictProcess> selfPrediction = new ArrayList<>();

    @EventTarget
    public void onGameEvent(GameEvent e) {
        selfPrediction.clear();

        MovementInput movementInput = new MovementInput();

        movementInput.moveForward = mc.thePlayer.movementInput.moveForward;
        movementInput.moveStrafe = mc.thePlayer.movementInput.moveStrafe;
        movementInput.jump = true;
        movementInput.sneak = mc.thePlayer.movementInput.sneak;

        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(movementInput, 1, 11);

        simulatedSelf.rotationYaw = RotationUtils.currentRotation != null ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < 11; i++) {
            simulatedSelf.tick();
            selfPrediction.add(new PlayerUtils.PredictProcess(
                            simulatedSelf.getPos(),
                            simulatedSelf.fallDistance,
                            simulatedSelf.onGround,
                            simulatedSelf.isCollidedHorizontally,
                            simulatedSelf.player
                    )
            );
        }

        EntityLivingBase target = PlayerUtils.getTarget(64, false);

        if (target == null) return;

        AxisAlignedBB entityBoundingBox = target.getHitbox().offset(new Vec3(target.prevPosX, target.prevPosY, target.prevPosZ).multiply(11));
        PlayerUtils.PredictProcess predictProcess = selfPrediction.get(selfPrediction.size() - 1);

        double predictedDistance = PlayerUtils.getCustomDistanceToEntityBox(entityBoundingBox.getCenter(), predictProcess.player); //predictProcess.position.distanceTo(entityBoundingBox.getCenter());

        ChatUtils.sendMessageClient(target.getName() + " / " + predictedDistance + " blocks away");

        if (predictedDistance < 3 && predictProcess.fallDistance < 0) {
            mc.thePlayer.jump();
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        double x = selfPrediction.get(selfPrediction.size() - 1).position.xCoord - mc.getRenderManager().viewerPosX;
        double y = selfPrediction.get(selfPrediction.size() - 1).position.yCoord - mc.getRenderManager().viewerPosY;
        double z = selfPrediction.get(selfPrediction.size() - 1).position.zCoord - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
        AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
        RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1, 100), true).getRGB());
    }
}