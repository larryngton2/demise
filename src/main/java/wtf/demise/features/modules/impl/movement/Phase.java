package wtf.demise.features.modules.impl.movement;

import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.*;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.BlockAABBEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.DebugUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "Phase", category = ModuleCategory.Movement)
public class Phase extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Intave"}, "Vanilla", this);
    private final SliderValue amount = new SliderValue("Amount", 52, 1, 100, 1, this, () -> mode.is("Intave"));

    private boolean phasing;
    private boolean handle;
    private BlockPos pos;
    private EnumFacing sideHit;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        switch (mode.get()) {
            case "Vanilla":
                phasing = false;

                final double rotation = Math.toRadians(mc.thePlayer.rotationYaw);

                final double x = Math.sin(rotation);
                final double z = Math.cos(rotation);

                if (mc.thePlayer.isCollidedHorizontally) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX - x * 0.005, mc.thePlayer.posY, mc.thePlayer.posZ + z * 0.005);
                    phasing = true;
                } else if (PlayerUtils.insideBlock()) {
                    sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX - x * 1.5, mc.thePlayer.posY, mc.thePlayer.posZ + z * 1.5, false));

                    mc.thePlayer.motionX *= 0.3D;
                    mc.thePlayer.motionZ *= 0.3D;

                    phasing = true;
                }
                break;
            case "Intave":
                boolean check = mc.gameSettings.keyBindAttack.isKeyDown() && !handle && mc.thePlayer.rotationPitch > 80;

                if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && check) {
                    pos = mc.objectMouseOver.getBlockPos();
                    sideHit = mc.objectMouseOver.sideHit;
                    handle = true;
                }

                if (handle) {
                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, sideHit));
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - (amount.get() * 0.0001), mc.thePlayer.posZ);
                }

                if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK && handle) {
                    switch (mc.objectMouseOver.typeOfHit) {
                        case ENTITY -> DebugUtils.sendMessage("A fatass is blocking the way, can't phase");
                        case MISS -> DebugUtils.sendMessage("Stopped");
                    }

                    mc.thePlayer.jump();
                    handle = false;
                }
                break;
        }
    }

    @EventTarget
    public void onBlockAABB(BlockAABBEvent e) {
        if (mode.get().equals("Vanilla")) {
            if (e.getBlock() instanceof BlockAir && phasing) {
                final double x = e.getBlockPos().getX(), y = e.getBlockPos().getY(), z = e.getBlockPos().getZ();

                if (y < mc.thePlayer.posY) {
                    e.setBoundingBox(AxisAlignedBB.fromBounds(-15, -1, -15, 15, 1, 15).offset(x, y, z));
                }
            }
        }
    }
}