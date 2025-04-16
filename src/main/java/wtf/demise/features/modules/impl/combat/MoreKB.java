package wtf.demise.features.modules.impl.combat;

import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.killaura.KillAura;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "MoreKB", category = ModuleCategory.Combat)
public class MoreKB extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Legit", "LessPacket", "Packet", "DoublePacket"}, "Legit", this);
    private final BoolValue intelligent = new BoolValue("Intelligent", false, this);

    @EventTarget
    public void onTick(TickEvent e) {
        EntityLivingBase entity = null;
        if (MoreKB.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && MoreKB.mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            entity = (EntityLivingBase) MoreKB.mc.objectMouseOver.entityHit;
        }
        if (getModule(KillAura.class).isEnabled() && KillAura.currentTarget != null) {
            entity = KillAura.currentTarget;
        }
        if (entity == null) {
            return;
        }
        final double x = MoreKB.mc.thePlayer.posX - entity.posX;
        final double z = MoreKB.mc.thePlayer.posZ - entity.posZ;
        final float calcYaw = (float) (MathHelper.atan2(z, x) * 180.0 / 3.141592653589793 - 90.0);
        final float diffY = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - entity.rotationYawHead));
        if (this.intelligent.get() && diffY > 120.0f) {
            return;
        }
        final String selected = this.mode.get();
        switch (selected) {
            case "Packet": {
                if (entity.hurtTime == 10) {
                    MoreKB.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(MoreKB.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    MoreKB.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(MoreKB.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    MoreKB.mc.thePlayer.serverSprintState = true;
                    break;
                }
                break;
            }
            case "DoublePacket": {
                if (entity.hurtTime == 10) {
                    MoreKB.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(MoreKB.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    MoreKB.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(MoreKB.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    MoreKB.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(MoreKB.mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    MoreKB.mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(MoreKB.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    MoreKB.mc.thePlayer.serverSprintState = true;
                    break;
                }
                break;
            }
            case "Legit": {
                if (entity.hurtTime == 10) {
                    MoreKB.mc.thePlayer.reSprint = 2;
                    System.out.println("Resprint   " + entity.hurtTime);
                    break;
                }
                break;
            }
            case "LessPacket": {
                if (entity.hurtTime == 10) {
                    if (MoreKB.mc.thePlayer.isSprinting()) {
                        MoreKB.mc.thePlayer.setSprinting(false);
                    }
                    MoreKB.mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(MoreKB.mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    MoreKB.mc.thePlayer.serverSprintState = true;
                    break;
                }
                break;
            }
        }
    }
}