package wtf.demise.features.modules.impl.player;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.TeleportEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.rotation.RotationUtils;

@ModuleInfo(name = "BedNuker", description = "Automatically breaks beds around you.", category = ModuleCategory.Player)
public class BedNuker extends Module {
    public final SliderValue breakRange = new SliderValue("Break Range", 4.5f, 1, 6, 0.1f, this);
    private final RotationManager rotationManager = new RotationManager(this);
    public final BoolValue whitelistOwnBed = new BoolValue("Whitelist Own Bed", true, this);
    public BlockPos bedPos;
    public boolean rotate = false;
    private int breakTicks;
    private int delayTicks;
    private Vec3 home;

    @Override
    public void onEnable() {
        rotate = false;
        bedPos = null;

        breakTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset(true);
        super.onDisable();
    }

    @EventTarget
    public void onTeleport(TeleportEvent event) {
        if (whitelistOwnBed.get()) {
            final double distance = mc.thePlayer.getDistance(event.getPosX(), event.getPosY(), event.getPosZ());

            if (distance > 40) {
                home = new Vec3(event.getPosX(), event.getPosY(), event.getPosZ());
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
//        if (Demise.INSTANCE.getModuleManager().getModule(Scaffold.class).isEnabled() && getModule(Scaffold.class).data == null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
//            reset(true);
//            return;
//        }

        rotationManager.updateRotSpeed(e);

        getBedPos();

        if (bedPos != null) {
            if (rotate) {
                float[] rot = RotationUtils.getRotationToBlock(bedPos, getEnumFacing(bedPos));
                rotationManager.setRotation(rot);
                rotate = false;
            }
            mine(bedPos);
        } else {
            reset(true);
        }
    }

    private void getBedPos() {
        if (home != null && mc.thePlayer.getDistanceSq(home.xCoord, home.yCoord, home.zCoord) < 35 * 35 && whitelistOwnBed.get()) {
            return;
        }
        bedPos = null;
        double range = breakRange.get();
        for (double x = mc.thePlayer.posX - range; x <= mc.thePlayer.posX + range; x++) {
            for (double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - range; y <= mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + range; y++) {
                for (double z = mc.thePlayer.posZ - range; z <= mc.thePlayer.posZ + range; z++) {
                    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);

                    if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed && mc.theWorld.getBlockState(pos).getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                        bedPos = pos;
                        break;
                    }
                }
            }
        }
    }

    private void mine(BlockPos blockPos) {
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        IBlockState blockState = mc.theWorld.getBlockState(blockPos);

        if (blockState.getBlock() instanceof BlockAir) {
            return;
        }

        float totalBreakTicks = getBreakTicks(bedPos, mc.thePlayer.inventory.currentItem);
        if (breakTicks == 0) {
            rotate = true;
            mc.thePlayer.swingItem();
            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, bedPos, EnumFacing.UP));
        } else if (breakTicks >= totalBreakTicks) {
            rotate = true;
            mc.thePlayer.swingItem();
            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, bedPos, EnumFacing.UP));

            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, 1);

            reset(false);
            return;
        } else {
            rotate = true;

            mc.thePlayer.swingItem();
        }

        breakTicks += 1;

        int currentProgress = (int) (((double) breakTicks / totalBreakTicks) * 100);
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), bedPos, currentProgress / 10);
    }

    private void reset(boolean resetRotate) {
        if (bedPos != null) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), bedPos, -1);
            //test
            sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, bedPos, EnumFacing.DOWN));
        }

        breakTicks = 0;
        delayTicks = 5;
        bedPos = null;
        rotate = !resetRotate;
    }

    private void doAutoTool(BlockPos pos) {
        if (PlayerUtils.findTool(pos) != -1)
            mc.thePlayer.inventory.currentItem = PlayerUtils.findTool(pos);
    }

    public static boolean isBed(BlockPos blockPos) {
        Block block = mc.theWorld.getBlockState(blockPos).getBlock();
        return block instanceof BlockBed;
    }

    public static EnumFacing getEnumFacing(BlockPos pos) {
        Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);

        if (pos.getY() > eyesPos.yCoord) {
            if (PlayerUtils.isReplaceable(pos.add(0, -1, 0))) {
                return EnumFacing.DOWN;
            } else {
                return mc.thePlayer.getHorizontalFacing().getOpposite();
            }
        }

        if (!PlayerUtils.isReplaceable(pos.add(0, 1, 0))) {
            return mc.thePlayer.getHorizontalFacing().getOpposite();
        }

        return EnumFacing.UP;
    }

    private float getBreakTicks(BlockPos bp, int tool) {
        int oldHeld = mc.thePlayer.inventory.currentItem;

        mc.thePlayer.inventory.currentItem = tool;
        IBlockState bs = mc.theWorld.getBlockState(bp);
        float ticks = 1f / bs.getBlock().getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, bp);

        mc.thePlayer.inventory.currentItem = oldHeld;
        return ticks;
    }
}
