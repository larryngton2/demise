package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.MovingObjectPosition;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.RotationUtils;
import wtf.demise.utils.player.SimulatedPlayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TimerRange", description = "Abuses balance in order to be unpredictable to your target.", category = ModuleCategory.Combat)
public class TimerRange extends Module {
    private final BoolValue staticRange = new BoolValue("Static range", false, this);
    private final SliderValue minRange = new SliderValue("Min range", 3, 0.1f, 8, 0.1f, this, staticRange::get);
    private final SliderValue maxRange = new SliderValue("Max range", 4.5f, 0.1f, 8, 0.1f, this, staticRange::get);

    private final BoolValue smartRange = new BoolValue("Smart range", false, this, () -> !staticRange.get());
    //todo fix this
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this, () -> staticRange.get() ? false : !smartRange.get());

    private final BoolValue alwaysGainBal = new BoolValue("Always gain balance", false, this);
    private final SliderValue searchRange = new SliderValue("Search range", 100, 3, 500, 0.1f, this, () -> !alwaysGainBal.get());

    private final SliderValue attackTimer = new SliderValue("Attack timer", 2, 1, 10, 0.01f, this);
    private final SliderValue balanceTimer = new SliderValue("Balance gain timer", 0.95f, 0.01f, 0.99f, 0.01f, this);
    private final SliderValue maxBalance = new SliderValue("Max balance", 20, 0, 60, 0.1f, this);
    private final SliderValue balanceLimitTimer = new SliderValue("Balance limit timer", 0.99f, 0.01f, 2f, 0.01f, this);
    private final BoolValue pauseOnFlag = new BoolValue("Pause on flag", true, this);
    private final BoolValue teamCheck = new BoolValue("Team check", true, this);
    private final BoolValue renderBalance = new BoolValue("Render balance", true, this);
    private final BoolValue debug = new BoolValue("Debug", false, this);
    private final BoolValue currBalance = new BoolValue("Curr balance", false, this, debug::get);
    private final BoolValue lessThan0 = new BoolValue("Less than 0", true, this, debug::get);

    private final List<PlayerUtils.PredictProcess> predictProcesses = new ArrayList<>();
    private double balance = 0;
    private boolean attacked;
    private EntityLivingBase target;
    public boolean renderBal;
    public String balanceText;
    private double balancePercentage;

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1;
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        renderBal = renderBalance.get();

        balancePercentage = (((-balance / maxBalance.get()) * 100) + 100) / 2;

        if (renderBalance.get()) {
            balanceText = "Balance: " + BigDecimal.valueOf(balancePercentage).setScale(1, RoundingMode.FLOOR) + "%";
        } else {
            balanceText = "";
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && e.getTargetEntity() != null) {
            attacked = true;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(attackTimer.get()));

        target = PlayerUtils.getTarget(searchRange.get(), teamCheck.get());

        if (target != null || alwaysGainBal.get()) {
            balance += mc.timer.timerSpeed - 1;

            if (debug.get() && currBalance.get()) {
                ChatUtils.sendMessageClient(mc.timer.timerSpeed + " / " + balance + " / " + maxBalance.get() + " / " + mc.timer.elapsedTicks);
            }
        }
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (target != null || alwaysGainBal.get()) {
            boolean check;
            boolean rangeCheck;
            double predictedDistance;

            if (!staticRange.get()) {
                predictedDistance = PlayerUtils.getCustomDistanceToEntityBox(predictProcesses.get(predictProcesses.size() - 1).position, target);
                rangeCheck = predictedDistance < PlayerUtils.getDistanceToEntityBox(target) && ((PlayerUtils.getDistanceToEntityBox(target) > attackRange.get() && !smartRange.get()) || (smartRange.get() && !attacked && PlayerUtils.getDistanceToEntityBox(target) > 2));
                check = (target != null || !alwaysGainBal.get()) && predictedDistance <= attackRange.get() && rangeCheck && mc.thePlayer.canEntityBeSeen(target);
            } else {
                predictedDistance = PlayerUtils.getCustomDistanceToEntityBox(predictProcesses.get(predictProcesses.size() - 1).position, target);
                rangeCheck = predictedDistance < PlayerUtils.getDistanceToEntityBox(target) && Range.between(minRange.get(), maxRange.get()).contains((float) PlayerUtils.getDistanceToEntityBox(target));
                check = (target != null || !alwaysGainBal.get()) && rangeCheck && mc.thePlayer.canEntityBeSeen(target);
            }

            if (check && mc.thePlayer.hurtTime == 0) {
                if (balancePercentage - attackTimer.get() * 2 > 0) {
                    mc.timer.timerSpeed = attackTimer.get();
                } else {
                    mc.timer.timerSpeed = 1;
                }
            } else if (alwaysGainBal.get() || (PlayerUtils.getDistanceToEntityBox(target) <= searchRange.get() && !rangeCheck)) {
                if (balancePercentage < 100) {
                    mc.timer.timerSpeed = balanceTimer.get();
                } else {
                    mc.timer.timerSpeed = balanceLimitTimer.get();
                }

                if (balancePercentage < 0 && PlayerUtils.getDistanceToEntityBox(target) > 4.5) {
                    if (debug.get() && lessThan0.get()) {
                        ChatUtils.sendMessageClient("bal is < 0");
                    }

                    mc.timer.timerSpeed = 0.5f;
                }
            } else {
                mc.timer.timerSpeed = 1;
            }
        } else {
            mc.timer.timerSpeed = 1;
        }

        attacked = false;
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook && pauseOnFlag.get()) {
            balance = maxBalance.get();
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        balance = maxBalance.get();
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        predictProcesses.clear();

        SimulatedPlayer simulatedPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput, 1);

        simulatedPlayer.rotationYaw = RotationUtils.currentRotation != null ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < (int) (Math.max(attackTimer.get(), 1)); i++) {
            simulatedPlayer.tick();
            predictProcesses.add(
                    new PlayerUtils.PredictProcess(
                            simulatedPlayer.getPos(),
                            simulatedPlayer.fallDistance,
                            simulatedPlayer.onGround,
                            simulatedPlayer.isCollidedHorizontally
                    )
            );
        }
    }
}
