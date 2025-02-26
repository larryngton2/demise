package wtf.demise.features.modules.impl.misc.anticheat.impl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.anticheat.Check;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;

public class LegitScaffoldCheck extends Check {
    private final TimerUtils timer = new TimerUtils();
    private int sneakFlag;

    @Override
    public String getName() {
        return "Legit Scaffold";
    }

    @Override
    public void onPacketReceive(PacketEvent event, EntityPlayer player) {
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        if (player.isSneaking()) {
            timer.reset();
            sneakFlag += 1;
        }

        if (timer.hasTimeElapsed(140)) {
            sneakFlag = 0;
        }
        if (player.rotationPitch > 75 && player.rotationPitch < 90 && player.isSwingInProgress) {
            if (player.getHeldItem() != null && player.getHeldItem().getItem() instanceof ItemBlock) {
                if (MoveUtil.getSpeed(player) >= 0.10 && player.onGround && sneakFlag > 5) {
                    flag(player, "Sneak too fast");
                }
                if (MoveUtil.getSpeed(player) >= 0.21 && !player.onGround && sneakFlag > 5) {
                    flag(player, "Sneak too fast");
                }
            }
        }
    }
}