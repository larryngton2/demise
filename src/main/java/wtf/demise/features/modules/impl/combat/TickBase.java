package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TimerManipulationEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.RotationUtils;
import wtf.demise.utils.player.SimulatedPlayer;
import wtf.demise.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TickBase", category = ModuleCategory.Combat)
public class TickBase extends Module {
    private final SliderValue delay = new SliderValue("Delay", 50, 0, 1000, 50, this);
    private final SliderValue attackRange = new SliderValue("Attack range", 3f, 0.1f, 7f, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 7f, 0.1f, 7f, 0.1f, this);
    private final SliderValue maxTick = new SliderValue("Max Ticks", 4, 1, 20, this);
    private final SliderValue hurtTimeToStop = new SliderValue("HurtTime to stop (>)", 0, 0, 10, 1, this);
    private final BoolValue displayPredictPos = new BoolValue("Dislay Predict Pos", false, this);
    private final BoolValue check = new BoolValue("Check", false, this);
    private final BoolValue teamCheck = new BoolValue("Team Check", false, this);
    private final TimerUtils timer = new TimerUtils();
    private long shifted, previousTime;
    private final List<PlayerUtils.PredictProcess> selfPrediction = new ArrayList<>();
    private EntityPlayer target;

    @Override
    public void onEnable() {
        shifted = 0;
        previousTime = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        target = PlayerUtils.getTarget(searchRange.get() * 3, teamCheck.get());
    }

    @EventTarget
    public void onTimerManipulation(TimerManipulationEvent e) {
        if (target == null || selfPrediction.isEmpty() || shouldStop()) {
            return;
        }

        if (shouldStart() && timer.hasTimeElapsed(delay.get())) {
            shifted += e.getTime() - previousTime;
        }

        if (shifted >= maxTick.get() * (1000 / 20f)) {
            shifted = 0;
            timer.reset();
        }

        previousTime = e.getTime();
        e.setTime(e.getTime() - shifted);
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        selfPrediction.clear();

        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput, 1);

        simulatedSelf.rotationYaw = RotationUtils.currentRotation != null ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < maxTick.get(); i++) {
            simulatedSelf.tick();
            selfPrediction.add(new PlayerUtils.PredictProcess(
                            simulatedSelf.getPos(),
                            simulatedSelf.fallDistance,
                            simulatedSelf.onGround,
                            simulatedSelf.isCollidedHorizontally
                    )
            );
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (displayPredictPos.get() && mc.gameSettings.thirdPersonView != 0) {
            double x = selfPrediction.get(selfPrediction.size() - 1).position.xCoord - mc.getRenderManager().viewerPosX;
            double y = selfPrediction.get(selfPrediction.size() - 1).position.yCoord - mc.getRenderManager().viewerPosY;
            double z = selfPrediction.get(selfPrediction.size() - 1).position.zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, false, true, Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1));
        }
    }

    public boolean shouldStart() {
        double predictedDistance = PlayerUtils.getCustomDistanceToEntityBox(selfPrediction.get((int) maxTick.get() - 1).position, target);

        return predictedDistance < PlayerUtils.getDistanceToEntityBox(target) &&
                predictedDistance <= attackRange.get() && predictedDistance <= searchRange.get() &&
                mc.thePlayer.canEntityBeSeen(target) &&
                target.canEntityBeSeen(mc.thePlayer) &&
                (RotationUtils.getRotationDifference(mc.thePlayer, target) <= 90 && check.get() || !check.get()) &&
                !selfPrediction.get((int) (maxTick.get() - 1)).isCollidedHorizontally;
    }

    public boolean shouldStop() {
        return mc.thePlayer.hurtTime > hurtTimeToStop.get();
    }
}
