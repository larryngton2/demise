package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "Jesus", description = "Sorry non-Christian people. Allows you to walk on water.")
public class Jesus extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Solid", "NCP"}, "Solid", this);

    @EventTarget
    public void onMotion(MotionEvent e) {
        setTag(mode.get());

        if (e.isPre() && mode.is("NCP")) {
            if (PlayerUtils.isOnLiquid() && !getModule(Speed.class).isEnabled() && mc.thePlayer.ticksExisted % 2 == 0) {
                e.setY(e.getY() - 0.015625);
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (PlayerUtils.isOnLiquid() && mode.is("NCP")) {
            e.setJumping(false);
        }
    }
}