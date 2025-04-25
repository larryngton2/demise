package wtf.demise.features.modules.impl.movement;

import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.status.server.S01PacketPong;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "Fly", category = ModuleCategory.Movement)
public class Fly extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "Intave ladder"}, "Vanilla", this);
    private final SliderValue speed = new SliderValue("Speed", 2, 1, 5, 0.1f, this, () -> mode.is("Vanilla"));

    private double startY;
    private boolean cancel;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        switch (mode.get()) {
            case "Vanilla": {
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806D * speed.get()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
            }
            case "Intave ladder": {
                if (!cancel && mc.thePlayer.isOnLadder()) {
                    cancel = true;
                }

                if (startY >= mc.thePlayer.posY) {
                    mc.thePlayer.motionY = 0.42;
                }

                ChatUtils.sendMessageClient(String.valueOf(cancel));
                break;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mode.is("Intave ladder") && e.getPacket() instanceof C0FPacketConfirmTransaction && cancel) {
            ChatUtils.sendMessageClient("ping pong");
            e.setCancelled(true);
        }
    }

    @Override
    public void onEnable() {
        if (mode.is("Intave ladder")) {
            startY = mc.thePlayer.posY;
        }
    }

    @Override
    public void onDisable() {
        switch (mode.get()) {
            case "Vanilla":
                if (mc.thePlayer == null)
                    return;

                if (mc.thePlayer.capabilities.isFlying) {
                    mc.thePlayer.capabilities.isFlying = false;
                }

                mc.thePlayer.capabilities.setFlySpeed(0.05F);
                break;
            case "Intave ladder":
                cancel = false;
                break;
        }
    }
}