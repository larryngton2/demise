package wtf.demise.features.modules.impl.legit;

import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "MoreKB", description = "Deals increased knockback to targets.", category = ModuleCategory.Legit)
public class MoreKB extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Legit", "LessPacket", "Packet"}, "Legit", this);
    private final BoolValue smart = new BoolValue("Smart", false, this);

    @EventTarget
    public void onTick(TickEvent e) {
        setTag(mode.get());

        EntityLivingBase entity = null;

        if (getModule(KillAura.class).isEnabled() && KillAura.currentTarget != null) {
            entity = KillAura.currentTarget;
        } else if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            entity = (EntityLivingBase) mc.objectMouseOver.entityHit;
        }

        if (entity == null) {
            return;
        }

        final float calcYaw = (float) (MathHelper.atan2(mc.thePlayer.posZ - entity.posZ, mc.thePlayer.posX - entity.posX) * 180.0 / 3.141592653589793 - 90.0);
        final float diffY = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - entity.rotationYawHead));

        if (smart.get() && (diffY > 120.0f || mc.thePlayer.hurtTime != 0)) {
            return;
        }

        switch (mode.get()) {
            case "Packet": {
                if (entity.hurtTime == 10) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.serverSprintState = true;
                    break;
                }
                break;
            }
            case "Legit": {
                if (entity.hurtTime == 10) {
                    mc.thePlayer.reSprint = 2;
                    break;
                }
                break;
            }
            case "LessPacket": {
                if (entity.hurtTime == 10) {
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                    }
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.serverSprintState = true;
                    break;
                }
                break;
            }
        }
    }
}