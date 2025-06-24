package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.player.PlayerUtils;

import java.util.Objects;

@ModuleInfo(name = "Jesus", description = "Sorry non-Christian people. Allows you to walk on water.", category = ModuleCategory.Movement)
public class Jesus extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Solid", "NCP"}, "Solid", this);

    @EventTarget
    public void onMotion(MotionEvent e) {
        setTag(mode.get());

        if (e.isPre() && mode.is("NCP")) {
            if (PlayerUtils.isOnLiquid() && !Objects.requireNonNull(getModule(Speed.class)).isEnabled() && mc.thePlayer.ticksExisted % 2 == 0) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                e.setY(e.getY() - 0.015625);
            }
        }
    }
}
