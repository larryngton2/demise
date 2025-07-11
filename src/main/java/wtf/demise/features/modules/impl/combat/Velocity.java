package wtf.demise.features.modules.impl.combat;

import net.minecraft.block.BlockAir;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.BlockAABBEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.HitSlowDownEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "Velocity", description = "Minimises or removes knockback.")
public class Velocity extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Cancel", "Reduce", "Legit", "GrimC07", "Intave", "Collision", "ReStrafe", "Ground"}, "Normal", this);
    private final SliderValue chance = new SliderValue("Chance", 100, 0, 100, 1, this);
    private final BoolValue explosion = new BoolValue("Explosion", false, this);

    public final ModeValue intaveMode = new ModeValue("Intave mode", new String[]{"Tick Reduce", "Reduce"}, "Reduce", this, () -> mode.is("Intave"));

    private final SliderValue horizontal = new SliderValue("Horizontal", 0, 0, 100, 1, this, () -> mode.is("Normal"));
    private final SliderValue vertical = new SliderValue("Vertical", 100, 0, 100, 1, this, () -> mode.is("Normal") || mode.is("ReStrafe"));

    private final SliderValue mHurtTime = new SliderValue("Min hurtTime", 9, 1, 10, 1, this, () -> (mode.is("Intave") && intaveMode.is("Tick Reduce")));
    private final SliderValue mmHurtTime = new SliderValue("Max hurtTime", 10, 1, 10, 1, this, () -> (mode.is("Intave") && intaveMode.is("Tick Reduce")));

    public final SliderValue rFactorMin = new SliderValue("Factor (min)", 0.6f, 0, 1, 0.05f, this, () -> mode.is("Reduce") || (mode.is("Intave") && !intaveMode.is("Test")));
    public final SliderValue rFactorMax = new SliderValue("Factor (max)", 0.6f, 0, 1, 0.05f, this, () -> mode.is("Reduce") || (mode.is("Intave") && !intaveMode.is("Test")));

    private final SliderValue maxAttacks = new SliderValue("Max attacks", 5, 1, 10, 1, this, () -> mode.is("Legit"));

    private final SliderValue ticks = new SliderValue("Ticks", 0, 0, 6, 1, this, () -> mode.is("ReStrafe"));

    private final BoolValue motionGround = new BoolValue("Motion ground", false, this, () -> mode.is("Ground"));

    private final BoolValue onSwing = new BoolValue("On swing", false, this);

    private boolean attacked;
    private boolean velocity;
    private int sentPackets;

    @EventTarget
    public void onMotion(MotionEvent e) {
        setTag(mode.get());

        if ((onSwing.get() && !mc.thePlayer.isSwingInProgress) || (chance.get() != 100.0D && rand.nextInt(100) <= chance.get()) || e.isPost()) return;

        switch (mode.get()) {
            case "GrimC07":
                if (velocity) {
                    PacketUtils.sendPacket(
                            new C07PacketPlayerDigging((mc.objectMouseOver != null && mc.thePlayer.isSwingInProgress && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? C07PacketPlayerDigging.Action.START_DESTROY_BLOCK : C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK), new BlockPos(mc.thePlayer), EnumFacing.UP)
                    );

                    velocity = false;
                }
                break;
            case "ReStrafe":
                if (mc.thePlayer.hurtTime == 9 - ticks.get()) {
                    if (MoveUtil.isMoving()) {
                        MoveUtil.strafe();
                    } else {
                        mc.thePlayer.motionZ *= -1;
                        mc.thePlayer.motionX *= -1;
                    }

                    if (vertical.get() != 100.0D) {
                        mc.thePlayer.motionY *= vertical.get() / 100.0D;
                    }
                }
                break;
            case "Legit":
                if (mc.thePlayer.hurtTime != 0) {
                    if (!attacked && sentPackets < maxAttacks.get() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && !mc.thePlayer.isUsingItem()) {
                        ChatUtils.sendMessageClient("Reduced");
                        mc.clickMouse();
                        sentPackets++;
                    }
                } else {
                    sentPackets = 0;
                }

                attacked = false;
                break;
            case "Ground":
                if (velocity) {
                    mc.thePlayer.onGround = true;
                    if (motionGround.get()) {
                        e.setOnGround(true);
                    }
                    velocity = false;
                }
                break;
        }
    }

    @EventTarget
    public void onBlockAABB(BlockAABBEvent e) {
        if (onSwing.get() && !mc.thePlayer.isSwingInProgress) return;

        if (mode.is("Collision")) {
            if (e.getBlock() instanceof BlockAir && mc.thePlayer.hurtTime >= 1) {
                final double x = e.getBlockPos().getX(), y = e.getBlockPos().getY(), z = e.getBlockPos().getZ();

                if (y == Math.floor(mc.thePlayer.posY) + 1) {
                    e.setBoundingBox(AxisAlignedBB.fromBounds(0, 0, 0, 1, 0, 1).offset(x, y, z));
                }
            }
        }
    }

    @EventTarget
    public void onReceivePacket(PacketEvent e) {
        if (e.getState() != PacketEvent.State.INCOMING) {
            return;
        }

        if (e.getPacket() instanceof S12PacketEntityVelocity s12 && s12.getEntityID() == mc.thePlayer.getEntityId()) {
            switch (mode.get()) {
                case "Normal":
                    applyMotionModifiers(s12);
                    break;
                case "Cancel":
                    e.setCancelled(true);
                    break;
                case "GrimC07":
                    e.setCancelled(true);
                    velocity = true;
                    break;
                case "Ground":
                    velocity = true;
                    break;
            }
        } else if (e.getPacket() instanceof S27PacketExplosion s27 && explosion.get()) {
            switch (mode.get()) {
                case "Normal":
                    applyMotionModifiers(s27);
                    break;
                case "Cancel":
                    e.setCancelled(true);
                    break;
                case "GrimC07":
                    e.setCancelled(true);
                    velocity = true;
                    break;
                case "Ground":
                    velocity = true;
                    break;
            }
        }
    }

    private void applyMotionModifiers(S12PacketEntityVelocity s12) {
        if (horizontal.get() != 100) {
            if (horizontal.get() != 0) {
                s12.motionX *= (int) (horizontal.get() / 100);
                s12.motionZ *= (int) (horizontal.get() / 100);
            } else {
                s12.motionX = (int) (mc.thePlayer.motionX * 8000);
                s12.motionZ = (int) (mc.thePlayer.motionZ * 8000);
            }
        }

        if (vertical.get() != 100) {
            if (vertical.get() != 0) {
                s12.motionY *= (int) (vertical.get() / 100);
            } else {
                s12.motionY = (int) (mc.thePlayer.motionY * 8000);
            }
        }
    }

    private void applyMotionModifiers(S27PacketExplosion s27) {
        if (horizontal.get() != 100) {
            if (horizontal.get() != 0) {
                s27.posX *= horizontal.get() / 100.0;
                s27.posZ *= horizontal.get() / 100.0;
            } else {
                s27.posX = (int) (mc.thePlayer.motionX * 8000);
                s27.posZ = (int) (mc.thePlayer.motionZ * 8000);
            }
        }

        if (vertical.get() != 100) {
            if (vertical.get() != 0) {
                s27.posY *= vertical.get() / 100.0;
            } else {
                s27.posY = (int) (mc.thePlayer.motionY * 8000);
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        switch (mode.get()) {
            case "Intave":
                if (intaveMode.is("Tick Reduce")) {
                    if (Range.between((int) mHurtTime.get(), (int) mmHurtTime.get()).contains(mc.thePlayer.hurtTime)) {
                        double factor = MathUtils.nextDouble(rFactorMin.get(), rFactorMax.get());
                        mc.thePlayer.motionX *= factor;
                        mc.thePlayer.motionZ *= factor;
                    }
                }
                break;
            case "Legit":
                if (e.getTargetEntity() != null) {
                    attacked = true;
                }
                break;
        }
    }

    @EventTarget
    public void onHitSlowDown(HitSlowDownEvent e) {
        if (mode.is("Reduce")) {
            e.setSlowDown(MathUtils.nextDouble(rFactorMin.get(), rFactorMax.get()));
        }
    }
}
