package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.TimerManipulationEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.legit.BackTrack;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.RotationUtils;
import wtf.demise.utils.player.SimulatedPlayer;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TickBase", description = "Abuses tick manipulation in order to be unpredictable to your target.", category = ModuleCategory.Combat)
public class TickBase extends Module {
    //public final ModeValue mode = new ModeValue("Mode", new String[]{"Future", "Past", "TimeTravel"}, "Future", this);
    private final SliderValue delay = new SliderValue("Delay", 50, 0, 1000, 50, this);
    private final SliderValue attackRange = new SliderValue("Attack range", 3f, 0.1f, 7f, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 7f, 0.1f, 7f, 0.1f, this);
    private final SliderValue maxTick = new SliderValue("Max ticks", 4, 1, 20, this);
    private final BoolValue renderPredictedTargetPos = new BoolValue("Render predicted target pos", false, this);
    private final SliderValue hurtTimeToStop = new SliderValue("HurtTime to stop (>)", 0, 0, 10, 1, this);
    private final BoolValue displayPredictPos = new BoolValue("Render predicted self pos", false, this);
    private final BoolValue useBacktrackPos = new BoolValue("Use backtrack pos", false, this);
    private final BoolValue teamCheck = new BoolValue("Team Check", false, this);

    private final TimerUtils timer = new TimerUtils();
    private int skippedTick = 0;
    private long shifted, previousTime;
    private final List<PlayerUtils.PredictProcess> selfPrediction = new ArrayList<>();
    private EntityPlayer target;
    private final TimerUtils futureTimer = new TimerUtils();

    @Override
    public void onEnable() {
        shifted = 0;
        previousTime = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        //setTag(mode.get());
        setTag(String.valueOf(maxTick.get()));

        target = PlayerUtils.getTarget(searchRange.get(), teamCheck.get());
    }

    @EventTarget
    public void onTimerManipulation(TimerManipulationEvent e) {
        //if (mode.is("Past")) {
            if (target == null || selfPrediction.isEmpty() || shouldStop()) {
                return;
            }

            if (shouldStart() && timer.hasTimeElapsed(delay.get())) {
                shifted += e.getTime() - previousTime;
            }

            if (shifted >= maxTick.get() * 50) {
                shifted = 0;
                timer.reset();
            }

            previousTime = e.getTime();
            e.setTime(e.getTime() - shifted);
        //}

        /*
        if (mode.is("Future")) {
            e.setTime(e.getTime() - shifted);

            if (target == null || selfPrediction.isEmpty() || shouldStop()) {
                return;
            }

            if (shouldStart() && timer.hasTimeElapsed(delay.get())) {
                shifted += e.getTime() - previousTime;
            }

            if (shifted >= maxTick.get() * 50) {
                shifted = 0;
                timer.reset();
            }

            previousTime = e.getTime();
        }

        if (mode.is("asdsd")) {
            if (target == null || selfPrediction.isEmpty() || shouldStop()) {
                return;
            }

            previousTime = e.getTime();

            if (!futureTimer.hasTimeElapsed(maxTick.get() * 50)) {
                shifted += e.getTime() - previousTime;

                timer.reset();

                previousTime = e.getTime();
                e.setTime(e.getTime() - shifted);
            }

            if (shouldStart() && timer.hasTimeElapsed(delay.get()) && futureTimer.hasTimeElapsed(maxTick.get() * 50)) {
                e.setTime(e.getTime() + (long) (maxTick.get() * 50));
                futureTimer.reset();
            }
        }

        //WARNING: THIS SHIT IS ASS
        if (mode.is("TimeTravel")) {
            if (target == null || selfPrediction.isEmpty() || shouldStop()) {
                return;
            }

            if (shouldStart() && timer.hasTimeElapsed(delay.get()) && futureTimer.hasTimeElapsed(maxTick.get() * 50)) {
                e.setTime(e.getTime() + (long) (maxTick.get() * 50));
                futureTimer.reset();
            }

            if (!futureTimer.hasTimeElapsed(maxTick.get() * 50)) {
                timer.reset();

                shifted += e.getTime() - previousTime;
                previousTime = e.getTime();
                e.setTime(e.getTime() - shifted);
            }

            if (shifted >= maxTick.get() * 50) {
                shifted = 0;
            }
        }

         */
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
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

        if (renderPredictedTargetPos.get()) {
            Vec3 prediction = target.getPositionVector().subtract(new Vec3(target.prevPosX, target.prevPosY, target.prevPosZ)).multiply(maxTick.get());

            AxisAlignedBB entityBoundingBox = target.getHitbox().offset(prediction);

            double x = entityBoundingBox.getCenter().xCoord - mc.getRenderManager().viewerPosX;
            double y = entityBoundingBox.minY - mc.getRenderManager().viewerPosY;
            double z = entityBoundingBox.getCenter().zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1, 100), true).getRGB());
        }
    }

    public boolean shouldStart() {
        Vec3 prediction;

        if (useBacktrackPos.get() && getModule(BackTrack.class).isEnabled()) {
            Vec3 realPos = BackTrack.realPosition;
            Vec3 realLastPos = BackTrack.realLastPos;

            prediction = realPos.subtract(new Vec3(realLastPos.xCoord, realLastPos.yCoord, realLastPos.zCoord)).multiply(maxTick.get());

        } else {
            prediction = target.getPositionVector().subtract(new Vec3(target.prevPosX, target.prevPosY, target.prevPosZ)).multiply(maxTick.get());
        }

        AxisAlignedBB entityBoundingBox = target.getHitbox().offset(prediction);

        double predictedTargetDistance = PlayerUtils.getCustomDistanceToEntityBox(entityBoundingBox.getCenter(), mc.thePlayer);
        double predictedSelfDistance = PlayerUtils.getCustomDistanceToEntityBox(selfPrediction.get((int) maxTick.get() - 1).position, target);

        return predictedSelfDistance < predictedTargetDistance &&
                predictedSelfDistance + 0.5657 * (maxTick.get() / 2) /* (mode.is("Past") ? 0.5657 * (maxTick.get() / 2) : 0) */ <= attackRange.get() &&
                predictedSelfDistance <= searchRange.get() &&
                PlayerUtils.getDistanceToEntityBox(target) >= attackRange.get() &&
                mc.thePlayer.canEntityBeSeen(target) &&
                target.canEntityBeSeen(mc.thePlayer) &&
                !selfPrediction.get((int) (maxTick.get() - 1)).isCollidedHorizontally;
    }

    public boolean shouldStop() {
        return mc.thePlayer.hurtTime > hurtTimeToStop.get();
    }
}