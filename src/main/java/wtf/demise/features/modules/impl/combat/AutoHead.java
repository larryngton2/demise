package wtf.demise.features.modules.impl.combat;

import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "AutoHead", category = ModuleCategory.Combat)
public class AutoHead extends Module {
    private final SliderValue health = new SliderValue("Health", 15, 1, 20, 0.5f, this  );

    private boolean healed;
    private int lastSlot;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (Range.between(1, 9).contains(getSlot()) && mc.thePlayer.getHealth() < health.get()) {
            lastSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = getSlot();

            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());

            healed = true;
        }

        if (healed) {
            mc.thePlayer.inventory.currentItem = lastSlot;
            healed = false;
        }

        this.setTag(String.valueOf(health.get()));
    }

    private int getSlot() {
        int slot = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemSkull) {
                if (heldItem != null && heldItem.getItem() instanceof ItemSkull) {
                    continue;
                }

                slot = i;
            }
        }
        return slot;
    }
}
