package wtf.demise.features.modules.impl.visual;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "FullBright",category = ModuleCategory.Visual)
public class FullBright extends Module {

    @EventTarget
    public void onUpdate(UpdateEvent event){
        mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 5200, 1));
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer.isPotionActive(Potion.nightVision)) {
            mc.thePlayer.removePotionEffect(Potion.nightVision.id);
        }
    }
}
