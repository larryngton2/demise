package wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.motion.MotionA;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.motion.MotionB;

import java.util.Arrays;
import java.util.List;

public class MotionCheck extends Check {
    private final List<Check> checks = Arrays.asList(new MotionA(), new MotionB());

    @Override
    public String getName() {
        return "Motion";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        checks.forEach(check -> check.onUpdate(player));
    }

    @Override
    public void onPacket(PacketEvent e, EntityPlayer player) {
        checks.forEach(check -> check.onPacket(e, player));
    }
}