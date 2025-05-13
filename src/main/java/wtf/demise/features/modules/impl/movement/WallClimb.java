package wtf.demise.features.modules.impl.movement;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.BlockAABBEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "WallClimb", description = "Allows you to climb up walls.", category = ModuleCategory.Movement)
public class WallClimb extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Polar"}, "Polar", this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());
    }

    @EventTarget
    public void onBlockAABB(BlockAABBEvent e) {
        if (mode.is("Polar")) {
            if (e.getBlockPos().getY() >= mc.thePlayer.posY || mc.thePlayer.isSneaking() && mc.thePlayer.onGround) {
                e.setBoundingBox(e.getBoundingBox().contract(0.0001, 0, 0.0001));
            }
        }
    }
}
