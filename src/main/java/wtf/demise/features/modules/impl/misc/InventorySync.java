package wtf.demise.features.modules.impl.misc;

import net.minecraft.inventory.Container;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "Inventory Sync", category = ModuleCategory.Misc)
public class InventorySync extends Module {
    public short action;

    @EventTarget
    public void onReceivePacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.INCOMING) {
            return;
        }

        if (e.getPacket() instanceof S32PacketConfirmTransaction wrapper) {
            final Container inventory = mc.thePlayer.inventoryContainer;

            if (wrapper.getWindowId() == inventory.windowId) {
                this.action = wrapper.getActionNumber();

                if (this.action > 0 && this.action < inventory.transactionID) {
                    inventory.transactionID = (short) (this.action + 1);
                }
            }
        }
    }
}