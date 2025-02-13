package wtf.demise.features.modules.impl.combat;

import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.RandomUtils;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.PacketUtils;

@ModuleInfo(name = "AutoHead", category = ModuleCategory.Combat)
public class AutoHead extends Module {

    private final SliderValue health = new SliderValue("Health", 15, 0, 20, 1, this);
    private final SliderValue minDelay = new SliderValue("Min Delay", 300, 0, 1000, 25, this);
    private final SliderValue maxDelay = new SliderValue("Max Delay", 500, 0, 1000, 25, this);

    private final TimerUtils timer = new TimerUtils();
    private boolean switchBack;
    private long decidedTimer;

    @Override
    public void onDisable() {
        switchBack = false;
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPre()) {
            if (switchBack) {
                PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                switchBack = false;
                return;
            }

            if (timer.hasTimeElapsed(decidedTimer)) {
                if (mc.thePlayer.ticksExisted > 10 && mc.thePlayer.getHealth() < health.get()) {
                    PacketUtils.sendPacket(new C09PacketHeldItemChange(getSlot()));
                    PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(getSlot())));
                    switchBack = true;

                    final int delayFirst = (int) Math.floor(Math.min(minDelay.get(), maxDelay.get()));
                    final int delaySecond = (int) Math.ceil(Math.max(minDelay.get(), maxDelay.get()));

                    decidedTimer = RandomUtils.nextInt(delayFirst, delaySecond);
                    timer.reset();
                }
            }
        }
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