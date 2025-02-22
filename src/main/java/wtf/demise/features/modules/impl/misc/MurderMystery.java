package wtf.demise.features.modules.impl.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.utils.misc.DebugUtils;

@ModuleInfo(name = "Murder Mystery", category = ModuleCategory.Misc)
public final class MurderMystery extends Module {
    private EntityPlayer murderer;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.ticksExisted % 2 == 0 || this.murderer != null) {
            return;
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.getHeldItem() != null) {
                if (player.getHeldItem().getItem() instanceof ItemSword) {
                    DebugUtils.sendMessage(player.getName() + " is the murderer");
                    this.murderer = player;
                }
            }
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        murderer = null;
    }
}
