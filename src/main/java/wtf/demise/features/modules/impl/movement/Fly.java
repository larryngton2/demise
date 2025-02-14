package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MovementUtils;

@ModuleInfo(name = "Fly", category = ModuleCategory.Movement)
public class Fly extends Module {

    public final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "GrimTNT"}, "Vanilla", this);
    private final SliderValue speed = new SliderValue("Speed", 2f, 1f, 5f, 0.1f, this, () -> mode.is("Vanilla"));

    private Boolean flight;

    public void onDisable() {
        switch (mode.get()) {
            case "Vanilla":
                if (Minecraft.getMinecraft().thePlayer == null)
                    return;

                if (Minecraft.getMinecraft().thePlayer.capabilities.isFlying) {
                    Minecraft.getMinecraft().thePlayer.capabilities.isFlying = false;
                }

                Minecraft.getMinecraft().thePlayer.capabilities.setFlySpeed(0.05F);
                break;
            case "GrimTNT":
                flight = false;
                break;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        switch (mode.get()) {
            case "Vanilla":
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806D * speed.get()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            switch (mode.get()) {
                case "GrimTNT":
                    if (flight) {
                        final double yaw = Math.toRadians(MovementUtils.getDirection());

                        e.setX(mc.thePlayer.posX - Math.sin(yaw) * 500);
                        e.setY(MovementUtils.predictedMotion(mc.thePlayer.motionY));
                        e.setZ(mc.thePlayer.posZ + Math.cos(yaw) * 500);
                    }
                    break;
            }
        }
    }

    @EventTarget
    public void onReceivePacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING) {
            switch (mode.get()) {
                case "GrimTNT":
                    if (e.getPacket() instanceof S12PacketEntityVelocity packet) {
                        if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
                            return;
                        }

                        flight = true;
                    }
                    break;
            }
        }
    }
}