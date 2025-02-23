package wtf.demise.features.modules.impl.movement;

import net.minecraft.network.play.client.C0BPacketEntityAction;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "Sneak", category = ModuleCategory.Movement)
public class Sneak extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Legit", "Packet"}, "Legit", this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());

        switch (mode.get()) {
            case "Legit":
                mc.gameSettings.keyBindSneak.setPressed(true);
                break;
            case "Packet":
                if (!mc.thePlayer.serverSneakState) {
                    sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                    mc.thePlayer.serverSneakState = true;
                }
                break;
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (mode.is("NCP")) {
            sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, e.isPre() ? C0BPacketEntityAction.Action.STOP_SNEAKING : C0BPacketEntityAction.Action.START_SNEAKING));
        }
    }

    @Override
    public void onEnable() {
        switch (mode.get()) {
            case "Legit":
                mc.gameSettings.keyBindSneak.setPressed(true);
                break;
            case "Packet":
                if (mc.thePlayer.serverSneakState) {
                    sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                    mc.thePlayer.serverSneakState = false;
                }
                break;
            case "NCP":
                sendPacketNoEvent(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                break;
        }
    }
}