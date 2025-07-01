package wtf.demise.features.modules.impl.player;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.*;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.InventoryUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "Manager", description = "Manages your inventory.", category = ModuleCategory.Player)
public class Manager extends Module {
    private final SliderValue startDelay = new SliderValue("Start delay", 1, 0, 10, 1, this);

    private final BoolValue autoArmor = new BoolValue("Auto armor", true, this);
    private final SliderValue minArmorDelay = new SliderValue("Min armor delay", 1, 0, 10, 1, this, autoArmor::get);
    private final SliderValue maxArmorDelay = new SliderValue("Max armor delay", 1, 0, 10, 1, this, autoArmor::get);

    private final BoolValue sortItems = new BoolValue("Sort items", true, this);
    private final SliderValue minSortDelay = new SliderValue("Min sort delay", 1, 0, 10, 1, this, sortItems::get);
    private final SliderValue maxSortDelay = new SliderValue("Max sort delay", 1, 0, 10, 1, this, sortItems::get);

    private final BoolValue dropItems = new BoolValue("Drop items", true, this);
    private final SliderValue minDropDelay = new SliderValue("Min drop delay", 1, 0, 10, 1, this, dropItems::get);
    private final SliderValue maxDropDelay = new SliderValue("Max drop delay", 1, 0, 10, 1, this, dropItems::get);

    private final int[] bestArmorPieces = new int[4];
    private final List<Integer> trash = new ArrayList<>();
    private final int[] bestToolSlots = new int[3];
    private final List<Integer> gappleStackSlots = new ArrayList<>();
    private final List<Integer> blockSlot = new ArrayList<>();
    private int bestSwordSlot;
    private int bestBowSlot;
    public int slot;
    private final TimerUtils armorTimer = new TimerUtils();
    private int armorWait;
    private final TimerUtils sortTimer = new TimerUtils();
    private int sortWait;
    private final TimerUtils dropTimer = new TimerUtils();
    private int dropWait;
    private final TimerUtils startDelayTimer = new TimerUtils();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        boolean open = mc.currentScreen instanceof GuiInventory;

