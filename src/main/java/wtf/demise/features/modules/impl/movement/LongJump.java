package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "LongJump", description = "Jump, but long.")
public class LongJump extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Boat", "Miniblox", "Matrix"}, "Vanilla", this);
    private final SliderValue jumpOffAmount = new SliderValue("JumpOff amount", 0.2f, 0.01f, 2, 0.01f, this, () -> mode.is("Vanilla"));
    private final SliderValue verticalMotion = new SliderValue("Vertical motion", 0.25f, 0, 2, 0.01f, this, () -> mode.is("Boat"));
    private final SliderValue horizontalMotion = new SliderValue("Horizontal motion", 0.25f, 0, 2, 0.01f, this, () -> mode.is("Boat"));

    private boolean jumped;
    private int currentTimer;
    private int pauseTimes;
    private int activeTicks;
    private boolean flagged;
    private boolean wasInBoat;

    @EventTarget
    public void onJump(JumpEvent e) {
        if (mode.is("Vanilla")) {
            e.setJumpoff(jumpOffAmount.get());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        switch (mode.get()) {
            case "Miniblox":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                activeTicks++;

                if (activeTicks <= 10) {
                    MoveUtil.stop();
                } else {
                    if (!jumped) {
                        if (mc.thePlayer.onGround) {
                            MoveUtil.stop();
                            mc.thePlayer.jump();
                        }

                        jumped = true;
                    } else {
                        int maxTimer = 0;
                        float yaw = (float) Math.toRadians(mc.thePlayer.rotationYaw);

                        switch (pauseTimes) {
                            case 0:
                                mc.thePlayer.motionX = 1.9 * -Math.sin(yaw);
                                mc.thePlayer.motionZ = 1.9 * Math.cos(yaw);
                                maxTimer = 10;
                                break;
                            case 1:
                                mc.thePlayer.motionX = 1.285 * -Math.sin(yaw);
                                mc.thePlayer.motionZ = 1.285 * Math.cos(yaw);
                                maxTimer = 15;
                                break;
                            case 2:
                                mc.thePlayer.motionX = 1.1625 * -Math.sin(yaw);
                                mc.thePlayer.motionZ = 1.1625 * Math.cos(yaw);
                                maxTimer = 5;
                                break;
                        }

                        mc.thePlayer.motionY = 0.29;
                        currentTimer++;

                        if (Range.between(4, maxTimer).contains(currentTimer)) {
                            MoveUtil.stop();
                        } else if (currentTimer > maxTimer) {
                            pauseTimes++;
                            currentTimer = 0;
                            jumped = false;
                        }
                    }

                    if (pauseTimes >= 3) {
                        MoveUtil.stop();
                        toggle();
                    }
                }
                break;
            case "Matrix":
                if (mc.thePlayer.onGround) {
                    if (MoveUtil.isMoving()) {
                        mc.thePlayer.jump();
                    }
                } else if (MoveUtil.isMoving()) {
                    mc.thePlayer.motionY = 0.42;
                    MoveUtil.strafe(1.97);
                }

                if (flagged) {
                    toggle();
                }
                break;
            case "Boat":
                if (mc.thePlayer.ridingEntity != null) {
                    wasInBoat = true;
                } else if (wasInBoat) {
                    mc.thePlayer.motionY = verticalMotion.get();
                    MoveUtil.strafe(horizontalMotion.get());
                    wasInBoat = false;
                }
                break;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook) {
            flagged = true;
        }
    }

    @Override
    public void onDisable() {
        if (mode.is("Miniblox")) {
            MoveUtil.stop();
        }
        jumped = false;
        currentTimer = 0;
        pauseTimes = 0;
        activeTicks = 0;
        flagged = false;
        wasInBoat = false;
    }
}