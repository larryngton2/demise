package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.utils.player.MovementUtils;

@ModuleInfo(name = "MoreKB", category = ModuleCategory.Combat)
public class MoreKB extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Legit Fast", "Packet"}, "Legit Test", this);
    private final BoolValue onlyGround = new BoolValue("Only Ground", true, this);
    public int ticks;

    EntityLivingBase target = null;

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTargetEntity() != null && event.getTargetEntity() instanceof EntityLivingBase) {
            target = (EntityLivingBase) event.getTargetEntity();
            ticks = 2;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setTag(mode.get());
        if (target != null && MovementUtils.isMoving()) {
            if ((onlyGround.get() && mc.thePlayer.onGround || !onlyGround.get())) {
                switch (mode.get()) {
                    case "Legit Fast":
                        mc.thePlayer.sprintingTicksLeft = 0;
                        break;
                    case "Packet":
                        sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                        sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                        break;
                }
            }
            target = null;
        }
    }
}