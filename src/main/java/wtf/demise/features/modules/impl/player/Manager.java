package wtf.demise.features.modules.impl.player;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.InventoryUtils;

import java.util.*;

@ModuleInfo(name = "Manager", description = "Manages your inventory.", category = ModuleCategory.Player)
public class Manager extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Open Inventory", "Spoof"}, "Open Inventory", this);

    private final BoolValue autoArmor = new BoolValue("Auto armor", true, this);
    private final SliderValue minArmorDelay = new SliderValue("Min armor delay", 1, 0, 10, 1, this, autoArmor::get);
    private final SliderValue maxArmorDelay = new SliderValue("Max armor delay", 1, 0, 10, 1, this, autoArmor::get);

    private final BoolValue sortItems = new BoolValue("Sort items", true, this);
    private final SliderValue minSortDelay = new SliderValue("Min sort delay", 1, 0, 10, 1, this, sortItems::get);
    private final SliderValue maxSortDelay = new SliderValue("Max sort delay", 1, 0, 10, 1, this, sortItems::get);

    private final BoolValue dropItems = new BoolValue("Drop items", true, this);
    private final SliderValue minDropDelay = new SliderValue("Min drop delay", 1, 0, 10, 1, this, dropItems::get);
    private final SliderValue maxDropDelay = new SliderValue("Max drop delay", 1, 0, 10, 1, this, dropItems::get);

    private final BoolValue startDelay = new BoolValue("Start Delay", true, this);
    public final BoolValue display = new BoolValue("Display", true, this);

    private final TimerUtils timer = new TimerUtils();
    private final int[] bestArmorPieces = new int[4];
    private final List<Integer> trash = new ArrayList<>();
    private final int[] bestToolSlots = new int[3];
    private final List<Integer> gappleStackSlots = new ArrayList<>();
    private final List<Integer> blockSlot = new ArrayList<>();
    private int bestSwordSlot;
    private int bestBowSlot;
    private boolean serverOpen;
    private boolean clientOpen;
    private boolean nextTickCloseInventory;
    public int slot;
    private final TimerUtils armorTimer = new TimerUtils();
    private int armorWait;
    private final TimerUtils sortTimer = new TimerUtils();
    private int sortWait;
    private final TimerUtils dropTimer = new TimerUtils();
    private int dropWait;

    @EventTarget
    public void onPacketSend(PacketEvent event) {
        final Packet<?> packet = event.getPacket();
        if (packet instanceof C16PacketClientStatus clientStatus) {
            if (clientStatus.getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                if (startDelay.get()) timer.reset();
                this.clientOpen = true;
                this.serverOpen = true;
            }
        } else if (packet instanceof C0DPacketCloseWindow packetCloseWindow) {
            if (packetCloseWindow.windowId == mc.thePlayer.inventoryContainer.windowId) {
                this.clientOpen = false;
                this.serverOpen = false;
                slot = -1;
            }
        }
        if (packet instanceof S2DPacketOpenWindow) {
            this.clientOpen = false;
            this.serverOpen = false;
        }
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

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (this.clientOpen || (mc.currentScreen == null && !Objects.equals(this.mode.get(), "Open Inventory"))) {
            this.clear();

            for (int slot = InventoryUtils.INCLUDE_ARMOR_BEGIN; slot < InventoryUtils.END; slot++) {
                final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();

                if (stack != null) {
                    if (stack.getItem() instanceof ItemSword && InventoryUtils.isBestSword(stack)) {
                        this.bestSwordSlot = slot;
                    } else if (stack.getItem() instanceof ItemBow && InventoryUtils.isBestBow(stack)) {
                        this.bestBowSlot = slot;
                    } else if (stack.getItem() instanceof ItemTool && InventoryUtils.isBestTool(mc.thePlayer, stack)) {
                        final int toolType = InventoryUtils.getToolType(stack);
                        if (toolType != -1 && slot != this.bestToolSlots[toolType])
                            this.bestToolSlots[toolType] = slot;
                    } else if (stack.getItem() instanceof ItemArmor armor && InventoryUtils.isBestArmor(mc.thePlayer, stack)) {
                        final int pieceSlot = this.bestArmorPieces[armor.armorType];

                        if (pieceSlot == -1 || slot != pieceSlot)
                            this.bestArmorPieces[armor.armorType] = slot;
                    } else if (stack.getItem() instanceof ItemBlock && slot == InventoryUtils.findBestBlockStack()) {
                        this.blockSlot.add(slot);
                    } else if (stack.getItem() instanceof ItemAppleGold) {
                        this.gappleStackSlots.add(slot);
                    } else if (!this.trash.contains(slot) && !InventoryUtils.isValidStack(stack)) {
                        this.trash.add(slot);
                    }
                }
            }

            final boolean armorReady = armorTimer.hasTimeElapsed(armorWait * 50L);
            final boolean sortReady = sortTimer.hasTimeElapsed(sortWait * 50L);
            final boolean dropReady = dropTimer.hasTimeElapsed(dropWait * 50L);

            boolean busy = false;

            if (armorReady && this.equipArmor(true)) {
                busy = true;
                resetTimings();
            } else if (dropReady && this.dropItem(this.trash)) {
                busy = true;
                resetTimings();
            } else if (sortReady && this.sortItems(true)) {
                busy = true;
                resetTimings();
            }

            if (!busy) {
                if (this.nextTickCloseInventory) {
                    this.close();
                    this.nextTickCloseInventory = false;
                } else {
                    this.nextTickCloseInventory = true;
                }
            } else {
                boolean waitUntilNextTick = !this.serverOpen;

                this.open();

                if (this.nextTickCloseInventory)
                    this.nextTickCloseInventory = false;

                if (waitUntilNextTick) return;
            }
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

    private boolean sortItems(final boolean moveItems) {
        if (this.sortItems.get()) {

            if (this.bestSwordSlot != -1) {
                if (this.bestSwordSlot != 36) {
                    if (moveItems) {
                        this.putItemInSlot(36, this.bestSwordSlot);
                        this.bestSwordSlot = 36;
                    }
                    return true;
                }
            }

            if (this.bestBowSlot != -1) {
                if (this.bestBowSlot != 38) {
                    if (moveItems) {
                        this.putItemInSlot(38, this.bestBowSlot);
                        this.bestBowSlot = 38;
                    }
                    return true;
                }
            }

            if (!this.gappleStackSlots.isEmpty()) {
                this.gappleStackSlots.sort(Comparator.comparingInt(slot -> mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));

                final int bestGappleSlot = this.gappleStackSlots.get(0);

                if (bestGappleSlot != 37) {
                    if (moveItems) {
                        this.putItemInSlot(37, bestGappleSlot);
                        this.gappleStackSlots.set(0, 37);
                    }
                    return true;
                }
            }

            if (!this.blockSlot.isEmpty()) {
                this.blockSlot.sort(Comparator.comparingInt(slot -> -mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));

                final int blockSlot = this.blockSlot.get(0);

                if (blockSlot != 42) {
                    if (moveItems) {
                        this.putItemInSlot(42, blockSlot);
                        this.blockSlot.set(0, 42);
                    }
                    return true;
                }
            }

            final int[] toolSlots = {39, 40, 41};

            for (final int toolSlot : this.bestToolSlots) {
                if (toolSlot != -1) {
                    final int type = InventoryUtils.getToolType(mc.thePlayer.inventoryContainer.getSlot(toolSlot).getStack());

                    if (type != -1) {
                        if (toolSlot != toolSlots[type]) {
                            if (moveItems) {
                                this.putToolsInSlot(type, toolSlots);
                            }
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean equipArmor(boolean moveItems) {
        if (this.autoArmor.get()) {
            for (int i = 0; i < this.bestArmorPieces.length; i++) {
                final int piece = this.bestArmorPieces[i];

                if (piece != -1) {
                    int armorPieceSlot = i + 5;
                    final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(armorPieceSlot).getStack();
                    if (stack != null)
                        continue;

                    if (moveItems)
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
        timer.reset();
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
    public void onEnable() {
        this.clientOpen = mc.currentScreen instanceof GuiInventory;
        this.serverOpen = this.clientOpen;
    }

    @Override
    public void onDisable() {
        this.close();
        this.clear();
    }

    private void open() {
        if (!this.clientOpen && !this.serverOpen) {
            mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            this.serverOpen = true;
        }
    }

    private void close() {
        if (!this.clientOpen && this.serverOpen) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            this.serverOpen = false;
        }
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
