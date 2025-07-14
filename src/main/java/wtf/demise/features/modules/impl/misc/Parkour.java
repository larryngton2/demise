package wtf.demise.features.modules.impl.misc;

import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;

@ModuleInfo(name = "Parkour")
public class Parkour extends Module {
    private final BoolValue notWhileShifting = new BoolValue("Not while shifting", true, this);

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if ((notWhileShifting.get() && Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) || !mc.thePlayer.onGround)
            return;

        if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(mc.thePlayer.motionX / 3.0D, -1.0D, mc.thePlayer.motionZ / 3.0D)).isEmpty()) {
            e.setJumping(true);
        }
    }
}