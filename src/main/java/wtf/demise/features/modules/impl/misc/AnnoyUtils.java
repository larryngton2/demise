package wtf.demise.features.modules.impl.misc;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.movement.Freeze;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "AnnoyUtils", description = "Utility module for annoying others.", category = ModuleCategory.Misc)
public class AnnoyUtils extends Module {
    private final BoolValue voidFreeze = new BoolValue("Void freeze", true, this);
    private final SliderValue yLevel = new SliderValue("Y level", -10, -60, 0, 1, this, voidFreeze::get);

    private final BoolValue antiDeath = new BoolValue("Anti death", true, this);
    private final SliderValue healthToQuit = new SliderValue("Health to quit", 0, 0, 5, 1, this, antiDeath::get);

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        if (voidFreeze.get()) {
            getModule(Freeze.class).setEnabled(mc.thePlayer.posY <= yLevel.get());
        }

        if (antiDeath.get() && mc.thePlayer.getHealth() <= healthToQuit.get()) {
            mc.thePlayer.sendChatMessage("/lobby");
        }
    }
}
