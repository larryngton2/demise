package wtf.demise.features.modules.impl.player;

import net.minecraft.block.BlockBed;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.rotation.RotationUtils;

import static wtf.demise.utils.player.rotation.RotationHandler.currentRotation;

@ModuleInfo(name = "BedBreaker", description = "Automatically breaks beds around you.")
public class BedBreaker extends Module {
    private final BoolValue legit = new BoolValue("Legit", false, this);
    private final RotationManager rotationManager = new RotationManager(this);

    private BlockPos bedPos;
    private BlockPos cachedBedPos;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        rotationManager.updateRotSpeed(e);

        // checking every 2 ticks because that's fast enough
        if (mc.thePlayer.ticksExisted % 2 == 0) {
            getBedPos();
        }

        if (bedPos != null) {
            if (!legit.get()) {
                if (rayTraceBed().typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    mc.playerController.onPlayerDamageBlock(bedPos, mc.objectMouseOver.sideHit);
                    sendPacket(new C0APacketAnimation());
                } else {
                    mc.playerController.resetBlockRemoving();
                }
            } else {
                // the bed will break eventually since the player is aiming at it
                if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    mc.playerController.onPlayerDamageBlock(mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit);
                    sendPacket(new C0APacketAnimation());
                } else {
                    mc.playerController.resetBlockRemoving();
                }
            }
        } else {
            if (mc.playerController.currentBlock == cachedBedPos) {
                mc.playerController.resetBlockRemoving();
            }
        }
    }

    private MovingObjectPosition rayTraceBed() {
        Vec3 from = mc.thePlayer.getPositionEyes(1);
        Vec3 direction = mc.thePlayer.getLookCustom(currentRotation[0], currentRotation[1]);
        Vec3 to = from.addVector(direction.xCoord * 4.5, direction.yCoord * 4.5, direction.zCoord * 4.5);

        return PlayerUtils.getBlock(bedPos).collisionRayTrace(mc.theWorld, bedPos, from, to);
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (bedPos != null) {
            rotationManager.setRotation(RotationUtils.getRotations(bedPos));
        }
    }

    private void getBedPos() {
        bedPos = null;
        double nearestPos = Double.MAX_VALUE;
        for (double x = mc.thePlayer.posX - 4.5; x <= mc.thePlayer.posX + 4.5; x++) {
            for (double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - 4.5; y <= mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + 4.5; y++) {
                for (double z = mc.thePlayer.posZ - 4.5; z <= mc.thePlayer.posZ + 4.5; z++) {
                    BlockPos pos = new BlockPos((int) x, (int) y, (int) z);

                    if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed && pos.distanceTo(mc.thePlayer.getPositionVector()) < nearestPos) {
                        bedPos = pos;
                        cachedBedPos = pos;
                        nearestPos = pos.distanceTo(mc.thePlayer.getPositionVector());
                    }
                }
            }
        }
    }
}