package wtf.demise.features.modules.impl.combat;

import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.misc.DebugUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.PlayerUtils;

import java.util.Objects;
import java.util.Random;

@ModuleInfo(name = "Velocity", category = ModuleCategory.Combat)
public class Velocity extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Cancel", "Reduce", "JumpReset", "Legit packet", "Intave test", "GrimC07"}, "Normal", this);
    private final SliderValue horizontal = new SliderValue("Horizontal", 0, 0, 100, 1, this, () -> Objects.equals(mode.get(), "Normal") || Objects.equals(mode.get(), "Cancel"));
    private final SliderValue vertical = new SliderValue("Vertical", 100, 0, 100, 1, this, () -> Objects.equals(mode.get(), "Normal") || Objects.equals(mode.get(), "Cancel"));
    private final SliderValue chance = new SliderValue("Chance", 100, 0, 100, 1, this);
    private final SliderValue rHurtTime = new SliderValue("HurtTime", 9, 1, 10, 1, this, () -> mode.is("Reduce"));
    private final SliderValue rFactorMin = new SliderValue("Factor (min)", 0.6f, 0, 1, 0.05f, this, () -> mode.is("Reduce"));
    private final SliderValue rFactorMax = new SliderValue("Factor (max)", 0.6f, 0, 1, 0.05f, this, () -> mode.is("Reduce"));
    private final BoolValue debug = new BoolValue("Debug", false, this, () -> Objects.equals(mode.get(), "Intave test"));
    private final SliderValue packets = new SliderValue("Packets", 5, 1, 20, 1, this, () -> Objects.equals(mode.get(), "Legit packet"));

    private double iFactor = 0.0;
    private boolean attacked;
    private boolean shouldReduce;
    private Entity currentTarget;
    private boolean velocity;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (PlayerUtils.nullCheck() && mc.thePlayer.maxHurtTime > 0) {
            if (chance.get() != 100.0D) {
                if (new Random().nextInt(100) <= chance.get()) {
                    return;
                }
            }

            switch (mode.get()) {
                case "Normal":
                    if (mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) {
                        if (horizontal.get() != 100.0D) {
                            mc.thePlayer.motionX *= horizontal.get() / 100.0D;
                            mc.thePlayer.motionZ *= horizontal.get() / 100.0D;
                        }

                        if (vertical.get() != 100.0D) {
                            mc.thePlayer.motionY *= vertical.get() / 100.0D;
                        }
                    }
                    break;
                case "JumpReset":
                    if (mc.thePlayer.onGround && mc.thePlayer.hurtTime > 5) {
                        mc.thePlayer.jump();
                    }
                    break;
                case "Intave test":
                    if (mc.thePlayer.hurtTime < 5) {
                        iFactor -= 0.005;
                    }

                    if (iFactor > 1 || iFactor < 0) {
                        iFactor = 0.0;
                    }

                    if (!Range.between(5, 7).contains(mc.thePlayer.hurtTime)) {
                        attacked = false;
                    }
                    break;
                case "Legit packet":
                    if (Range.between(7, 9).contains(mc.thePlayer.hurtTime)) {
                        if (shouldReduce) {
                            for (int i = 0; i < packets.get(); i++) {
                                AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);

                                DebugUtils.sendMessage(String.valueOf(i + 1));
                            }

                            currentTarget = null;
                            shouldReduce = false;
                        }
                    } else {
                        attacked = false;
                    }
                    break;
                case "GrimC07":
                    if (velocity) {
                        PacketUtils.sendPacket(
                                new C07PacketPlayerDigging((mc.objectMouseOver != null && mc.thePlayer.isSwingInProgress && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? C07PacketPlayerDigging.Action.START_DESTROY_BLOCK : C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK), new BlockPos(mc.thePlayer), EnumFacing.UP)
                        );

                        velocity = false;
                    }
                    break;
            }
        }

        this.setTag(mode.get());
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (Objects.equals(mode.get(), "Intave test") && debug.get()) {
            Fonts.interSemiBold.get(15).drawCenteredString(String.valueOf(iFactor), (float) event.getScaledResolution().getScaledWidth() / 2, (float) event.getScaledResolution().getScaledHeight() / 2 - 30, -1);
        }
    }

    @EventTarget
    public void onReceivePacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.INCOMING) {
            return;
        }

        switch (mode.get()) {
            case "Cancel":
                if (e.getPacket() instanceof S12PacketEntityVelocity packet) {
                    if (packet.getEntityID() == mc.thePlayer.getEntityId()) {

                        if (horizontal.get() != 100.0D) {
                            mc.thePlayer.motionX = ((double) packet.getMotionX() / 8000) * horizontal.get() / 100.0;
                            mc.thePlayer.motionZ = ((double) packet.getMotionZ() / 8000) * horizontal.get() / 100.0;
                        }

                        if (vertical.get() != 100.0D) {
                            mc.thePlayer.motionY = ((double) packet.getMotionY() / 8000) * vertical.get() / 100.0;
                        }

                        e.setCancelled(true);
                    }
                }
                break;
            case "GrimC07":
                final Packet<?> packet = e.getPacket();
                if (e.isCancelled()) return;

                if (packet instanceof S12PacketEntityVelocity wrapper) {
                    if (wrapper.getEntityID() == mc.thePlayer.getEntityId()) {
                        e.setCancelled(true);

                        velocity = true;
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        switch (mode.get()) {
            case "Reduce":
                if (mc.thePlayer.hurtTime == rHurtTime.get()) {
                    double factor = rand.nextDouble(rFactorMin.get(), rFactorMax.get());

                    mc.thePlayer.motionX *= factor;
                    mc.thePlayer.motionZ *= factor;
                }
                break;
            case "Intave test":
                if (Range.between(5, 7).contains(mc.thePlayer.hurtTime) && !attacked) {
                    mc.thePlayer.motionX *= iFactor;
                    mc.thePlayer.motionZ *= iFactor;

                    iFactor += rand.nextDouble(0.2, 0.5);

                    attacked = true;
                }
                break;
            case "Legit packet":
                if (Range.between(7, 9).contains(mc.thePlayer.hurtTime) && !attacked) {
                    currentTarget = e.getTargetEntity();
                    shouldReduce = true;
                    attacked = true;
                }
                break;
        }
    }
}