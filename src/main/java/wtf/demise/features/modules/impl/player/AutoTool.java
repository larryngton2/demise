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

@ModuleInfo(name = "AutoTool", category = ModuleCategory.Player)
public class AutoTool extends Module {

    public final BoolValue spoof = new BoolValue("Spoof", false, this);
    public final BoolValue switchBack = new BoolValue("Switch Back", true, this, () -> !spoof.get());
    private int oldSlot;
    public boolean wasDigging;

    @Override
    public void onDisable() {
        if (this.wasDigging) {
            mc.thePlayer.inventory.currentItem = this.oldSlot;
            this.wasDigging = false;
        }
        SpoofSlotUtils.stopSpoofing();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && PlayerUtils.findTool(mc.objectMouseOver.getBlockPos()) != -1) {
            if (!this.wasDigging) {
                this.oldSlot = mc.thePlayer.inventory.currentItem;
                if (this.spoof.get()) {
                    SpoofSlotUtils.startSpoofing(this.oldSlot);
                }
            }
            mc.thePlayer.inventory.currentItem = PlayerUtils.findTool(mc.objectMouseOver.getBlockPos());
            this.wasDigging = true;
        } else if (this.wasDigging && (switchBack.get() || spoof.get())) {
            mc.thePlayer.inventory.currentItem = this.oldSlot;
            SpoofSlotUtils.stopSpoofing();
            this.wasDigging = false;
        } else {
            this.oldSlot = mc.thePlayer.inventory.currentItem;
        }
    }
}
