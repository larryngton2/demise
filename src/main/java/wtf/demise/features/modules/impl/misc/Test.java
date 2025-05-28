package wtf.demise.features.modules.impl.misc;

import net.minecraft.entity.Entity;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;

@ModuleInfo(name = "Test", category = ModuleCategory.Misc)
public class Test extends Module {
    private final BoolValue pTicks = new BoolValue("partial ticks", false, this);
    private final RotationHandler rotationHandler = new RotationHandler(this);

    @EventTarget
    public void onAngle(AngleEvent e) {
        Entity target = PlayerUtils.getTarget(12, false);

        if (target == null) return;

        rotationHandler.setRotation(rotationHandler.getSimpleRotationsToEntity(target));
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        rotationHandler.updateRotSpeed(e);
    }

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        if (pTicks.get()) {
            ChatUtils.sendMessageClient(String.valueOf(mc.timer.partialTicks));
        }
    }
}