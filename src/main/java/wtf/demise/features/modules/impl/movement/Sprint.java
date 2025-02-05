package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.MovementUtils;
import wtf.demise.utils.player.RotationUtils;

@ModuleInfo(name = "Sprint", category = ModuleCategory.Movement)
public class Sprint extends Module {

    private final BoolValue omni = new BoolValue("Omni", false, this);
    private final BoolValue silent = new BoolValue("Silent", false, this);

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled(Scaffold.class))
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);

        if (omni.get()) {
            mc.thePlayer.omniSprint = MovementUtils.isMoving();
        }

        if (silent.get()) {
            mc.thePlayer.serverSprintState = false;
        }
    }

    @Override
    public void onDisable() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        mc.thePlayer.omniSprint = false;
    }
}