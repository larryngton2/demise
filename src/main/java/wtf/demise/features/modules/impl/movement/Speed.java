package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.DebugUtils;
import wtf.demise.utils.player.MovementUtils;

import java.util.Objects;

@ModuleInfo(name = "Speed", category = ModuleCategory.Movement)
public class Speed extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Strafe", "GroundStrafe", "BHop", "NCP Tick 5", "NCP Tick 4", "Miniblox", "Vulcan test", "ONCPBHop", "Watchdog 7 tick"}, "Strafe", this);
    private final SliderValue speed = new SliderValue("Speed", 0.25f, 0, 5, 0.05f, this, () -> Objects.equals(mode.get(), "Strafe") || Objects.equals(mode.get(), "GroundStrafe") || Objects.equals(mode.get(), "BHop"));
    private final SliderValue minSpeed = new SliderValue("Min speed", 0.25f, 0, 1, 0.05f, this);
    private final BoolValue printAirTicks = new BoolValue("Print airTicks", false);

    private int movingTicks, stoppedTicks;

    @Override
    public void onEnable() {
        mc.timer.timerSpeed = 1.0f;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;

        if (Objects.equals(mode.get(), "NCP Tick 4")) {
            MovementUtils.stopXZ();
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get().replace(" ", ""));

        if (!MovementUtils.isMoving()) {
            movingTicks = 0;
            stoppedTicks++;
            return;
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            movingTicks++;
            stoppedTicks = 0;
        }

        if (printAirTicks.get()) {
            DebugUtils.sendMessage("Air Ticks: " + mc.thePlayer.offGroundTicks);
        }

        if (mc.thePlayer.onGround && MovementUtils.isMoving() && !Objects.equals(mode.get(), "NCP Tick 4")) {
            mc.thePlayer.jump();
        }

        if (MovementUtils.getSpeed() < minSpeed.get() && MovementUtils.isMoving() && movingTicks > 15) {
            MovementUtils.strafe(minSpeed.get());
        }

        if (!Objects.equals(mode.get(), "ONCPBHop")) {
            mc.timer.timerSpeed = 1.0f;
        }

        switch (mode.get()) {
            case "Strafe":
                if (speed.get() <= 0.05f) {
                    MovementUtils.strafe();
                } else {
                    MovementUtils.strafe(speed.get());
                }
                break;

            case "GroundStrafe": {
                if (mc.thePlayer.onGround) {
                    if (speed.get() <= 0.05f) {
                        MovementUtils.strafe();
                    } else {
                        MovementUtils.strafe(speed.get());
                    }
                }
            }
            break;

            case "BHop": {
                if (MovementUtils.isMoving() && !mc.thePlayer.isInWater()) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                    double spd = 0.0025D * speed.get();
                    double m = (float) (Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ) + spd);
                    MovementUtils.bop(m);
                }
            }
            break;

            case "NCP Tick 5": {
                if (mc.thePlayer.onGround) {
                    MovementUtils.strafe();
                }

                if (mc.thePlayer.offGroundTicks == 5) {
                    mc.thePlayer.motionY -= 0.1523351824467155;
                }

                if (mc.thePlayer.hurtTime >= 5 && mc.thePlayer.motionY >= 0) {
                    mc.thePlayer.motionY -= 0.1;
                }

                double BOOST_CONSTANT = 0.00718;

                if (MovementUtils.isMoving()) {
                    mc.thePlayer.motionX *= 1f + BOOST_CONSTANT;
                    mc.thePlayer.motionZ *= 1f + BOOST_CONSTANT;
                }
            }
            break;

            case "NCP Tick 4": {
                switch (mc.thePlayer.offGroundTicks) {
                    case 4: {
                        if (mc.thePlayer.posY % 1.0 == 0.16610926093821377) {
                            mc.thePlayer.motionY = -0.09800000190734863;
                        }
                    }
                    break;

                    case 6: {
                        if (MovementUtils.isMoving()) MovementUtils.strafe();
                    }
                    break;
                }

                if (mc.thePlayer.hurtTime >= 1 && mc.thePlayer.motionY > 0) {
                    mc.thePlayer.motionY -= 0.15;
                }

                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                    MovementUtils.strafe();

                    if (MovementUtils.getSpeed() < 0.281) {
                        MovementUtils.strafe(0.281);
                    } else {
                        MovementUtils.strafe();
                    }
                }
            }
            break;

            case "Miniblox": {
                switch (mc.thePlayer.offGroundTicks) {
                    case 3:
                        mc.thePlayer.motionY -= 0.1523351824467155;
                        break;
                    case 5:
                        mc.thePlayer.motionY -= 0.232335182447;
                        break;
                }

                if (mc.thePlayer.onGround) {
                    MovementUtils.strafe(0.175f);
                } else {
                    MovementUtils.strafe(0.35f);
                }
            }
            break;

            case "Vulcan test": {
                switch (mc.thePlayer.offGroundTicks) {
                    case 1: {
                        if (mc.thePlayer.movementInput.moveStrafe != 0f) {
                            MovementUtils.strafe(0.3345);
                        } else {
                            MovementUtils.strafe(0.3355);
                        }
                    }
                    break;

                    case 2: {
                        if (mc.thePlayer.isSprinting()) {
                            if (mc.thePlayer.movementInput.moveStrafe != 0f) {
                                MovementUtils.strafe(0.3235);
                            } else {
                                MovementUtils.strafe(0.3284);
                            }
                        }
                    }
                    break;

                    case 6:
                        if (MovementUtils.getSpeed() > 0.298) {
                            MovementUtils.strafe(0.298);
                        }
                        break;
                }
            }
            break;

            case "ONCPBHop":
                if (MovementUtils.isMoving() && !mc.thePlayer.isInWater()) {
                    double spd = 0.0025 * 0.4;
                    double m = (float) (Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ) + spd);
                    MovementUtils.bop(m);
                }

                if (mc.thePlayer.offGroundTicks == 4) {
                    mc.thePlayer.motionY -= 0.09800000190734863;
                }

                if (MovementUtils.getSpeed() < 0.312866806998394775 && movingTicks > 15) {
                    MovementUtils.strafe(0.312866806998394775);
                }

                if (MovementUtils.isMoving()) {
                    float timerSpeed = (float) (1.337 - MovementUtils.getSpeed());

                    if (timerSpeed > 1.5) timerSpeed = 1.5f;
                    if (timerSpeed < 0.6) timerSpeed = 0.6f;
                    mc.timer.timerSpeed = timerSpeed;
                }
                break;
            case "Watchdog 7 tick":
                if (mc.thePlayer.onGround) {
                    MovementUtils.strafe();
                } else {
                    switch (mc.thePlayer.offGroundTicks) {
                        case 1:
                            MovementUtils.strafe();
                            mc.thePlayer.motionY += 0.0568;
                            break;
                        case 3:
                            mc.thePlayer.motionY -= 0.13;
                            break;
                        case 4:
                            mc.thePlayer.motionY -= 0.2;
                            break;
                    }

                    if (mc.thePlayer.hurtTime >= 7) {
                        MovementUtils.strafe(Math.max(MovementUtils.getSpeed(), 0.281));
                    }

                    if (MovementUtils.getSpeedEffect() == 3) {
                        switch (mc.thePlayer.offGroundTicks) {
                            case 1:
                            case 2:
                            case 5:
                            case 6:
                            case 8:
                                mc.thePlayer.motionX *= 1.2;
                                mc.thePlayer.motionZ *= 1.2;
                                break;
                        }
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        if (Objects.equals(mode.get(), "Watchdog 7 tick")) {
            double atLeast = 0.281 + 0.13 * (MovementUtils.getSpeedEffect() - 1);
            MovementUtils.strafe(Math.max(MovementUtils.getSpeed(), atLeast));
        }
    }
}