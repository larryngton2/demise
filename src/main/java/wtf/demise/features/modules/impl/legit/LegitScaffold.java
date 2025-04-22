package wtf.demise.features.modules.impl.legit;

import net.minecraft.block.BlockAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;

@ModuleInfo(name = "LegitScaffold", category = ModuleCategory.Legit)
public class LegitScaffold extends Module {
    private final SliderValue delay = new SliderValue("Delay", 50, 0, 200, 10, this);
    private final BoolValue blockCheck = new BoolValue("Blocks Only", true, this);
    private final BoolValue directionCheck = new BoolValue("Directional Check", true, this);
    private final TimerUtils timer = new TimerUtils();
    private boolean wasOverBlock = false;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(String.valueOf(delay.get()));
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPre()) {
            if (!blockCheck.get() || (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) && !directionCheck.get() || mc.thePlayer.moveForward < 0) {
                if (mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)).getBlock() instanceof BlockAir && mc.thePlayer.onGround) {
                    mc.gameSettings.keyBindSneak.setPressed(true);
                    wasOverBlock = true;
                } else if (mc.thePlayer.onGround) {
                    if (wasOverBlock) timer.reset();

                    if (timer.hasTimeElapsed((long) (delay.get() * (Math.random() * 0.1 + 0.95)))) {
                        mc.gameSettings.keyBindSneak.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
                    }

                    wasOverBlock = false;
                }
            } else {
                mc.gameSettings.keyBindSneak.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
            }
        }
    }

    @Override
    public void onDisable() {
        mc.gameSettings.keyBindSneak.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
    }
}