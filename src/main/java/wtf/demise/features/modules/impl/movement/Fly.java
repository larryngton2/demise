package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.DebugUtils;
import wtf.demise.utils.player.MovementUtils;

import java.util.Objects;

@ModuleInfo(name = "Fly", category = ModuleCategory.Movement)
public class Fly extends Module {

    public final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "GrimTNT", "Miniblox"}, "Vanilla", this);
    private final SliderValue speed = new SliderValue("Speed", 2f, 1f, 5f, 0.1f, this, () -> mode.is("Vanilla"));

    private Boolean flight;
    private boolean jumped;
    private int currentTimer = 0;
    private int activeTicks = 0;
    private double mSpeed = 5;

    public void onDisable() {
        if (mc.thePlayer == null)
            return;

        if (mc.thePlayer.capabilities.isFlying) {
            mc.thePlayer.capabilities.isFlying = false;
        }

        mc.thePlayer.capabilities.setFlySpeed(0.05F);
        flight = false;
        jumped = false;
        currentTimer = 0;
        activeTicks = 0;
        mSpeed = 5;
        MovementUtils.stop();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        switch (mode.get()) {
            case "Vanilla":
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806D * speed.get()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
            case "Miniblox":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                activeTicks++;

                if (activeTicks <= 20) {
                    MovementUtils.stop();
                } else {
                    if (!jumped) {
                        if (mc.thePlayer.onGround) {
                            MovementUtils.stop();
                            mc.thePlayer.jump();
                        }

                        jumped = true;
                    } else {
                        DebugUtils.sendMessage(String.valueOf(mSpeed));

                        mc.thePlayer.motionX = mSpeed * -Math.sin(MovementUtils.getDirection());
                        mc.thePlayer.motionY = 0;
                        mc.thePlayer.motionZ = mSpeed * Math.cos(MovementUtils.getDirection());

                        mSpeed -= 0.5;

                        currentTimer++;

                        if (Range.between(4, 20).contains(currentTimer)) {
                            MovementUtils.stop();
                        } else if (currentTimer > 20) {
                            currentTimer = 0;
                            mSpeed = 4.5;
                        }
                    }
                }
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