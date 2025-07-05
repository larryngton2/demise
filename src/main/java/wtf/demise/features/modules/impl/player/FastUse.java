package wtf.demise.features.modules.impl.player;

import net.minecraft.network.play.client.C03PacketPlayer;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.packet.PacketUtils;

@ModuleInfo(name = "FastUse", description = "Allows you to consume items faster.")
public class FastUse extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "NCP", "AAC"}, "Instant", this);
    private final SliderValue packets = new SliderValue("Packets", 32, 1, 100, 1, this, () -> mode.is("Vanilla"));
    private final SliderValue timer = new SliderValue("Timer", 1, 0.1f, 5, 0.1f, this);

    private boolean timered = false;

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1f;
        timered = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        if (!mc.thePlayer.isUsingItem()) {
            return;
        }

        if (timered) {
            mc.timer.timerSpeed = 1f;
            timered = false;
        }

        if (timer.get() != 1) {
            mc.timer.timerSpeed = timer.get();
        }

        switch (mode.get()) {
            case "Vanilla":
                for (int i = 0; i < packets.get(); i++) {
                    PacketUtils.sendPacket(new C03PacketPlayer(mc.thePlayer.onGround));
                }

                mc.playerController.onStoppedUsingItem(mc.thePlayer);
                break;
            case "NCP":
                if (mc.thePlayer.getItemInUseDuration() > 14) {
                    for (int i = 0; i < 20; i++) {
                        PacketUtils.sendPacket(new C03PacketPlayer(mc.thePlayer.onGround));
                    }

                    mc.playerController.onStoppedUsingItem(mc.thePlayer);
                }
                break;
            case "AAC":
                if (timer.get() == 1) {
                    mc.timer.timerSpeed = 1.22f;
                }
                timered = true;
                break;
        }
    }
}