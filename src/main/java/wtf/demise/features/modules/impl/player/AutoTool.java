package wtf.demise.features.modules.impl.player;

import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "AutoTool", description = "Automatically switches to the best tool when digging.", category = ModuleCategory.Player)
public class AutoTool extends Module {
    private final BoolValue spoof = new BoolValue("Spoof", false, this);
    private final BoolValue switchBack = new BoolValue("Switch Back", true, this, () -> !spoof.get());

    private int oldSlot;
    private boolean wasDigging;

    @Override
    public void onDisable() {
        if (wasDigging) {
            mc.thePlayer.inventory.currentItem = oldSlot;
            wasDigging = false;
        }
        SpoofSlotUtils.stopSpoofing();
    }

    @EventTarget
    public void onTick(TickEvent e) {
        if (mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && PlayerUtils.findTool(mc.objectMouseOver.getBlockPos()) != -1) {
            if (!wasDigging) {
                oldSlot = mc.thePlayer.inventory.currentItem;
                if (spoof.get()) {
                    SpoofSlotUtils.startSpoofing(oldSlot);
                }
            }
            mc.thePlayer.inventory.currentItem = PlayerUtils.findTool(mc.objectMouseOver.getBlockPos());
            wasDigging = true;
        } else if (wasDigging && (switchBack.get() || spoof.get())) {
            mc.thePlayer.inventory.currentItem = oldSlot;
            SpoofSlotUtils.stopSpoofing();
            wasDigging = false;
        } else {
            oldSlot = mc.thePlayer.inventory.currentItem;
        }
    }
}
