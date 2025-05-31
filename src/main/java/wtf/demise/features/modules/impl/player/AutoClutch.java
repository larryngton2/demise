package wtf.demise.features.modules.impl.player;

import net.minecraft.block.*;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;

import java.util.Arrays;
import java.util.List;

//todo improaventtmt
@ModuleInfo(name = "AutoClutch", description = "Automatically clutches for you when falling.", category = ModuleCategory.Player)
public class AutoClutch extends Module {
    private final BoolValue sneak = new BoolValue("Sneak", false, this);
    private final ModeValue clutchRotMode = new ModeValue("Clutch rotation mode", new String[]{"Normal", "Center"}, "Center", this);
    private final SliderValue minCSearch = new SliderValue("Min C search", 0.1f, 0.01f, 1f, 0.01f, this, () -> clutchRotMode.canDisplay() && clutchRotMode.is("Normal"));
    private final SliderValue maxCSearch = new SliderValue("Max C search", 0.9f, 0.01f, 1f, 0.01f, this, () -> clutchRotMode.canDisplay() && clutchRotMode.is("Normal"));
    private final RotationHandler rotationHandler = new RotationHandler(this);
    private final BoolValue rayTrace = new BoolValue("Ray Trace", true, this);
    private final BoolValue swing = new BoolValue("Swing", true, this);

    private final List<Block> blacklistedBlocks = Arrays.asList(Blocks.air, Blocks.water, Blocks.flowing_water, Blocks.lava, Blocks.wooden_slab, Blocks.chest, Blocks.flowing_lava,
            Blocks.enchanting_table, Blocks.carpet, Blocks.glass_pane, Blocks.skull, Blocks.stained_glass_pane, Blocks.iron_bars, Blocks.snow_layer, Blocks.ice, Blocks.packed_ice,
            Blocks.coal_ore, Blocks.diamond_ore, Blocks.emerald_ore, Blocks.trapped_chest, Blocks.torch, Blocks.anvil,
            Blocks.noteblock, Blocks.jukebox, Blocks.tnt, Blocks.gold_ore, Blocks.iron_ore, Blocks.lapis_ore, Blocks.lit_redstone_ore, Blocks.quartz_ore, Blocks.redstone_ore,
            Blocks.wooden_pressure_plate, Blocks.stone_pressure_plate, Blocks.light_weighted_pressure_plate, Blocks.heavy_weighted_pressure_plate,
            Blocks.stone_button, Blocks.wooden_button, Blocks.lever, Blocks.tallgrass, Blocks.tripwire, Blocks.tripwire_hook, Blocks.rail, Blocks.waterlily, Blocks.red_flower,
            Blocks.red_mushroom, Blocks.brown_mushroom, Blocks.vine, Blocks.trapdoor, Blocks.yellow_flower, Blocks.ladder, Blocks.furnace, Blocks.sand, Blocks.cactus,
            Blocks.dispenser, Blocks.noteblock, Blocks.dropper, Blocks.crafting_table, Blocks.pumpkin, Blocks.sapling, Blocks.cobblestone_wall,
            Blocks.oak_fence, Blocks.activator_rail, Blocks.detector_rail, Blocks.golden_rail, Blocks.redstone_torch, Blocks.acacia_stairs,
            Blocks.birch_stairs, Blocks.brick_stairs, Blocks.dark_oak_stairs, Blocks.jungle_stairs, Blocks.nether_brick_stairs, Blocks.oak_stairs,
            Blocks.quartz_stairs, Blocks.red_sandstone_stairs, Blocks.sandstone_stairs, Blocks.spruce_stairs, Blocks.stone_brick_stairs, Blocks.stone_stairs, Blocks.double_wooden_slab, Blocks.stone_slab, Blocks.double_stone_slab, Blocks.stone_slab2, Blocks.double_stone_slab2,
            Blocks.web, Blocks.gravel, Blocks.daylight_detector_inverted, Blocks.daylight_detector, Blocks.soul_sand, Blocks.piston, Blocks.piston_extension,
            Blocks.piston_head, Blocks.sticky_piston, Blocks.iron_trapdoor, Blocks.ender_chest, Blocks.end_portal, Blocks.end_portal_frame, Blocks.standing_banner,
            Blocks.wall_banner, Blocks.deadbush, Blocks.slime_block, Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.dark_oak_fence_gate,
            Blocks.jungle_fence_gate, Blocks.spruce_fence_gate, Blocks.oak_fence_gate);

