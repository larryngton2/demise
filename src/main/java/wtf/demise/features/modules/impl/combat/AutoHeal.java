package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.DebugUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;

@ModuleInfo(name = "AutoHeal", category = ModuleCategory.Combat)
public class AutoHeal extends Module {
    private final BoolValue head = new BoolValue("Head", true, this);
    private final SliderValue headHealth = new SliderValue("HeadHealth", 15, 0, 20, 1, this, head::get);
    private final SliderValue minHeadDelay = new SliderValue("MinHeadDelay", 300, 50, 5000, 50, this, head::get);
    private final SliderValue maxHeadDelay = new SliderValue("MaxHeadDelay", 500, 50, 5000, 50, this, head::get);
    private final BoolValue gapple = new BoolValue("Gapple", true, this);
    private final SliderValue gappleHealth = new SliderValue("GappleHealth", 15, 0, 20, 1, this, gapple::get);
    private final SliderValue minGappleDelay = new SliderValue("MinGappleDelay", 300, 50, 5000, 50, this, gapple::get);
    private final SliderValue maxGappleDelay = new SliderValue("MaxGappleDelay", 500, 50, 5000, 50, this, gapple::get);

    private final TimerUtils headTimer = new TimerUtils();
    private int headWaitTime = 0;
    private final TimerUtils gapTimer = new TimerUtils();
    private int gapWaitTime = 0;
    private boolean switchBack;
    private int oldSlot;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (head.get()) {
            if (Range.between(1, 9).contains(getHeadSlot()) && mc.thePlayer.getHealth() < headHealth.get() && headTimer.hasTimeElapsed(headWaitTime)) {
                int lastHeadSlot = mc.thePlayer.inventory.currentItem;

                mc.thePlayer.inventory.currentItem = getHeadSlot();

                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());

                mc.thePlayer.inventory.currentItem = lastHeadSlot;

                headWaitTime = MathUtils.randomizeInt(minHeadDelay.get(), maxHeadDelay.get());
                headTimer.reset();
            }
        }

        if (gapple.get()) {
            if (Range.between(1, 9).contains(getGappleSlot()) && mc.thePlayer.getHealth() < gappleHealth.get() && gapTimer.hasTimeElapsed(gapWaitTime)) {
                if (!switchBack) {
                    oldSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = getGappleSlot();
                    SpoofSlotUtils.startSpoofing(oldSlot);

                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                } else {
                    mc.thePlayer.inventory.currentItem = oldSlot;
                    SpoofSlotUtils.stopSpoofing();

                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);

                    gapWaitTime = MathUtils.randomizeInt(minGappleDelay.get(), maxGappleDelay.get());
                    gapTimer.reset();
                }

                DebugUtils.sendMessage(switchBack + "");

                switchBack = false;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (gapple.get()) {
            if (Range.between(1, 9).contains(getGappleSlot()) && mc.thePlayer.getHealth() < gappleHealth.get() && gapTimer.hasTimeElapsed(gapWaitTime)) {
                if (e.getPacket() instanceof C07PacketPlayerDigging c07 && c07.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    switchBack = true;
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

    private int getGappleSlot() {
        int slot = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemAppleGold) {
                if (heldItem != null && heldItem.getItem() instanceof ItemAppleGold) {
                    continue;
                }

                slot = i;
            }
        }
        return slot;
    }
}