        if (!open) {
            startDelayTimer.reset();
        } else if (startDelayTimer.hasTimeElapsed(startDelay.get() * 50L)) {
            this.clear();

            for (int slot = InventoryUtils.INCLUDE_ARMOR_BEGIN; slot < InventoryUtils.END; slot++) {
                final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();

                if (stack != null) {
                    processInventoryItem(slot, stack);
                }
            }

            boolean armorReady = armorTimer.hasTimeElapsed(armorWait * 50L);
            boolean sortReady = sortTimer.hasTimeElapsed(sortWait * 50L);
            boolean dropReady = dropTimer.hasTimeElapsed(dropWait * 50L);

            if (armorReady && this.equipArmor()) {
                resetTimings();
            } else if (dropReady && this.dropItem(this.trash)) {
                resetTimings();
            } else if (sortReady && this.sortItems()) {
                resetTimings();
            }
        }
    }

    private void processInventoryItem(int slot, ItemStack stack) {
        if (stack == null) return;

        if (processCombatItems(slot, stack)) return;
        if (processToolsAndArmor(slot, stack)) return;
        if (processUtilityItems(slot, stack)) return;

        if (!trash.contains(slot) && !InventoryUtils.isValidStack(stack)) {
            trash.add(slot);
        }
    }

    private boolean processCombatItems(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemSword && InventoryUtils.isBestSword(stack)) {
            bestSwordSlot = slot;
            return true;
        }
        if (stack.getItem() instanceof ItemBow && InventoryUtils.isBestBow(stack)) {
            bestBowSlot = slot;
            return true;
        }
        if (stack.getItem() instanceof ItemAppleGold) {
            gappleStackSlots.add(slot);
            return true;
        }
        return false;
    }

    private boolean processToolsAndArmor(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemTool && InventoryUtils.isBestTool(stack)) {
            updateBestTool(slot, stack);
            return true;
        }
        if (stack.getItem() instanceof ItemArmor armor && InventoryUtils.isBestArmor(stack)) {
            updateBestArmor(slot, armor);
            return true;
        }
        return false;
    }

    private boolean dropItem(final List<Integer> listOfSlots) {
        if (this.dropItems.get()) {
            if (!listOfSlots.isEmpty()) {
                int slot = listOfSlots.remove(0);
                windowClick(slot, 1, 4);
                return true;
            }
        }
        return false;
    }

    private boolean processUtilityItems(int slot, ItemStack stack) {
        if (stack.getItem() instanceof ItemBlock && slot == InventoryUtils.findBestBlockStack()) {
            blockSlot.add(slot);
            return true;
        }
        return false;
    }

    private void updateBestTool(int slot, ItemStack stack) {
        int toolType = InventoryUtils.getToolType(stack);
        if (toolType != -1 && slot != bestToolSlots[toolType]) {
            bestToolSlots[toolType] = slot;
        }
    }

    private void updateBestArmor(int slot, ItemArmor armor) {
        int currentBestSlot = bestArmorPieces[armor.armorType];
        if (currentBestSlot == -1 || slot != currentBestSlot) {
            bestArmorPieces[armor.armorType] = slot;
        }
    }

    private void resetTimings() {
        armorTimer.reset();
        dropTimer.reset();
        sortTimer.reset();

        armorWait = MathUtils.randomizeInt(minArmorDelay.get(), maxArmorDelay.get());
        dropWait = MathUtils.randomizeInt(minDropDelay.get(), maxDropDelay.get());
        sortWait = MathUtils.randomizeInt(minSortDelay.get(), maxSortDelay.get());
    }

    private boolean sortItems() {
        if (this.sortItems.get()) {
            if (this.bestSwordSlot != -1) {
                if (this.bestSwordSlot != 36) {
                    this.putItemInSlot(36, this.bestSwordSlot);
                    this.bestSwordSlot = 36;
                    return true;
                }
            }

            if (this.bestBowSlot != -1) {
                if (this.bestBowSlot != 38) {
                    this.putItemInSlot(38, this.bestBowSlot);
                    this.bestBowSlot = 38;
                    return true;
                }
            }

            if (!this.gappleStackSlots.isEmpty()) {
                this.gappleStackSlots.sort(Comparator.comparingInt(slot -> mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));

                final int bestGappleSlot = this.gappleStackSlots.get(0);

                if (bestGappleSlot != 37) {
                    this.putItemInSlot(37, bestGappleSlot);
                    this.gappleStackSlots.set(0, 37);
                    return true;
                }
            }

            if (!this.blockSlot.isEmpty()) {
                this.blockSlot.sort(Comparator.comparingInt(slot -> -mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));

                final int blockSlot = this.blockSlot.get(0);

                if (blockSlot != 42) {
                    this.putItemInSlot(42, blockSlot);
                    this.blockSlot.set(0, 42);
                    return true;
                }
            }

            final int[] toolSlots = {39, 40, 41};

            for (final int toolSlot : this.bestToolSlots) {
                if (toolSlot != -1) {
                    final int type = InventoryUtils.getToolType(mc.thePlayer.inventoryContainer.getSlot(toolSlot).getStack());

                    if (type != -1) {
                        if (toolSlot != toolSlots[type]) {
                            this.putToolsInSlot(type, toolSlots);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean equipArmor() {
        if (this.autoArmor.get()) {
            for (int i = 0; i < this.bestArmorPieces.length; i++) {
                final int piece = this.bestArmorPieces[i];

                if (piece != -1) {
                    int armorPieceSlot = i + 5;
                    final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(armorPieceSlot).getStack();
                    if (stack != null)
                        continue;

                    windowClick(piece, 0, 1);

                    return true;
                }
            }
        }
        return false;
    }

    public void windowClick(int slotId, int mouseButtonClicked, int mode) {
        slot = slotId;
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slotId, mouseButtonClicked, mode, mc.thePlayer);
    }

    private void putItemInSlot(final int slot, final int slotIn) {
        windowClick(slotIn, slot - 36, 2);
    }

    private void putToolsInSlot(final int tool, final int[] toolSlots) {
        final int toolSlot = toolSlots[tool];

        windowClick(this.bestToolSlots[tool],
                toolSlot - 36,
                2);
        this.bestToolSlots[tool] = toolSlot;
    }

    @Override
    public void onDisable() {
        this.clear();
    }

    private void clear() {
        this.trash.clear();
        this.bestBowSlot = -1;
        this.bestSwordSlot = -1;
        this.gappleStackSlots.clear();
        this.blockSlot.clear();
        Arrays.fill(this.bestArmorPieces, -1);
        Arrays.fill(this.bestToolSlots, -1);
    }
}