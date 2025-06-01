package wtf.demise.features.modules.impl.misc.bloxdphysics;

import com.sun.javafx.geom.Vec3d;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.potion.Potion;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.StrafeEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.ClickHandler;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "BloxdPhysics", category = ModuleCategory.Misc)
public class BloxdPhysics extends Module {
    private final NoaPhysics bloxdPhysics = new NoaPhysics();
    private double jumpfunny = 0;
    private long jumpticks = System.currentTimeMillis();

    @EventTarget
    public void packetEvent(PacketEvent e) {
        if (e.getPacket() instanceof S12PacketEntityVelocity packet) {
            if (mc.thePlayer != null && packet.getEntityID() == mc.thePlayer.getEntityId()) {
                jumpticks = System.currentTimeMillis() + 1300;
            }
        } else if (e.getPacket() instanceof S3FPacketCustomPayload packet) {
            if ("bloxd:resyncphysics".equals(packet.getChannelName())) {
                PacketBuffer data = packet.getBufferData();
                jumpfunny = 0;
                bloxdPhysics.impulseVector.set(0, 0, 0);
                bloxdPhysics.forceVector.set(0, 0, 0);
                bloxdPhysics.velocityVector.set(data.readFloat(), data.readFloat(), data.readFloat());
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent e) {
        if (mc.thePlayer.onGround && bloxdPhysics.velocityVector.y < 0) {
            bloxdPhysics.velocityVector.set(0, 0, 0);
        }

        if (mc.thePlayer.onGround && mc.thePlayer.motionY == (double) 0.42f) {
            jumpfunny = Math.min(jumpfunny + 1, 3);
            bloxdPhysics.impulseVector.add(new Vec3d(0, 8, 0));
        }

        jumpfunny = mc.thePlayer.onGroundTicks > 5 ? 0 : jumpfunny;
        double speed = jumpticks > System.currentTimeMillis() && mc.timer.timerSpeed == 1 ? 1d : (mc.thePlayer.isUsingItem() ? 0.06d : 0.26d + 0.025d * jumpfunny);

        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            switch (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier()) {
                case 0, 1:
                    speed += 0.14d;
                    break;
            }
        }

        e.setForward(0);
        e.setStrafe(0);
        //e.setFriction(0);

        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionZ = 0;
        bloxdPhysics.gravityMul = ClickHandler.clickingNow && bloxdPhysics.velocityVector.y >= 0 ? 4d : 2d;

        if (mc.theWorld.isBlockLoaded(mc.thePlayer.getPosition())) {
            mc.thePlayer.motionY = bloxdPhysics.getMotionForTick().y * (1 / 30d);
            MoveUtil.strafe(speed);
        }
    }
}