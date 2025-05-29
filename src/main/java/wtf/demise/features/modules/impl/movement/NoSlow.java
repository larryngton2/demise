package wtf.demise.features.modules.impl.movement;

import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.SlowDownEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.packet.PacketUtils;

import static net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM;

@ModuleInfo(name = "NoSlow", description = "Modifies item-use slowdown.", category = ModuleCategory.Movement)
public class NoSlow extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Intave", "NCP", "Prediction"}, "Vanilla", this);
    public final ModeValue intaveMode = new ModeValue("Intave mode", new String[]{"Release", "Old", "Test"}, "Release", this, () -> mode.is("Intave"));
    public final SliderValue speed = new SliderValue("Speed", 1, 0, 1, 0.1f, this);
    private final SliderValue amount = new SliderValue("Amount", 2, 2, 5, 1, this, () -> mode.is("Prediction"));
    private final BoolValue sprint = new BoolValue("Sprint", true, this);
    private final BoolValue sword = new BoolValue("Sword", true, this);
    private final BoolValue consumable = new BoolValue("Consumable", true, this);
    private final BoolValue bow = new BoolValue("Bow", true, this);

    private boolean ncpShouldWork = true;
    private boolean lastUsingItem;

    @EventTarget
    public void onMotion(MotionEvent event) {
        setTag(mode.get());

        if (!mc.thePlayer.isUsingItem() && mode.is("Intave") && intaveMode.is("Test")) {
            lastUsingItem = false;
        }

        if (!check()) return;

        switch (mode.get()) {
            case "Intave": {
                if (event.isPre()) {
                    switch (intaveMode.get()) {
                        case "Release":
                            if (isUsingConsumable()) {
                                sendPacketNoEvent(new C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP));
                            }
                            break;
                        case "Old":
                            if (isUsingConsumable()) {
                                sendPacketNoEvent(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
                                sendPacketNoEvent(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                            }
                            break;
                        case "Test":
                            if (isUsingConsumable()) {
                                if (!lastUsingItem) {
                                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP));
                                }
                            } else {
                                if (isUsingSword()) {
                                    PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                                }
                            }

                            lastUsingItem = true;
                            break;
                    }
                }
                break;
            }
            case "NCP":
                if (isUsingConsumable()) {
                    if (mc.thePlayer.isUsingItem() && ncpShouldWork) {
                        if (mc.thePlayer.ticksExisted % 3 == 0) {
                            sendPacketNoEvent(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, null, 0, 0, 0));
                        }
                    }
                } else if (isUsingSword()) {
                    if (event.isPre()) {
                        sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    }

                    if (event.isPost()) {
                        sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getCurrentEquippedItem()));
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!check()) return;

        final Packet<?> packet = event.getPacket();
        switch (mode.get()) {
            case "NCP":
                ncpShouldWork = !(packet instanceof C07PacketPlayerDigging);
                break;
        }
    }

    private boolean check() {
        if (mc.thePlayer.getCurrentEquippedItem() == null) return false;
        if (isUsingConsumable() && !consumable.get()) return false;
        if (isUsingBow() && !bow.get()) return false;
        return !isUsingSword() || sword.get();
    }

    public boolean isHoldingConsumable() {
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemFood || mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getMetadata()) || mc.thePlayer.getHeldItem().getItem() instanceof ItemBucketMilk;
    }

    public boolean isUsingConsumable() {
        return mc.thePlayer.isUsingItem() && isHoldingConsumable();
    }

    public boolean isUsingSword() {
        return mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    public boolean isUsingBow() {
        return mc.thePlayer.isUsingItem() && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow;
    }

    public boolean isInteractBlock(Block block) {
        return block instanceof BlockFence ||
                block instanceof BlockFenceGate ||
                block instanceof BlockDoor ||
                block instanceof BlockChest ||
                block instanceof BlockEnderChest ||
                block instanceof BlockEnchantmentTable ||
                block instanceof BlockFurnace ||
                block instanceof BlockAnvil ||
                block instanceof BlockBed ||
                block instanceof BlockWorkbench ||
                block instanceof BlockNote ||
                block instanceof BlockTrapDoor ||
                block instanceof BlockHopper ||
                block instanceof BlockDispenser ||
                block instanceof BlockDaylightDetector ||
                block instanceof BlockRedstoneRepeater ||
                block instanceof BlockRedstoneComparator ||
                block instanceof BlockButton ||
                block instanceof BlockBeacon ||
                block instanceof BlockBrewingStand ||
                block instanceof BlockSign;
    }

    @EventTarget
    public void onSlowDown(SlowDownEvent e) {
        if (!check()) return;

        if (mode.is("Prediction")) {
            if (mc.thePlayer.onGroundTicks % amount.get() != 0 && mc.thePlayer.isUsingItem()) {
                e.setCancelled(mc.thePlayer.onGround);
                return;
            }
        }

        e.setSprinting(sprint.get());

        e.setForward(speed.get());
        e.setStrafe(speed.get());
    }
}