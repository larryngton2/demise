package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Vec3;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.TimerManipulationEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
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
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.SimulatedPlayer;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TickBase", description = "Abuses tick manipulation in order to be unpredictable to your target.", category = ModuleCategory.Combat)
public class TickBase extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Future", "Past"}, "Future", this);
    public final BoolValue passthroughClicking = new BoolValue("Pass through clicking", true, this, () -> mode.is("Future"));
    private final SliderValue delay = new SliderValue("Delay", 50, 0, 1000, 50, this);
    private final SliderValue tickRange = new SliderValue("Tick range", 3f, 0.1f, 8f, 0.1f, this);
    private final SliderValue minRange = new SliderValue("Min range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue stopRange = new SliderValue("Stop range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 7f, 0.1f, 15, 0.1f, this);
    private final SliderValue maxTick = new SliderValue("Max ticks", 4, 1, 20, this);
    private final BoolValue prioritiseCrits = new BoolValue("Prioritise crits", false, this);
    private final SliderValue hurtTimeToStop = new SliderValue("HurtTime to stop (>)", 0, 0, 10, 1, this);
    private final SliderValue targetPredictionTicks = new SliderValue("Target prediction ticks", 4, 0, 20, 1, this);
    private final BoolValue renderPredictedTargetPos = new BoolValue("Render predicted target pos", false, this);
    private final BoolValue renderPredictedSelfPos = new BoolValue("Render predicted self pos", false, this);
    private final BoolValue useBacktrackPos = new BoolValue("Use backtrack pos", false, this);
    private final BoolValue teamCheck = new BoolValue("Team Check", false, this);

    private final TimerUtils timer = new TimerUtils();
    private int skippedTick = 0;
    private long shifted, previousTime;
    private final List<PlayerUtils.PredictProcess> selfPrediction = new ArrayList<>();
    private EntityPlayer target;
    private boolean firstAnimation = true;
    public boolean working;
    private int ticksToSkip;

    @Override
    public void onEnable() {
        shifted = 0;
        previousTime = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        target = PlayerUtils.getTarget(searchRange.get(), teamCheck.get());
    }

    @EventTarget
    public void onTimerManipulation(TimerManipulationEvent e) {
        if (mode.is("Past")) {
            if (target == null || selfPrediction.isEmpty() || shouldStop()) {
                return;
            }

            if (shouldStart() && timer.hasTimeElapsed(delay.get())) {
                shifted += e.getTime() - previousTime;
            }

            if (shifted >= ticksToSkip * 50L) {
                shifted = 0;
                timer.reset();
            }

            previousTime = e.getTime();
            e.setTime(e.getTime() - shifted);
        }
    }

    @EventTarget
    public void onGame(GameEvent e) {
        if (mode.is("Future")) {
            if (target == null || selfPrediction.isEmpty() || shouldStop()) {
                return;
            }

            if (timer.hasTimeElapsed(delay.get())) {
                if (shouldStart()) {
                    firstAnimation = false;
                    while (skippedTick < ticksToSkip && !shouldStop()) {
                        working = true;
                        skippedTick++;
                        try {
                            mc.runTick();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    timer.reset();
                }
            }

            working = false;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        selfPrediction.clear();

        MovementInput movementInput = new MovementInput();

        movementInput.moveForward = mc.thePlayer.movementInput.moveForward;
        movementInput.moveStrafe = 0;
        movementInput.jump = mc.thePlayer.movementInput.jump;
        movementInput.sneak = mc.thePlayer.movementInput.sneak;

        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(movementInput, 1);

        simulatedSelf.rotationYaw = RotationManager.currentRotation != null ? RotationManager.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < maxTick.get(); i++) {
            simulatedSelf.tick();

            PlayerUtils.PredictProcess predictProcess = new PlayerUtils.PredictProcess(
                    simulatedSelf.getPos(),
                    simulatedSelf.fallDistance,
                    simulatedSelf.onGround,
                    simulatedSelf.isCollidedHorizontally,
                    simulatedSelf.player
            );

            predictProcess.tick = i;

            selfPrediction.add(predictProcess);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (renderPredictedSelfPos.get() && mc.gameSettings.thirdPersonView != 0) {
            double x = selfPrediction.get(selfPrediction.size() - 1).position.xCoord - mc.getRenderManager().viewerPosX;
            double y = selfPrediction.get(selfPrediction.size() - 1).position.yCoord - mc.getRenderManager().viewerPosY;
            double z = selfPrediction.get(selfPrediction.size() - 1).position.zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1, 100), true).getRGB());
        }

        if (renderPredictedTargetPos.get()) {
            Vec3 prediction = target.getPositionVector().subtract(new Vec3(target.prevPosX, target.prevPosY, target.prevPosZ)).multiply(targetPredictionTicks.get());

            AxisAlignedBB entityBoundingBox = target.getHitbox().offset(prediction);

            double x = entityBoundingBox.getCenter().xCoord - mc.getRenderManager().viewerPosX;
            double y = entityBoundingBox.minY - mc.getRenderManager().viewerPosY;
            double z = entityBoundingBox.getCenter().zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1, 100), true).getRGB());
        }
    }

    private Vec3 getTargetPrediction() {
        if (useBacktrackPos.get() && getModule(BackTrack.class).isEnabled()) {
            Vec3 realPos = BackTrack.realPosition;
            Vec3 realLastPos = BackTrack.realLastPos;
            return realPos.subtract(new Vec3(realLastPos.xCoord, realLastPos.yCoord, realLastPos.zCoord))
                    .multiply(targetPredictionTicks.get());
        }
        return target.getPositionVector()
                .subtract(new Vec3(target.prevPosX, target.prevPosY, target.prevPosZ))
                .multiply(targetPredictionTicks.get());
    }

    private boolean shouldStart() {
        boolean picked = false;

        for (PlayerUtils.PredictProcess predictProcess : selfPrediction) {
            if (criteria(predictProcess.tick)) {
                ticksToSkip = predictProcess.tick;
                picked = true;

                if (predictProcess.fallDistance > 0 && prioritiseCrits.get()) {
                    break;
                }
            }
        }

        if (!picked) {
            ticksToSkip = (int) maxTick.get() - 1;
        }

        return criteria(ticksToSkip);
    }

    private boolean criteria(int tick) {
        AxisAlignedBB entityBoundingBox = target.getHitbox().offset(getTargetPrediction());

        double predictedTargetDistance = PlayerUtils.getCustomDistanceToEntityBox(entityBoundingBox.getCenter(), mc.thePlayer);
        double predictedSelfDistance = PlayerUtils.getDistToTargetFromMouseOver(selfPrediction.get(tick).position.add(0, mc.thePlayer.getEyeHeight(), 0), mc.thePlayer.getLook(1), target, entityBoundingBox);

        return predictedSelfDistance < predictedTargetDistance &&
                predictedSelfDistance <= tickRange.get() &&
                predictedSelfDistance > minRange.get() &&
                predictedSelfDistance <= searchRange.get() &&
                PlayerUtils.getDistanceToEntityBox(target) >= stopRange.get() &&
                mc.thePlayer.canEntityBeSeen(target) &&
                target.canEntityBeSeen(mc.thePlayer) &&
                !selfPrediction.get(tick).isCollidedHorizontally &&
                !mc.thePlayer.isCollidedHorizontally;
    }

    private boolean shouldStop() {
        return mc.thePlayer.hurtTime > hurtTimeToStop.get();
    }

    public boolean skipTick() {
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