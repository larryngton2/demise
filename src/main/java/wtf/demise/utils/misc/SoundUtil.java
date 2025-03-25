package wtf.demise.utils.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import wtf.demise.utils.InstanceAccess;

@UtilityClass
public class SoundUtil implements InstanceAccess {
    private int ticksExisted;

    public void toggleSound(final boolean enable) {
        if (mc.thePlayer != null && mc.thePlayer.ticksExisted != ticksExisted) {
            if (enable) {
                playSound("demise.enable");
            } else {
                playSound("demise.disable");
            }

            ticksExisted = mc.thePlayer.ticksExisted;
        }
    }

    public void playSound(final String sound) {
        playSound(sound, 1);
    }

    public void playSound(final String sound, final float pitch) {
        mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(sound), pitch));
    }
}
