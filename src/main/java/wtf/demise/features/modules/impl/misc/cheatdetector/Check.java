package wtf.demise.features.modules.impl.misc.cheatdetector;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import wtf.demise.Demise;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.features.modules.impl.misc.CheatDetector;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.TimerUtils;

import java.util.Set;
import java.util.UUID;

public abstract class Check implements InstanceAccess {
    public TimerUtils flagTimer = new TimerUtils();

    public abstract String getName();

    public void onUpdate(EntityPlayer player) {
    }

    public void onPacket(PacketEvent event, EntityPlayer player) {
    }

    public void cleanup(Set<UUID> onlineUUIDs) {
    }

    public void flag(EntityPlayer player, String verbose) {
        if (flagTimer.hasTimeElapsed(Demise.INSTANCE.getModuleManager().getModule(CheatDetector.class).alertCoolDown.get())) {
            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, player.getName() + EnumChatFormatting.WHITE + " has failed " + EnumChatFormatting.GRAY + getName() + EnumChatFormatting.WHITE, verbose, 5);
            Demise.INSTANCE.getModuleManager().getModule(CheatDetector.class).mark(player);
            flagTimer.reset();
        }
    }
}