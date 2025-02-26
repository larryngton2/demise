package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "Sprint", category = ModuleCategory.Movement)
public class Sprint extends Module {

    private final BoolValue omni = new BoolValue("Omni", false, this);
    public final BoolValue silent = new BoolValue("Silent", false, this);

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled(Scaffold.class)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }

        if (omni.get()) {
            mc.thePlayer.omniSprint = MoveUtil.isMoving();
        }
    }

    @Override
    public void onDisable() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        mc.thePlayer.omniSprint = false;
    }
}