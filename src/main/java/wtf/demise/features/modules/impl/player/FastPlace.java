package wtf.demise.features.modules.impl.player;

import net.minecraft.item.ItemBlock;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "FastPlace", category = ModuleCategory.Player)
public class FastPlace extends Module {
    public final SliderValue speed = new SliderValue("Speed", 1, 0, 4, this);

    @EventTarget
    public void onMotion(MotionEvent event) {
        setTag(MathUtils.incValue(speed.get(), 1) + "");
        if (!PlayerUtils.nullCheck())
            return;
        if (mc.thePlayer.getHeldItem() == null)
            return;
        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock)
            mc.rightClickDelayTimer = (int) speed.get();
    }
}
