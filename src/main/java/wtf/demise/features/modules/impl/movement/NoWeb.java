package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.player.WebSlowDownEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "NoWeb")
public class NoWeb extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Legit flat"}, "Vanilla", this);
    private final BoolValue safeJump = new BoolValue("Safe jump", true, this, () -> mode.is("Vanilla"));

    public NoWeb() {
        safeJump.setDescription("Modifies jump values to not get flagged.");
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());
    }

    @EventTarget
    public void onWebSlowDown(WebSlowDownEvent e) {
        if (mode.is("Vanilla")) {
            e.setCancelled(true);

            if (mc.thePlayer.offGroundTicks > 10 && safeJump.get()) {
                mc.thePlayer.motionX *= 0.5f;
                mc.thePlayer.motionZ *= 0.5f;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (mode.is("Legit flat") && mc.thePlayer.isInWeb && MoveUtil.isMoving()) {
            mc.thePlayer.jumpTicks = 0;
            e.setJumping(true);
        }
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        if (mc.thePlayer.isInWeb) {
            switch (mode.get()) {
                case "Vanilla":
                    if (safeJump.get()) {
                        e.setJumpoff(0.05f);
                    }
                    break;
                case "Legit flat":
                    e.setMotionY(0);
                    break;
            }
        }
    }
}
