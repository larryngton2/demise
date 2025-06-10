package wtf.demise.utils.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.InstanceAccess;

@UtilityClass
public class SoundUtil implements InstanceAccess {
    private int ticksExisted;
    private NotificationType prevNotificationType;

    public void notifSound(NotificationType type) {
        boolean checkTicksExisted = mc.thePlayer != null;

        if (!checkTicksExisted || (prevNotificationType != type || ticksExisted != mc.thePlayer.ticksExisted)) {
            String sound = "demise." + type.name().toLowerCase();

            if (mc.theWorld != null) {
                playSound("demise." + type.name().toLowerCase());
            } else {
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(sound), 1));
            }
        }

        if (checkTicksExisted) {
            ticksExisted = mc.thePlayer.ticksExisted;
        }
        prevNotificationType = type;
    }

    public void playSound(String sound) {
        playSound(sound, 1, 1);
    }

    public void playSound(String sound, float volume, float pitch) {
        mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, sound, volume, pitch, false);
    }
}
