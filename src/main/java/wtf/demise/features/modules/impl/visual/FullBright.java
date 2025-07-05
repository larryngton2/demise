package wtf.demise.features.modules.impl.visual;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;

@ModuleInfo(name = "FullBright", description = "Brightens up the world.")
public class FullBright extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Gamma", "Potion"}, "Gamma", this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        switch (mode.get()) {
            case "Gamma":
                mc.gameSettings.gammaSetting = 100000;
                break;
            case "Potion":
                mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 5200, 1));
                break;
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer.isPotionActive(Potion.nightVision) && mode.is("Potion")) {
            mc.thePlayer.removePotionEffect(Potion.nightVision.id);
        }
    }
}
