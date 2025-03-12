package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

@ModuleInfo(name = "FakeLag", category = ModuleCategory.Combat)
public class FakeLag extends Module {
    private final SliderValue minRange = new SliderValue("Min range", 4, 0, 15, 0.1f, this);
    private final SliderValue maxRange = new SliderValue("Max range", 6, 1, 15, 0.1f, this);
    private final SliderValue recoilTime = new SliderValue("Recoil time (ms)", 1, 0, 1000, this);
    private final SliderValue delayMin = new SliderValue("Delay (min ms)", 100, 0, 1000, this);
    private final SliderValue delayMax = new SliderValue("Delay (max ms)", 250, 1, 1000, this);
    private final BoolValue realPos = new BoolValue("Display real pos", true, this);
    private final BoolValue onlyOnGround = new BoolValue("Only onGround", false, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);

    private boolean blinking = false, picked = false;
    private final TimerUtils delay = new TimerUtils();
    private final TimerUtils ever = new TimerUtils();
    private double x, y, z;
    public EntityPlayer target;
    private int ms;

    @Override
    public void onEnable() {
        blinking = false;
    }

    @Override
    public void onDisable() {
        if (blinking) {
            BlinkComponent.dispatch(true);
        }

        picked = false;
        target = null;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(ms + " ms");

        target = PlayerUtils.getTarget(maxRange.get() + 1, teamCheck.get());

        if (ms == 0) ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());

        if (onlyOnGround.get() && !mc.thePlayer.onGround) {
            if (blinking) {
                BlinkComponent.dispatch(true);
                blinking = false;
            }

            if (picked) picked = false;
            return;
        }

        if (target != null && MathUtils.inBetween(minRange.get(), maxRange.get(), PlayerUtils.getDistanceToEntityBox(target)) && mc.thePlayer.canEntityBeSeen(target)) {
            ms = (int) MathUtils.randomizeDouble(delayMin.get(), delayMax.get());

            if (ever.hasTimeElapsed(recoilTime.get())) {
                blinking = true;
            }

            if (delay.hasTimeElapsed(ms) && blinking) {
                blinking = false;
                delay.reset();
            }

            if (blinking) {
                if (!picked) {
                    x = mc.thePlayer.posX;
                    y = mc.thePlayer.posY;
                    z = mc.thePlayer.posZ;
                    picked = true;
                }
                BlinkComponent.blinking = true;
                ever.reset();
            } else {
                BlinkComponent.dispatch(true);
                picked = false;
            }
        } else {
            BlinkComponent.dispatch(true);
            picked = false;
            if (delay.hasTimeElapsed(ms) && blinking) {
                blinking = false;
                delay.reset();
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (realPos.get() && blinking && mc.gameSettings.thirdPersonView != 0) {
            double x = this.x - mc.getRenderManager().viewerPosX;
            double y = this.y - mc.getRenderManager().viewerPosY;
            double z = this.z - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, true, new Color(255, 255, 255, 150).getRGB());
        }
    }
}
