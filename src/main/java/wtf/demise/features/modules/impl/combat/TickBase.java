package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TimerManipulationEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.RotationUtils;
import wtf.demise.utils.player.SimulatedPlayer;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TickBase", category = ModuleCategory.Combat)
public class TickBase extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Future", "Past"}, "Future", this);
    public final SliderValue delay = new SliderValue("Delay", 50, 0, 1000, 50, this);
    public final SliderValue minActiveRange = new SliderValue("Min Active Range", 3f, 0.1f, 7f, 0.1f, this);
    public final SliderValue maxActiveRange = new SliderValue("Max Active Range", 7f, 0.1f, 7f, 0.1f, this);
    public final SliderValue maxTick = new SliderValue("Max Ticks", 4, 1, 20, this);
    public final BoolValue displayPredictPos = new BoolValue("Dislay Predict Pos", false, this);
    public final BoolValue check = new BoolValue("Check", false, this);
    public final BoolValue workWithBackTrack = new BoolValue("Work With Back Track", false, this);
    public TimerUtils timer = new TimerUtils();
    public int skippedTick = 0;
    private long shifted, previousTime;
    public boolean working;
    private boolean firstAnimation = true;
    public final List<PlayerUtils.PredictProcess> predictProcesses = new ArrayList<>();
    public EntityPlayer target;

    @Override
    public void onEnable() {
        shifted = 0;
        previousTime = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (workWithBackTrack.get()) {
            target = getModule(BackTrack.class).target;
        } else {
            target = PlayerUtils.getTarget(maxActiveRange.get() * 3);
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        setTag(mode.get());
        if (mode.is("Future")) {
            if (event.getState() == MotionEvent.State.PRE)
                return;

            if (target == null || predictProcesses.isEmpty() || shouldStop()) {
                return;
            }

            if (timer.hasTimeElapsed(delay.get())) {
                if (shouldStart()) {
                    firstAnimation = false;
                    while (skippedTick <= maxTick.get() && !shouldStop()) {
                        ++skippedTick;
                        try {
                            mc.runTick();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    timer.reset();
                }
            }
            working = false;
        }
    }

    @EventTarget
    public void onTimerManipulation(TimerManipulationEvent event) {
        if (mode.is("Past")) {

            if (target == null || predictProcesses.isEmpty() || shouldStop()) {
                return;
            }

            if (shouldStart() && timer.hasTimeElapsed(delay.get())) {
                shifted += event.getTime() - previousTime;
            }

            if (shifted >= maxTick.get() * (1000 / 20f)) {
                shifted = 0;
                timer.reset();
            }

            previousTime = event.getTime();
            event.setTime(event.getTime() - shifted);
        }
    }

    @EventTarget
    public void onMove(MoveEvent event) {

        predictProcesses.clear();

        SimulatedPlayer simulatedPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);

        simulatedPlayer.rotationYaw = RotationUtils.currentRotation != null ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < (skippedTick != 0 ? skippedTick : maxTick.get()); i++) {
            simulatedPlayer.tick();
            predictProcesses.add(new PlayerUtils.PredictProcess(
                    simulatedPlayer.getPos(),
                    simulatedPlayer.fallDistance,
                    simulatedPlayer.onGround,
                    simulatedPlayer.isCollidedHorizontally
            ));
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (displayPredictPos.get() && mc.gameSettings.thirdPersonView != 0) {
            double x = predictProcesses.get(predictProcesses.size() - 1).position.xCoord - mc.getRenderManager().viewerPosX;
            double y = predictProcesses.get(predictProcesses.size() - 1).position.yCoord - mc.getRenderManager().viewerPosY;
            double z = predictProcesses.get(predictProcesses.size() - 1).position.zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, false, true, Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1));
        }
    }

    public boolean shouldStart() {
        Vec3 targetPos = target.getPositionVector();
        if (workWithBackTrack.get())
            targetPos = getModule(BackTrack.class).realPosition;
        return predictProcesses.get((int) (maxTick.get() - 1)).position.distanceTo(target.getPositionVector()) <
                mc.thePlayer.getPositionVector().distanceTo(target.getPositionVector()) &&
                MathUtils.inBetween(minActiveRange.get(), maxActiveRange.get(), predictProcesses.get((int) (maxTick.get() - 1)).position.distanceTo(target.getPositionVector())) &&
                mc.thePlayer.canEntityBeSeen(target) &&
                target.canEntityBeSeen(mc.thePlayer) &&
                (RotationUtils.getRotationDifference(mc.thePlayer, target) <= 90 && check.get() || !check.get()) &&
                !predictProcesses.get((int) (maxTick.get() - 1)).isCollidedHorizontally;
    }

    public boolean shouldStop() {
        return mc.thePlayer.hurtTime != 0;
    }

    public boolean handleTick() {
        if (mode.is("Future")) {
            if (working || skippedTick < 0) return true;
            if (isEnabled() && skippedTick > 0) {
                --skippedTick;
                return true;
            }
        }
        return false;
    }

    public boolean freezeAnim() {
        if (skippedTick != 0) {
            if (!firstAnimation) {
                firstAnimation = true;
                return false;
            }
            return true;
        }
        return false;
    }
}