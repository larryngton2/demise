package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovementInput;
import org.apache.commons.lang3.Range;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.StaticTickEvent;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.SimulatedPlayer;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TimerRange", description = "Abuses balance in order to be unpredictable to your target.", category = ModuleCategory.Combat)
public class TimerRange extends Module {
    private final BoolValue preload = new BoolValue("Preload", true, this);
    private final SliderValue balanceRange = new SliderValue("Balance range", 8, 3, 15, 0.1f, this, preload::get);
    private final SliderValue minBalanceRange = new SliderValue("Min balance range", 3, 0, 15, 0.1f, this, preload::get);
    private final SliderValue delay = new SliderValue("Delay", 50, 0, 1000, 50, this);
    private final SliderValue tickRange = new SliderValue("Tick range", 3f, 0.1f, 8f, 0.1f, this);
    private final SliderValue minRange = new SliderValue("Min range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue stopRange = new SliderValue("Stop range", 2.5f, 0.1f, 8f, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 7f, 0.1f, 15, 0.1f, this);
    private final SliderValue maxTick = new SliderValue("Max ticks", 4, 1, 20, this);
    private final BoolValue allowEarlyBreak = new BoolValue("Allow early break", false, this);
    private final BoolValue prioritiseCrits = new BoolValue("Prioritise crits", false, this);
    private final SliderValue hurtTimeToStop = new SliderValue("HurtTime to stop (>)", 0, 0, 10, 1, this);
    private final BoolValue renderPredictedSelfPos = new BoolValue("Render predicted self pos", false, this);
    private final BoolValue teamCheck = new BoolValue("Team Check", false, this);
    private final BoolValue renderBalance = new BoolValue("Render balance", false, this);

    private final TimerUtils timer = new TimerUtils();
    private final List<PlayerUtils.PredictProcess> selfPrediction = new ArrayList<>();
    private EntityPlayer target;
    private int ticksToSkip;
    private int cachedTicks;
    private boolean cachedBalance;
    private boolean pause;
    public static int balance;
    public static boolean working;

    @Override
    public void onEnable() {
        balance = 0;
        cachedTicks = 0;
        pause = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(String.valueOf(maxTick.get()));

        target = PlayerUtils.getTarget(searchRange.get(), teamCheck.get());
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

        for (int i = 0; i < maxTick.get() + 1; i++) {
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
    public void onTick(TickEvent e) {
        balance++;
    }

    @EventTarget
    public void onStaticTick(StaticTickEvent e) {
        balance--;
    }

    @EventTarget
    public void onGame(GameEvent e) {
        if (target == null || selfPrediction.isEmpty() || shouldStop()) {
            return;
        }

        if (Range.between(minBalanceRange.get(), balanceRange.get()).contains((float) PlayerUtils.getDistanceToEntityBox(target)) && !cachedBalance && preload.get()) {
            double predictedTargetDistance = PlayerUtils.getCustomDistanceToEntityBox(PlayerUtils.getPosFromAABB(target.getHitbox()).add(0, target.getEyeHeight(), 0), mc.thePlayer);
            double predictedSelfDistance = PlayerUtils.getDistToTargetFromMouseOver(selfPrediction.get((int) maxTick.get() - 1).position.add(0, mc.thePlayer.getEyeHeight(), 0), mc.thePlayer.getLook(1), target, target.getHitbox());

            if (predictedSelfDistance < predictedTargetDistance) {
                cachedTicks = (int) maxTick.get();
                pause = true;
                cachedBalance = true;
            }
        }

        if (timer.hasTimeElapsed(delay.get()) && shouldStart()) {
            int skippedTick = 0;
            boolean skipped = false;
            while (skippedTick < ticksToSkip && !shouldStop() && -balance >= ticksToSkip) {
                skippedTick++;
                try {
                    mc.runTick();
                    skipped = true;
                    working = true;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (skipped) {
                cachedBalance = false;
                timer.reset();
            }
        } else {
            working = false;
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        balance = 0;
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (renderPredictedSelfPos.get() && mc.gameSettings.thirdPersonView != 0) {
            double x = selfPrediction.get(selfPrediction.size() - 1).position.xCoord - mc.getRenderManager().viewerPosX;
            double y = selfPrediction.get(selfPrediction.size() - 1).position.yCoord - mc.getRenderManager().viewerPosY;
            double z = selfPrediction.get(selfPrediction.size() - 1).position.zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, true, false, new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1, 100), true).getRGB());
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (renderBalance.get()) {
            ScaledResolution sr = new ScaledResolution(mc);
            Fonts.interRegular.get(18).drawCenteredString("Balance: " + balance, sr.getScaledWidth() / 2f, sr.getScaledHeight() - 80, -1);
        }
    }

    private boolean shouldStart() {
        boolean picked = false;

        for (PlayerUtils.PredictProcess predictProcess : selfPrediction) {
            if (criteria(predictProcess.tick)) {
                ticksToSkip = predictProcess.tick;
                picked = true;

                AxisAlignedBB entityBoundingBox = target.getHitbox();

                double predictedSelfDistance = PlayerUtils.getDistToTargetFromMouseOver(selfPrediction.get(predictProcess.tick).position.add(0, mc.thePlayer.getEyeHeight(), 0), mc.thePlayer.getLook(1), target, entityBoundingBox);

                if (predictProcess.fallDistance > 0 && prioritiseCrits.get()) {
                    break;
                }

                if (Range.between(tickRange.get(), tickRange.get() - 0.1f).contains((float) predictedSelfDistance) && allowEarlyBreak.get()) {
                    break;
                }
            }
        }

        if (!picked) {
            ticksToSkip = (int) maxTick.get();
        }

        return criteria(ticksToSkip);
    }

    private boolean criteria(int tick) {
        double predictedTargetDistance = PlayerUtils.getCustomDistanceToEntityBox(PlayerUtils.getPosFromAABB(target.getHitbox()).add(0, target.getEyeHeight(), 0), mc.thePlayer);
        double predictedSelfDistance = PlayerUtils.getDistToTargetFromMouseOver(selfPrediction.get(tick).position.add(0, mc.thePlayer.getEyeHeight(), 0), mc.thePlayer.getLook(1), target, target.getHitbox());

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
        if (timer.hasTimeElapsed(delay.get())) {
            if (cachedTicks <= 0) {
                pause = false;
                return false;
            }
            if (isEnabled() && pause && cachedTicks > 0) {
                --cachedTicks;
                return true;
            }
        }
        return false;
    }
}