package wtf.demise.features.modules.impl.legit;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "NoHitDelay", description = "Removes Minecraft's hit delay.", category = ModuleCategory.Legit)
public class NoHitDelay extends wtf.demise.features.modules.Module {

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.theWorld != null && mc.thePlayer != null) {
            if (!mc.inGameHasFocus) return;

            mc.leftClickCounter = 0;
        }
    }
}
