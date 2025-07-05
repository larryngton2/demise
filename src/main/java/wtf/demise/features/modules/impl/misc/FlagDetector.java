package wtf.demise.features.modules.impl.misc;

import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.gui.notification.NotificationType;

@ModuleInfo(name = "FlagDetector", description = "Alerts you about anticheat flags.")
public class FlagDetector extends Module {
    private int totalFlags;

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.INCOMING && mc.thePlayer.ticksExisted > 20) {
            if (e.getPacket() instanceof S08PacketPlayerPosLook) {
                totalFlags++;
                alert("Lagback");
            }
        }

        if (e.getPacket() instanceof S01PacketJoinGame || e.getPacket() instanceof S00PacketDisconnect) {
            totalFlags = 0;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!mc.thePlayer.isDead) {
            if (mc.thePlayer.getHealth() <= 0) {
                totalFlags++;
                alert("Invalid health");
            }

            if (mc.thePlayer.getFoodStats().getFoodLevel() <= 0) {
                totalFlags++;
                alert("Invalid hunger");
            }
        }
    }

    private void alert(String flagType) {
        Demise.INSTANCE.getNotificationManager().post(NotificationType.ERROR, "FlagDetector", "Detected " + flagType + " (" + totalFlags + "x)");
    }
}