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
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

@ModuleInfo(name = "FakeLag", category = ModuleCategory.Combat)
public class FakeLag extends Module {
    private final SliderValue minRange = new SliderValue("Min range", 4, 1, 15, this);
    private final SliderValue maxRange = new SliderValue("Max range", 6, 4, 15, this);
    private final SliderValue recoilTime = new SliderValue("Recoil time (ms)", 1, 0, 1000, this);
    private final SliderValue delayMin = new SliderValue("Delay (min ms)", 100, 0, 1000, this);
    private final SliderValue delayMax = new SliderValue("Delay (max ms)", 250, 1, 1000, this);
    private final BoolValue velocity = new BoolValue("Pause on velocity", true, this);
    private final BoolValue teleport = new BoolValue("Pause on teleport", true, this);
    private final BoolValue realPos = new BoolValue("Display real pos", true, this);
    private boolean blinking = false, picked = false;
    private final TimerUtils delay = new TimerUtils();
    private final TimerUtils ever = new TimerUtils();
    private double x, y, z;
    public EntityPlayer target;

    @Override
    public void onEnable() {
        blinking = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        this.setTag(delayMax.get() + " - " + delayMax.get());

        target = PlayerUtils.getTarget(maxRange.get() + 1);

        double ms = MathUtils.randomizeDouble(delayMin.get(), delayMax.get());

        if (target != null && MathUtils.inBetween(minRange.get(), maxRange.get(), PlayerUtils.getDistanceToEntityBox(target)) && mc.thePlayer.canEntityBeSeen(target)) {
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
                PingSpoofComponent.spoof(999999999, true, teleport.get(), velocity.get(), true, true, true);
                ever.reset();
            } else {
                PingSpoofComponent.dispatch();
                picked = false;
            }
        } else {
            PingSpoofComponent.dispatch();
            picked = false;
            if (delay.hasTimeElapsed(ms) && blinking) {
                blinking = false;
                delay.reset();
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
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