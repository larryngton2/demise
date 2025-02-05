package wtf.demise.features.modules.impl.combat;

import net.minecraft.network.play.client.C03PacketPlayer;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.impl.movement.Speed;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.MovementUtils;

import java.util.Objects;

public class Criticals extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Jump", "NoGround", "Visual", "NCP", "Timer"}, "Jump", this);
    private boolean attacked;

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1f;
        attacked = false;
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (mc.thePlayer.isInWater()) {
            return;
        }

        attacked = true;

        switch (mode.get()) {
            case "Jump":
                if (mc.thePlayer.onGround) {
                    Speed speed = Demise.INSTANCE.getModuleManager().getModule(Speed.class);
                    if (speed.isEnabled()) {
                        if (!MovementUtils.isMoving()) {
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
            case "NCP":
                PacketUtils.sendPacket(
                        new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.11, mc.thePlayer.posZ, false)
                );
                PacketUtils.sendPacket(
                        new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.1100013579, mc.thePlayer.posZ, false)
                );
                PacketUtils.sendPacket(
                        new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.0000013579, mc.thePlayer.posZ, false)
                );
                break;
            case "Timer":
                if (mc.thePlayer.onGround) {
                    Speed speed = Demise.INSTANCE.getModuleManager().getModule(Speed.class);
                    if (speed.isEnabled()) {
                        if (!MovementUtils.isMoving()) {
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