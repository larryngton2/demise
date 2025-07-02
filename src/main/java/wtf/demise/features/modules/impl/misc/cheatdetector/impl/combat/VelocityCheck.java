package wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.utils.player.MoveUtil;

public class VelocityCheck extends Check {

    @Override
    public String getName() {
        return "Velocity";
    }

    @Override
    public void onPacket(PacketEvent e, EntityPlayer player) {
        if (MoveUtil.getSpeed(player) == 0.0 && player.hurtTime < 6 && player.hurtTime > 2 && !mc.theWorld.checkBlockCollision(player.getEntityBoundingBox().expand(0.05, 0.0, 0.05))) {
            flag(player, "Invalid velocity");
        }
    }
}