    private int oldSlot;
    private BlockPos previousBlock;
    public BlockPos targetBlock;
    public boolean placed;
    public PlaceData data;
    private float yaw, pitch;
    private boolean startClutch;
    private final TimerUtils clutchTime = new TimerUtils();
    private boolean fsgdfdds;

    @Override
    public void onEnable() {
        oldSlot = mc.thePlayer.inventory.currentItem;
    }

    @Override
    public void onDisable() {
        mc.thePlayer.inventory.currentItem = oldSlot;
        SpoofSlotUtils.stopSpoofing();
    }

    private void handleScaffolding() {
        data = null;

        double posY = mc.thePlayer.getEntityBoundingBox().minY;

        targetBlock = new BlockPos(mc.thePlayer.posX, posY - 1, mc.thePlayer.posZ);

        data = findBlock(targetBlock);

        if (data == null || data.blockPos == null || data.facing == null || getBlockSlot() == -1 || isEnabled(KillAura.class) && KillAura.currentTarget != null && !(mc.theWorld.getBlockState(targetBlock).getBlock() instanceof BlockAir))
            return;

        boolean isLeaningOffBlock = PlayerUtils.getBlock(targetBlock.offset(data.facing.getOpposite())) instanceof BlockAir;
        boolean nextBlockIsAir = mc.theWorld.getBlockState(mc.thePlayer.getPosition().offset(EnumFacing.fromAngle(yaw), 1).down()).getBlock() instanceof BlockAir;
        boolean shouldCorrect = isLeaningOffBlock && nextBlockIsAir;

        if (shouldCorrect) {
            startClutch = true;
            oldSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = getBlockSlot();
            SpoofSlotUtils.startSpoofing(oldSlot);
        } else if (startClutch) {
            clutchTime.reset();
            startClutch = false;
        }

        if (startClutch || !clutchTime.hasTimeElapsed(200)) {
            Vec3 hitVec = getVec3(data);

            switch (clutchRotMode.get()) {
                case "Normal": {
                    this.yaw = getBestRotation(data.blockPos, data.facing, minCSearch.get(), maxCSearch.get())[0];
                    this.pitch = getBestRotation(data.blockPos, data.facing, minCSearch.get(), maxCSearch.get())[1];
                }
                break;
                case "Center": {
                    this.yaw = RotationUtils.getRotations(hitVec)[0];
                    this.pitch = RotationUtils.getRotations(hitVec)[1];
                }
                break;
            }

            rotationHandler.setRotation(new float[]{yaw, pitch});
            place(data.blockPos, data.facing, getVec3(data));
            fsgdfdds = true;
        } else {
            if (fsgdfdds) {
                mc.thePlayer.inventory.currentItem = oldSlot;
                SpoofSlotUtils.stopSpoofing();
                fsgdfdds = false;
            }
        }
    }

    @EventTarget
    private void onUpdate(UpdateEvent e) {
        rotationHandler.updateRotSpeed(e);
        handleScaffolding();
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (startClutch || !clutchTime.hasTimeElapsed(200)) {
            RotationUtils.enabled = true;
        }
    }

    @EventTarget
    public void onMovementInput(MoveInputEvent e) {
        if (sneak.get() && (startClutch || !clutchTime.hasTimeElapsed(200))) {
            e.setSneaking(true);
        }
    }

