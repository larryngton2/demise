package wtf.demise.features.modules.impl.movement;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.network.play.client.C03PacketPlayer;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.BlockAABBEvent;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.player.PostStepEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "Step", description = "Allows you to step up blocks.", category = ModuleCategory.Movement)
public class Step extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla", "NCP", "BMC"}, "Vanilla", this);
    private final SliderValue height = new SliderValue("Height", 1, 0.5f, 2, 0.01f, this, () -> mode.is("Vanilla"));
    private final SliderValue timer = new SliderValue("Timer", 1, 0.01f, 1, 0.01f, this, () -> mode.is("NCP"));
    private final SliderValue delay = new SliderValue("Delay", 1000, 0, 2500, 1, this, () -> mode.is("NCP"));

    private final DoubleList MOTION = DoubleList.of(.42, .75, 1);
    private boolean stepped = false;
    private boolean bmcIsStepping = false;
    private int bmcStepTicks;

    @Override
    public void onDisable() {
        mc.thePlayer.stepHeight = 0.6f;
        mc.timer.timerSpeed = 1;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        switch (mode.get()) {
            case "BMC":
                if (mc.thePlayer.onGround) {
                    bmcStepTicks = 0;
                    bmcIsStepping = false;
                    if (mc.thePlayer.isCollidedHorizontally) {
                        bmcIsStepping = true;
                        bmcStepTicks = 1;
                    }
                }

                if (bmcIsStepping) {
                    switch (bmcStepTicks) {
                        case 1:
                            mc.thePlayer.motionY = 0.42;
                            ++bmcStepTicks;
                            break;
                        case 2:
                            mc.thePlayer.motionY = 0.33;
                            ++bmcStepTicks;
                            break;
                        case 3:
                            mc.thePlayer.motionY = 0.25F;
                            ++bmcStepTicks;
                            break;
                        case 4:
                            mc.thePlayer.motionY = 0.0F;
                            bmcStepTicks = 0;
                            bmcIsStepping = false;
                            MoveUtil.strafe(MoveUtil.isMoving() ? 0.247 : (double) 0.0F);
                    }
                }
                break;
            case "Vanilla":
                mc.thePlayer.stepHeight = height.get();
                break;
        }
    }

    @EventTarget
    public void onPostStep(PostStepEvent event) {
        if (mode.is("NCP")) {
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
    public void onTick(TickEvent e) {
        if (mode.is("NCP")) {
            if (stepped) {
                mc.timer.timerSpeed = 1;
                stepped = false;
            }
            long lastStep = -1;
            if (System.currentTimeMillis() - lastStep > delay.get()) {
                mc.thePlayer.stepHeight = 1;
            }
        }
    }
}
