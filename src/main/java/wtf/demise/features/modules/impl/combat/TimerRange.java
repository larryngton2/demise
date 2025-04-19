package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.RotationUtils;
import wtf.demise.utils.player.SimulatedPlayer;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "TimerRange", category = ModuleCategory.Combat)
public class TimerRange extends Module {
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 100, 3, 500, 0.1f, this);
    private final SliderValue attackTimer = new SliderValue("Attack timer", 2, 1, 10, 0.01f, this);
    private final SliderValue balanceTimer = new SliderValue("Balance gain timer", 0.95f, 0.01f, 0.99f, 0.01f, this);
    private final SliderValue maxBalance = new SliderValue("Max balance", 20, 0, 60, 1, this);
    private final SliderValue balanceLimitTimer = new SliderValue("Balance limit timer", 0.99f, 0.01f, 2f, 0.01f, this);
    private final BoolValue pauseOnFlag = new BoolValue("Pause on flag", true, this);
    private final BoolValue teamCheck = new BoolValue("Team check", true, this);
    private final BoolValue debug = new BoolValue("Debug", false, this);

    private final List<PlayerUtils.PredictProcess> predictProcesses = new ArrayList<>();
    private double balance = 0;
    private boolean attacked;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(attackTimer.get()));

        EntityLivingBase target = PlayerUtils.getTarget(searchRange.get(), teamCheck.get());

        if (target != null) {
            balance += mc.timer.timerSpeed - 1;

            double predictedDistance = PlayerUtils.getCustomDistanceToEntityBox(predictProcesses.get((int) ((attackTimer.get() * 0.65) - 1)).position, target);

            if (predictedDistance <= attackRange.get() && PlayerUtils.getDistanceToEntityBox(target) > attackRange.get()) {
                if (balance < maxBalance.get()) {
                    mc.timer.timerSpeed = attackTimer.get();
                } else {
                    mc.timer.timerSpeed = 1;
                }
            } else if (PlayerUtils.getDistanceToEntityBox(target) <= searchRange.get() && PlayerUtils.getDistanceToEntityBox(target) > attackRange.get()) {
                if (balance >= -maxBalance.get()) {
                    mc.timer.timerSpeed = balanceTimer.get();
                } else {
                    mc.timer.timerSpeed = balanceLimitTimer.get();
                }
            } else {
                mc.timer.timerSpeed = 1;
            }

            if (debug.get()) {
                ChatUtils.sendMessageClient(mc.timer.timerSpeed + " / " + balance + " / " + maxBalance.get() + " / " + mc.timer.elapsedTicks);
            }
        }
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

        for (int i = 0; i < (int) ((attackTimer.get() * 0.65)); i++) {
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
