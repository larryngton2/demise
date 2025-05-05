package wtf.demise.features.modules.impl.movement;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.network.play.client.C03PacketPlayer;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.player.PostStepEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "Step", description = "Allows you to step up blocks.", category = ModuleCategory.Movement)
public class Step extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"NCP"}, "NCP", this);
    public final SliderValue timer = new SliderValue("Timer", 1, 0.05f, 1, 0.05f, this);
    public final SliderValue delay = new SliderValue("Delay", 1000, 0, 2500, 1, this);
    public final DoubleList MOTION = DoubleList.of(.42, .75, 1);
    private final long lastStep = -1;
    private boolean stepped = false;

    @Override
    public void onDisable() {
        mc.thePlayer.stepHeight = 0.6f;
        mc.timer.timerSpeed = 1;
    }

    @EventTarget
    public void onPostStep(PostStepEvent event) {
        if (mode.get().equals("NCP")) {
            if (event.getHeight() == 1 && mc.thePlayer.onGround && !PlayerUtils.inLiquid()) {
                Block block = PlayerUtils.getBlock(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                if (block instanceof BlockStairs || block instanceof BlockSlab) return;

                mc.timer.timerSpeed = timer.get();
                stepped = true;
                for (double motion : MOTION) {
                    MoveUtil.strafe();
                    sendPacketNoEvent(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + motion, mc.thePlayer.posZ, false));
                }
                mc.thePlayer.stepHeight = 0.6f;
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (stepped) {
            mc.timer.timerSpeed = 1;
            stepped = false;
        }
        if (System.currentTimeMillis() - lastStep > delay.get())
            mc.thePlayer.stepHeight = 1;
    }

}
