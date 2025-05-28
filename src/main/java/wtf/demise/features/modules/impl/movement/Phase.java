package wtf.demise.features.modules.impl.movement;

import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.BlockAABBEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationUtils;

@ModuleInfo(name = "Phase", description = "Allows you to phase through blocks.", category = ModuleCategory.Movement)
public class Phase extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Intave"}, "Vanilla", this);
    private final ModeValue intaveMode = new ModeValue("Intave mode", new String[]{"Manual", "SWAuto"}, "Manual", this, () -> mode.is("Intave"));

    private boolean phasing;
    private boolean handle;
    private BlockPos pos;
    private EnumFacing sideHit;
    private boolean phaseNow;
    private double startY;
    private boolean setY;
    private final TimerUtils timer = new TimerUtils();

    @Override
    public void onEnable() {
        if (mode.is("Intave") && intaveMode.is("Manual")) {
            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Intave Phase", "Sneak to move forward in blocks.", 10);
            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Intave Phase", "LMB to start.", 10);
        }
    }

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
                boolean check;

                if (intaveMode.is("SWAuto")) {
                    boolean check1 = phaseNow && timer.hasTimeElapsed(500);

                    check = check1;

                    if (check1) {
                        if (!setY) {
                            startY = mc.thePlayer.posY;
                            setY = true;
                        }

                        RotationUtils.setRotation(new float[]{mc.thePlayer.rotationYaw, 89.9f}, MovementCorrection.Silent);

                        if (startY - mc.thePlayer.posY > 2) {
                            timer.reset();
                            setY = false;
                            check = false;
                            phaseNow = false;
                        }
                    }
                } else {
                    check = mc.gameSettings.keyBindAttack.isKeyDown() && mc.thePlayer.rotationPitch > 80;
                }

                if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && check) {
                    pos = mc.objectMouseOver.getBlockPos();
                    sideHit = mc.objectMouseOver.sideHit;

                    if (!handle) {
                        handle = true;
                    }
                }

                if (handle) {
                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, sideHit));
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.0052, mc.thePlayer.posZ);
                }

                if (!check) {
                    handle = false;
                }

                if (intaveMode.is("Manual")) {
                    if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK && handle) {
                        switch (mc.objectMouseOver.typeOfHit) {
                            case ENTITY -> ChatUtils.sendMessageClient("A fatass is blocking the way, can't phase");
                            case MISS -> ChatUtils.sendMessageClient("Stopped");
                        }

                        mc.thePlayer.jump();
                        handle = false;
                    }

                    if (mc.thePlayer.isSneaking()) {
                        final double wDist = 0.00001D;
                        final double aDist = 0.00001D;
                        final double sDist = -0.00001D;
                        final double dDist = -0.00001D;

                        final double rotationn = Math.toRadians(mc.thePlayer.rotationYaw);

                        if (mc.gameSettings.keyBindForward.isKeyDown()) {
                            final double xx = Math.sin(rotationn) * wDist;
                            final double zz = Math.cos(rotationn) * wDist;

                            mc.thePlayer.setPosition(mc.thePlayer.posX - xx, mc.thePlayer.posY, mc.thePlayer.posZ + zz);
                        }

                        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                            final double xx = Math.sin(rotationn) * aDist;

                            mc.thePlayer.setPosition(mc.thePlayer.posX + xx, mc.thePlayer.posY, mc.thePlayer.posZ);
                        }

                        if (mc.gameSettings.keyBindBack.isKeyDown()) {
                            final double xx = Math.sin(rotationn) * sDist;
                            final double zz = Math.cos(rotationn) * sDist;

                            mc.thePlayer.setPosition(mc.thePlayer.posX - xx, mc.thePlayer.posY, mc.thePlayer.posZ + zz);
                        }

                        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
                            final double xx = Math.sin(rotationn) * dDist;

                            mc.thePlayer.setPosition(mc.thePlayer.posX + xx, mc.thePlayer.posY, mc.thePlayer.posZ);
                        }
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mode.is("Intave") && intaveMode.is("SWAuto")) {
            if (e.getPacket() instanceof S45PacketTitle s45PacketTitle) {
                if (s45PacketTitle.getMessage().getFormattedText().contains("2")) {
                    phaseNow = true;
                    timer.reset();
                }
            }
        }
    }

    @EventTarget
    public void onBlockAABB(BlockAABBEvent e) {
        if (mode.is("Vanilla")) {
            if (e.getBlock() instanceof BlockAir && phasing) {
                final double x = e.getBlockPos().getX(), y = e.getBlockPos().getY(), z = e.getBlockPos().getZ();

                if (y < mc.thePlayer.posY) {
                    e.setBoundingBox(AxisAlignedBB.fromBounds(-15, -1, -15, 15, 1, 15).offset(x, y, z));
                }
            }
        }
    }
}