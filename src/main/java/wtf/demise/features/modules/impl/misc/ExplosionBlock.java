package wtf.demise.features.modules.impl.misc;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.SpoofSlotUtils;

//todo fix this bullshit
@ModuleInfo(name = "ExplosionBlock", description = "WIP, Automatically blocks when near an explosion.", category = ModuleCategory.Misc)
public class ExplosionBlock extends Module {
    private final BoolValue tnt = new BoolValue("TNT", true, this);
    private final SliderValue tntRange = new SliderValue("TNT Range", 4, 0.1f, 8, 0.01f, this, tnt::get);
    private final SliderValue tntFuse = new SliderValue("TNT Fuse", 5, 4, 80, 1, this, tnt::get);

    private final BoolValue creeper = new BoolValue("Creeper", true, this);
    private final SliderValue creeperRange = new SliderValue("Creeper Range", 4, 0.1f, 8, 0.01f, this, tnt::get);
    private final SliderValue creeperFuse = new SliderValue("Creeper Fuse", 5, 4, 30, 1, this, tnt::get);

    private final BoolValue spoof = new BoolValue("Spoof", false, this);

    private int oldSlot;
    private boolean blocked;

    @Override
    public void onDisable() {
        SpoofSlotUtils.stopSpoofing();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        boolean blockNow = false;

        if (tnt.get()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityTNTPrimed entityTNTPrimed) {
                    if (mc.thePlayer.getDistanceToEntity(entityTNTPrimed) - 0.5657 < tntRange.get() && entityTNTPrimed.fuse < tntFuse.get()) {
                        blockNow = true;
                    }
                }
            }
        }

        if (creeper.get()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityCreeper creeper) {
                    if (mc.thePlayer.getDistanceToEntity(creeper) - 0.5657 < creeperRange.get() && creeper.timeSinceIgnited >= 30 - creeperFuse.get()) {
                        blockNow = true;
                    }
                }
            }
        }

        if (blockNow && !blocked) {
            if (!(mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem].getItem() instanceof ItemSword)) {

            }

            oldSlot = mc.thePlayer.inventory.currentItem;
            if (spoof.get()) {
                SpoofSlotUtils.startSpoofing(oldSlot);
            }

            mc.thePlayer.inventory.currentItem = getSword();

            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
            blocked = true;
        }

        if (!blockNow && blocked && mc.thePlayer.hurtTime < 10) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            mc.thePlayer.inventory.currentItem = oldSlot;
            if (spoof.get()) {
                SpoofSlotUtils.stopSpoofing();
            }

            blocked = false;
        }
    }

    private int getSword() {
        int slot = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemSword) {
                if (heldItem != null && heldItem.getItem() instanceof ItemSword) {
                    continue;
                }

                slot = i;
            }
        }
        return slot;
    }
}
