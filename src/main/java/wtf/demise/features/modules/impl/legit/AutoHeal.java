package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.InventoryUtils;

@ModuleInfo(name = "AutoHeal", description = "Automatically heals you.")
public class AutoHeal extends Module {
    private final BoolValue head = new BoolValue("Head", true, this);
    private final SliderValue headHealth = new SliderValue("Head health", 15, 0, 20, 1, this, head::get);
    private final SliderValue minHeadDelay = new SliderValue("Min head delay", 1, 0, 10, 1, this, head::get);
    private final SliderValue maxHeadDelay = new SliderValue("Max head delay", 1, 0, 10, 1, this, head::get);

    private final BoolValue soup = new BoolValue("Soup", true, this);
    private final SliderValue soupHealth = new SliderValue("Soup health", 15, 0, 20, 1, this, soup::get);
    private final SliderValue minSoupDelay = new SliderValue("Min soup delay", 1, 0, 10, 1, this, soup::get);
    private final SliderValue maxSoupDelay = new SliderValue("Max soup delay", 1, 0, 10, 1, this, soup::get);
    private final BoolValue refill = new BoolValue("Refill", true, this, soup::get);
    private final SliderValue openDelay = new SliderValue("Open delay", 1, 0, 10, 1, this, () -> soup.get() && refill.get());
    private final SliderValue refillDelay = new SliderValue("Refill delay", 1, 0, 10, 1, this, () -> soup.get() && refill.get());

    private final TimerUtils headTimer = new TimerUtils();
    private int headWaitTime = 0;
    private final TimerUtils soupTimer = new TimerUtils();
    private int soupWaitTime = 0;
    private boolean switchBack;
    private int lastSoupSlot;
    private final TimerUtils refillTimer = new TimerUtils();
    private final TimerUtils openTimer = new TimerUtils();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (head.get() && mc.currentScreen == null) {
            if (Range.between(1, 9).contains(getHeadSlot()) && mc.thePlayer.getHealth() < headHealth.get() && headTimer.hasTimeElapsed(headWaitTime * 50L)) {
                int lastHeadSlot = mc.thePlayer.inventory.currentItem;

                mc.thePlayer.inventory.currentItem = getHeadSlot();

                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());

                mc.thePlayer.inventory.currentItem = lastHeadSlot;

                headWaitTime = MathUtils.randomizeInt(minHeadDelay.get(), maxHeadDelay.get());
                headTimer.reset();
            }
        }

        if (soup.get()) {
            if (mc.currentScreen == null) {
                if (soupTimer.hasTimeElapsed(soupWaitTime * 50L)) {
                    if (switchBack) {
                        mc.thePlayer.inventory.currentItem = lastSoupSlot;

                        switchBack = false;
                        soupWaitTime = MathUtils.randomizeInt(minSoupDelay.get(), maxSoupDelay.get());
                        soupTimer.reset();
                    }

                    if (Range.between(1, 9).contains(getSoupSlot()) && mc.thePlayer.getHealth() < soupHealth.get()) {
                        lastSoupSlot = mc.thePlayer.inventory.currentItem;

                        mc.thePlayer.inventory.currentItem = getSoupSlot();

                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                        mc.thePlayer.dropOneItem(false);

                        switchBack = true;
                        soupWaitTime = MathUtils.randomizeInt(minSoupDelay.get(), maxSoupDelay.get());
                        soupTimer.reset();
                    }
                } else if (mc.thePlayer.getCurrentEquippedItem().getItem() == Items.bowl) {
                    mc.thePlayer.dropOneItem(false);
                }

                refillTimer.reset();
                openTimer.reset();
            } else if (mc.currentScreen instanceof GuiInventory) {
                if (refill.get() && refillTimer.hasTimeElapsed(refillDelay.get() * 50L) && openTimer.hasTimeElapsed(openDelay.get() * 50L)) {
                    for (int slot = InventoryUtils.EXCLUDE_ARMOR_BEGIN; slot < InventoryUtils.ONLY_HOT_BAR_BEGIN; slot++) {
                        final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();

                        if (stack != null) {
                            if (stack.getItem() instanceof ItemSoup) {
                                for (int hotbarSlot = InventoryUtils.ONLY_HOT_BAR_BEGIN; hotbarSlot < 45; hotbarSlot++) {
                                    final ItemStack hotbarStack = mc.thePlayer.inventoryContainer.getSlot(hotbarSlot).getStack();

                                    if (hotbarStack == null) {
                                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 0, 1, mc.thePlayer);
                                        if (refillDelay.get() > 0) {
                                            refillTimer.reset();
                                            return;
                                        }
                                    }
                                }
                            } else if (stack.getItem() == Items.bowl) {
                                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 1, 4, mc.thePlayer);
                            }
                        }
                    }
                }
            }
        }
    }

    private int getHeadSlot() {
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

    private int getSoupSlot() {
        int slot = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemSoup) {
                if (heldItem != null && heldItem.getItem() instanceof ItemSoup) {
                    continue;
                }

                slot = i;
            }
        }
        return slot;
    }
}