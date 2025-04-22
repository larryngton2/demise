package wtf.demise.features.modules.impl.movement;

import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.StrafeEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "LongJump", category = ModuleCategory.Movement)
public class LongJump extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "NCP"}, "Vanilla", this);
    private final SliderValue jumpOffAmount = new SliderValue("JumpOff amount", 0.2f, 0.01f, 2, 0.01f, this, () -> mode.is("Vanilla"));

    private final SliderValue groundSpeed = new SliderValue("Ground Speed", 0.4f, 0.1f, 3, 0.1f, this, () -> mode.is("NCP"));
    private final SliderValue jumpSpeed = new SliderValue("Jump Speed", 1.4f, 0, 3, 0.1f, this, () -> mode.is("NCP"));
    private final SliderValue glide = new SliderValue("Glide", 0, 0, 3, 0.5f, this, () -> mode.is("NCP"));
    private final SliderValue timer = new SliderValue("Timer", 1, 0.1f, 10, 0.1f, this, () -> mode.is("NCP"));
    private final BoolValue autoDisable = new BoolValue("Auto disable", true, this, () -> mode.is("NCP"));

    private boolean reset;
    private double speed;
    private boolean disable;

    @EventTarget
    public void onJump(JumpEvent e) {
        if (mode.is("Vanilla")) {
            e.setJumpoff(jumpOffAmount.get());
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent e) {
        if (mode.is("NCP")) {
            final double base = MoveUtil.getAllowedHorizontalDistance();

            if (MoveUtil.isMoving()) {
                switch (mc.thePlayer.offGroundTicks) {
                    case 0:
                        mc.thePlayer.motionY = MoveUtil.getJumpHeight();
                        speed = groundSpeed.get();
                        break;

                    case 1:
                        speed = jumpSpeed.get();
                        if (autoDisable.get()) {
                            disable = true;
                        }
                        break;

                    default:
                        speed -= speed / MoveUtil.BUNNY_FRICTION;
                        break;
                }

                mc.timer.timerSpeed = timer.get();
                reset = false;
            } else if (!reset) {
                speed = MoveUtil.getAllowedHorizontalDistance();
                mc.timer.timerSpeed = 1;
                reset = true;
            }

            if (mc.thePlayer.fallDistance > 0) {
                mc.thePlayer.motionY += glide.get() / 100;
            }

            if (mc.thePlayer.isCollidedHorizontally) {
                speed = MoveUtil.getAllowedHorizontalDistance();
            }

            e.setSpeed(Math.max(speed, base), Math.random() / 2000);

            if (disable && mc.thePlayer.onGround) {
                this.setEnabled(false);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING && mode.is("NCP")) {
            if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                speed = 0;
            }
        }
    }

    @Override
    public void onDisable() {
        MoveUtil.stop();
        speed = 0;
        disable = false;
    }
}