    private void place(BlockPos pos, EnumFacing facing, Vec3 hitVec) {
        if (rayTrace.get()) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), pos, facing, hitVec)) {
                if (swing.get()) {
                    mc.thePlayer.swingItem();
                    mc.getItemRenderer().resetEquippedProgress();
                } else {
                    mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                }
                placed = true;
            }
            previousBlock = data.blockPos.offset(data.facing);
        } else {
            MovingObjectPosition ray = RotationUtils.rayTrace(4.5, 1);

            if (ray.getBlockPos().equalsBlockPos(pos)) {
                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), ray.getBlockPos(), ray.sideHit, ray.hitVec)) {
                    if (swing.get()) {
                        mc.thePlayer.swingItem();
                        mc.getItemRenderer().resetEquippedProgress();
                    } else {
                        mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                    }
                    placed = true;
                }
            }

            previousBlock = ray.getBlockPos().offset(ray.sideHit);
        }
    }

    private int getBlockSlot() {
        int slot = mc.thePlayer.inventory.currentItem;

        if (getBlockCount() == 0) {
            return slot;
        }

        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];

            if (itemStack != null) {
                final Item item = itemStack.getItem();

                if (item instanceof ItemBlock && !blacklistedBlocks.contains(((ItemBlock) item).getBlock())) {
                    slot = i;
                }
            }
        }

        return slot;
    }

    public int getBlockCount() {
        int blockCount = 0;

        for (int i = 36; i < 45; ++i) {
            if (!mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) continue;

            final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if (!(is.getItem() instanceof ItemBlock && !blacklistedBlocks.contains(((ItemBlock) is.getItem()).getBlock()))) {
                continue;
            }

            blockCount += is.stackSize;
        }

        return blockCount;
    }

    private Vec3 getVec3(PlaceData data) {
        BlockPos pos = data.blockPos;
        EnumFacing face = data.facing;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        x += face.getFrontOffsetX() / 2.0D;
        z += face.getFrontOffsetZ() / 2.0D;
        y += face.getFrontOffsetY() / 2.0D;

        return new Vec3(x, y, z);
    }

    private PlaceData findBlock(BlockPos pos) {
        EnumFacing[] facings = {EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP};
        BlockPos[] offsets = {
                new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0),
                new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
                new BlockPos(0, -1, 0)
        };

        if (previousBlock != null && previousBlock.getY() > mc.thePlayer.posY) {
            previousBlock = null;
        }

        PlaceData result = checkPositions(pos, offsets, facings);
        if (result != null) {
            return result;
        }

        for (BlockPos offset : offsets) {
            result = checkPositions(pos.add(offset), offsets, facings);
            if (result != null) {
                return result;
            }
        }

        for (BlockPos offset1 : offsets) {
            for (BlockPos offset2 : offsets) {
                result = checkPositions(pos.add(offset1).add(offset2), offsets, facings);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private PlaceData checkPositions(BlockPos basePos, BlockPos[] offsets, EnumFacing[] facings) {
        for (int i = 0; i < offsets.length; i++) {
            BlockPos newPos = basePos.add(offsets[i]);
            Block block = mc.theWorld.getBlockState(newPos).getBlock();

            if (newPos.equals(previousBlock)) {
                return new PlaceData(facings[i], newPos);
            }

            if (!block.getMaterial().isReplaceable() && !isInteractable(block)) {
                return new PlaceData(facings[i], newPos);
            }
        }
        return null;
    }

    public static float[] getBestRotation(BlockPos blockPos, EnumFacing face, float min, float max) {
        Vec3i faceVec = face.getDirectionVec();

        float minX, maxX, minY, maxY, minZ, maxZ;

        if (faceVec.getX() == 0) {
            minX = min;
            maxX = max;
        } else if (faceVec.getX() == 1) {
            minX = maxX = 1.0f;
        } else if (faceVec.getX() == -1) {
            minX = maxX = 0.0f;
        } else {
            minX = min;
            maxX = max;
        }

        if (faceVec.getY() == 0) {
            minY = min;
            maxY = max;
        } else if (faceVec.getY() == 1) {
            minY = maxY = 1.0f;
        } else if (faceVec.getY() == -1) {
            minY = maxY = 0.0f;
        } else {
            minY = min;
            maxY = max;
        }

        if (faceVec.getZ() == 0) {
            minZ = min;
            maxZ = max;
        } else if (faceVec.getZ() == 1) {
            minZ = maxZ = 1.0f;
        } else if (faceVec.getZ() == -1) {
            minZ = maxZ = 0.0f;
        } else {
            minZ = min;
            maxZ = max;
        }

        float[] bestRot = RotationUtils.getRotations(blockPos, face);
        double bestDist = RotationUtils.getRotationDifference(bestRot);

        for (float x = minX; x <= maxX; x += 0.01f) {
            for (float y = minY; y <= maxY; y += 0.01f) {
                for (float z = minZ; z <= maxZ; z += 0.01f) {
                    Vec3 candidateLocal = new Vec3(x, y, z);
                    Vec3 candidateWorld = candidateLocal.add(new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()));

                    double diff = RotationUtils.getRotationDifference(candidateWorld);
                    if (diff < bestDist) {
                        bestDist = diff;
                        bestRot = RotationUtils.getRotations(candidateWorld);
                    }
                }
            }
        }

        return bestRot;
    }

    private static boolean isInteractable(Block block) {
        return block instanceof BlockFurnace || block instanceof BlockFenceGate || block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockEnchantmentTable || block instanceof BlockBrewingStand || block instanceof BlockBed || block instanceof BlockDispenser || block instanceof BlockHopper || block instanceof BlockAnvil || block == Blocks.crafting_table;
    }

    private static class PlaceData {
        public EnumFacing facing;
        public BlockPos blockPos;

        PlaceData(EnumFacing enumFacing, BlockPos blockPos) {
            this.facing = enumFacing;
            this.blockPos = blockPos;
        }
    }
}