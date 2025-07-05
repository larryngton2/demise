package wtf.demise.features.modules.impl.player;

import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "FastPlace", description = "Allows you to place blocks faster.")
public class FastPlace extends Module {
    public final SliderValue speed = new SliderValue("Right click delay", 1, 0, 4, this);
    private final BoolValue ignoreTickCycle = new BoolValue("Ignore tick cycle", false, this);
    private final SliderValue frameDelay = new SliderValue("Frame delay", 0, 0, 50, 1, this, ignoreTickCycle::get);
    private final BoolValue swing = new BoolValue("Swing", true, this, ignoreTickCycle::get);

    private final TimerUtils timer = new TimerUtils();

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (ignoreTickCycle.get() && !getModule(Scaffold.class).isEnabled()) {
            MovingObjectPosition ray = mc.objectMouseOver;

            if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                if (mc.gameSettings.keyBindUseItem.isKeyDown() && timer.hasTimeElapsed(frameDelay.get()) && mc.objectMouseOver.sideHit != EnumFacing.UP) {
                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), ray.getBlockPos(), ray.sideHit, ray.hitVec)) {
                        if (swing.get()) {
                            mc.thePlayer.swingItem();
                            mc.getItemRenderer().resetEquippedProgress();
                        } else {
                            mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                        }
                    }

                    timer.reset();
                }
            }
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        setTag(MathUtils.incValue(ignoreTickCycle.get() ? frameDelay.get() : speed.get(), 1) + "");
        if (mc.thePlayer.getHeldItem() == null) return;
        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) mc.rightClickDelayTimer = (int) speed.get();
    }
}
