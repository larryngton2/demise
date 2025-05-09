package wtf.demise.features.modules.impl.combat;

import net.minecraft.network.play.client.C03PacketPlayer;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.movement.Speed;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;

import java.util.Objects;

@ModuleInfo(name = "Criticals", description = "Allows you to get more critical hits.", category = ModuleCategory.Combat)
public class Criticals extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Jump", "NoGround", "Visual", "NCP", "Timer"}, "Jump", this);
    private final BoolValue staticOffset = new BoolValue("Static offset", false, this, () -> mode.is("NCP"));
    private final BoolValue stopXZOnHit = new BoolValue("Stop XZ on hit", true, this, () -> mode.is("NCP"));
    private final SliderValue tickDelay = new SliderValue("Tick delay", 9, 1, 20, 1, this);

    private boolean attacked;
    private final TimerUtils timer = new TimerUtils();
    private final double[] offsets = {0.06, 0.0, 0.03, 0.0};

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1f;
        attacked = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (mc.thePlayer.isInWater()) {
            return;
        }

        if (timer.hasTimeElapsed(tickDelay.get() * 50)) {
            attacked = true;

            switch (mode.get()) {
                case "Jump":
                    if (mc.thePlayer.onGround) {
                        Speed speed = Demise.INSTANCE.getModuleManager().getModule(Speed.class);
                        if (speed.isEnabled()) {
                            if (!MoveUtil.isMoving()) {
                                mc.thePlayer.jump();
                            }
                        } else {
                            mc.thePlayer.jump();
                        }
                    }
                    break;
                case "Visual":
                    mc.thePlayer.onCriticalHit(e.getTargetEntity());
                    break;
                case "Timer":
                    if (mc.thePlayer.onGround) {
                        Speed speed = Demise.INSTANCE.getModuleManager().getModule(Speed.class);
                        if (speed.isEnabled()) {
                            if (!MoveUtil.isMoving()) {
                                mc.thePlayer.jump();
                            }
                        } else {
                            mc.thePlayer.jump();
                        }
                    }

                    if (mc.thePlayer.motionY >= 0) {
                        mc.timer.timerSpeed = 2.75f;
                    } else {
                        mc.timer.timerSpeed = 0.6f;
                    }
                    break;
                case "NCP":
                    if (mc.thePlayer.isCollidedVertically && timer.hasTimeElapsed(tickDelay.get() * 50)) {
                        if (getModule(Speed.class).isEnabled()) {
                            return;
                        }

                        if (mc.thePlayer.onGround) {
                            if (stopXZOnHit.get()) {
                                mc.thePlayer.moveEntity(0, staticOffset.get() ? 0.05 : offsets[mc.thePlayer.ticksExisted % 5], 0);
                            } else {
                                mc.thePlayer.posY += staticOffset.get() ? 0.05 : offsets[mc.thePlayer.ticksExisted % 5];
                            }
                        }

                        timer.reset();
                    }
                    break;
            }

            timer.reset();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mode.is("NCP")) {
            if (e.getPacket() instanceof C03PacketPlayer c03PacketPlayer) {
                c03PacketPlayer.setMoving(false);
                c03PacketPlayer.setOnGround(false);
            }
        }
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPre() && attacked) {
            if (Objects.equals(mode.get(), "NoGround")) {
                e.setOnGround(false);
            }

            attacked = false;
        }
    }
}