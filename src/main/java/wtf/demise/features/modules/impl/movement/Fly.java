package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.MovementUtils;

@ModuleInfo(name = "Fly", category = ModuleCategory.Movement)
public class Fly extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Miniblox"}, "Vanilla", this);
    private final SliderValue moveSpeed = new SliderValue("Speed", 1.6f, 1, 10, 0.1f, this, () -> mode.is("Vanilla") || mode.is("Miniblox"));
    private final SliderValue upSpeed = new SliderValue("Up Speed", 0.5f, 0.1f, 5, 0.1f, this, () -> mode.is("Vanilla") || mode.is("Miniblox"));
    private final SliderValue downSpeed = new SliderValue("Down Speed", 0.5f, 0.1f, 5, 0.1f, this, () -> mode.is("Vanilla") || mode.is("Miniblox"));
    private final BoolValue cancelSetback = new BoolValue("Cancel Setback", true, this, () -> mode.is("Miniblox"));
    private final BoolValue autoResync = new BoolValue("Auto Resync", true, this, () -> mode.is("Miniblox"));
    private final BoolValue stop = new BoolValue("Stop", true, this, () -> mode.is("Miniblox"));
    private final SliderValue stopTicks = new SliderValue("Stop Ticks", 51, 5, 100, 1, this, () -> mode.is("Miniblox") && stop.get());
    private final BoolValue lockY = new BoolValue("Lock Y", true, this, () -> mode.is("Miniblox"));
    private final BoolValue offsetY = new BoolValue("Offset Y", true, this, () -> mode.is("Miniblox"));
    private final SliderValue stopY = new SliderValue("Y Offset", 0.55f, 0, 1.0f, 0.05f, this, () -> mode.is("Miniblox") && offsetY.get());
    private final BoolValue slow = new BoolValue("Slow", true, this, () -> mode.is("Miniblox"));
    private final SliderValue slowWaitTicks = new SliderValue("Slow Wait Ticks", 73, 5, 100f, 1, this, () -> mode.is("Miniblox") && slow.get());
    private final SliderValue slowAmount = new SliderValue("Slow Amount", 2.5f, 1, 3, 0.1f, this, () -> mode.is("Miniblox") && slow.get());

    private int ticksUntilStart;

    @Override
    public void onEnable() {
        ticksUntilStart = 0;
        if (mode.is("Miniblox") && autoResync.get()) {
            mc.thePlayer.sendChatMessage("/resync");
        }
    }

    @Override
    public void onDisable() {
        ticksUntilStart = 0;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        ticksUntilStart++;
        setTag(mode.get());
        switch (mode.get()) {
            case "Vanilla":
                if (!mc.thePlayer.isJumping) {
                    mc.thePlayer.motionY = 0.0D;
                }
                if (mc.thePlayer.isJumping) {
                    mc.thePlayer.motionY = upSpeed.get();
                }
                if (mc.thePlayer.isSneaking()) {
                    mc.thePlayer.motionY = -downSpeed.get();
                }

                MovementUtils.strafe(moveSpeed.get());

                break;
            case "Miniblox":
                if (offsetY.get())
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + stopY.get(), mc.thePlayer.posZ);
                    }

                if (ticksUntilStart <= stopTicks.get() && stop.get()) {
                    mc.thePlayer.motionY = 0.0D;
                }

                if (stop.get())
                    if (ticksUntilStart >= stopTicks.get()) {
                        if (!mc.thePlayer.isJumping) {
                            mc.thePlayer.motionY = 0.0D;
                        }
                        if (!lockY.get()) {
                            if (mc.thePlayer.isJumping)
                                mc.thePlayer.motionY = upSpeed.get();
                        }
                        if (!lockY.get()) {
                            if (mc.thePlayer.isSneaking())
                                mc.thePlayer.motionY = -downSpeed.get();
                        }

                        if (lockY.get()) {
                            mc.thePlayer.motionY = 0.0D;
                        }

                        MovementUtils.setSpeed(moveSpeed.get());
                    }

                if (!stop.get()) {
                    if (!mc.thePlayer.isJumping) {
                        mc.thePlayer.motionY = 0.0D;
                    }
                    if (!lockY.get()) {
                        if (mc.thePlayer.isJumping)
                            mc.thePlayer.motionY = upSpeed.get();
                    }
                    if (!lockY.get()) {
                        if (mc.thePlayer.isSneaking())
                            mc.thePlayer.motionY = -downSpeed.get();
                    }

                    if (lockY.get()) {
                        mc.thePlayer.motionY = 0.0D;
                    }

                    MovementUtils.setSpeed(moveSpeed.get());
                }

                if (slow.get())
                    if (ticksUntilStart >= slowWaitTicks.get()) {
                        if (!mc.thePlayer.isJumping) {
                            mc.thePlayer.motionY = 0.0D;
                        }
                        if (!lockY.get()) {
                            if (mc.thePlayer.isJumping)
                                mc.thePlayer.motionY = upSpeed.get();
                        }
                        if (!lockY.get()) {
                            if (mc.thePlayer.isSneaking())
                                mc.thePlayer.motionY = -downSpeed.get();
                        }

                        if (lockY.get()) {
                            mc.thePlayer.motionY = 0.0D;
                        }

                        MovementUtils.setSpeed(moveSpeed.get() / slowAmount.get());
                    }
                break;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.thePlayer == null) return;

        switch (mode.get()) {
            case "Miniblox":
                if (cancelSetback.get()) {
                    if (e.getPacket() instanceof S08PacketPlayerPosLook s08 && mc.thePlayer.ticksExisted >= 100) {
                        e.setCancelled(true);

                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(
                                        s08.getX(),
                                        s08.getY(),
                                        s08.getZ(),
                                        s08.getYaw(),
                                        s08.getPitch(),
                                        mc.thePlayer.onGround
                                )
                        );

                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(
                                        mc.thePlayer.posX,
                                        mc.thePlayer.posY,
                                        mc.thePlayer.posZ,
                                        mc.thePlayer.rotationYaw,
                                        mc.thePlayer.rotationPitch,
                                        mc.thePlayer.onGround
                                )
                        );
                    }
                }
                break;
        }
    }
}