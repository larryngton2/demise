package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "LongJump", description = "Jump, but long.", category = ModuleCategory.Movement)
public class LongJump extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "NCP", "Miniblox"}, "Vanilla", this);
    private final SliderValue jumpOffAmount = new SliderValue("JumpOff amount", 0.2f, 0.01f, 2, 0.01f, this, () -> mode.is("Vanilla"));

    private boolean jumped = false;
    private int currentTimer = 0;
    private int pauseTimes = 0;
    private int activeTicks = 0;

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

                        switch (pauseTimes) {
                            case 0:
                                mc.thePlayer.motionX = 1.9 * -Math.sin(MoveUtil.getDirection());
                                mc.thePlayer.motionZ = 1.9 * Math.cos(MoveUtil.getDirection());
                                maxTimer = 10;
                                break;
                            case 1:
                                mc.thePlayer.motionX = 1.285 * -Math.sin(MoveUtil.getDirection());
                                mc.thePlayer.motionZ = 1.285 * Math.cos(MoveUtil.getDirection());
                                maxTimer = 15;
                                break;
                            case 2:
                                mc.thePlayer.motionX = 1.1625 * -Math.sin(MoveUtil.getDirection());
                                mc.thePlayer.motionZ = 1.1625 * Math.cos(MoveUtil.getDirection());
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
        }
    }

    @Override
    public void onDisable() {
        MoveUtil.stop();
        jumped = false;
        currentTimer = 0;
        pauseTimes = 0;
        activeTicks = 0;
    }
}