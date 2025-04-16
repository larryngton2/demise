package wtf.demise.features.modules.impl.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import org.apache.commons.lang3.Range;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

@ModuleInfo(name = "FakeLag", category = ModuleCategory.Combat)
public class FakeLag extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Pulse", "Spoof"}, "Pulse", this);
    private final BoolValue alwaysSpoof = new BoolValue("Always spoof", false, this);
    private final SliderValue attackRange = new SliderValue("Attack range", 4, 0, 15, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 6, 1, 15, 0.1f, this);
    private final BoolValue smartRange = new BoolValue("Smart range", true, this);
    private final SliderValue recoilTime = new SliderValue("Recoil time (ms)", 1, 0, 1000, this);
    private final SliderValue delayMin = new SliderValue("Delay (min ms)", 100, 0, 1000, this);
    private final SliderValue delayMax = new SliderValue("Delay (max ms)", 250, 1, 1000, this);
    private final BoolValue realPos = new BoolValue("Display real pos", true, this);
    private final BoolValue onlyOnGround = new BoolValue("Only onGround", false, this);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);

    public static boolean blinking = false, picked = false;
    private final TimerUtils delay = new TimerUtils();
    private final TimerUtils ever = new TimerUtils();
    private static double x, y, z;
    private double lerpX, lerpY, lerpZ;
    public EntityPlayer target;
    private int ms;
    private boolean attacked;

    @Override
    public void onEnable() {
        blinking = false;
        picked = false;
        x = y = z = lerpX = lerpY = lerpZ = 0;
        target = null;

        delay.reset();
        ever.reset();
    }

    @Override
    public void onDisable() {
        switch (mode.get()) {
            case "Pulse":
                BlinkComponent.dispatch(true);
                break;
            case "Spoof":
                PingSpoofComponent.disable();
                PingSpoofComponent.dispatch();
                break;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(ms + " ms");

        target = PlayerUtils.getTarget(alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get() + 1, teamCheck.get());

        if (ms == 0) ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());

        if (onlyOnGround.get() && !mc.thePlayer.onGround) {
            if (blinking) {
                switch (mode.get()) {
                    case "Pulse":
                        BlinkComponent.dispatch(true);
                        break;
                    case "Spoof":
                        PingSpoofComponent.disable();
                        PingSpoofComponent.dispatch();
                        break;
                }
                blinking = false;
            }

            if (picked) picked = false;
            return;
        }

        switch (mode.get()) {
            case "Pulse":
                if (shouldLag() && mc.thePlayer.hurtTime == 0) {
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
                    ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());
                    if (blinking) {
                        BlinkComponent.dispatch(true);
                    }
                    picked = false;
                    if (delay.hasTimeElapsed(ms) && blinking) {
                        blinking = false;
                        delay.reset();
                    }
                }
                break;
            case "Spoof":
                if (shouldLag() && mc.thePlayer.hurtTime == 0) {
                    if (ever.hasTimeElapsed(recoilTime.get())) {
                        ms = MathUtils.randomizeInt(delayMin.get(), delayMax.get());
                        PingSpoofComponent.spoof(ms, true, false, false, false, true, true);

                        if (delay.hasTimeElapsed(ms + 250)) {
                            x = PingSpoofComponent.getRealPos().xCoord;
                            y = PingSpoofComponent.getRealPos().yCoord;
                            z = PingSpoofComponent.getRealPos().zCoord;
                        }

                        blinking = true;
                    }
                } else {
                    PingSpoofComponent.disable();
                    PingSpoofComponent.dispatch();
                    x = mc.thePlayer.posX;
                    y = mc.thePlayer.posY;
                    z = mc.thePlayer.posZ;
                    blinking = false;
                    ever.reset();
                    delay.reset();
                    picked = false;
                }
                break;
        }

        attacked = false;
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (e.getTargetEntity() != null) {
            attacked = true;
        }
    }

    private boolean shouldLag() {
        return target != null &&
                smartRange.get() ? !attacked && PlayerUtils.getDistanceToEntityBox(target) <= (alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get()) : Range.between(attackRange.get(), alwaysSpoof.get() ? Float.MAX_VALUE : searchRange.get()).contains((float) PlayerUtils.getDistanceToEntityBox(target)) &&
                mc.thePlayer.canEntityBeSeen(target);
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (realPos.get()) {
            lerpX = MathUtils.interpolate(lerpX, x);
            lerpY = MathUtils.interpolate(lerpY, y);
            lerpZ = MathUtils.interpolate(lerpZ, z);

            double x = lerpX - mc.getRenderManager().viewerPosX;
            double y = lerpY - mc.getRenderManager().viewerPosY;
            double z = lerpZ - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);

            if (blinking && mc.gameSettings.thirdPersonView != 0) {
                RenderUtils.drawAxisAlignedBB(axis, true, new Color(255, 255, 255, 150).getRGB());
            }
        }
    }
}