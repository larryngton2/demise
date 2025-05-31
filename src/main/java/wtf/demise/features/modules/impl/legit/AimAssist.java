package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.render.RenderUtils;

@ModuleInfo(name = "AimAssist", description = "Assists in aiming.", category = ModuleCategory.Legit)
public class AimAssist extends Module {
    private final SliderValue searchRange = new SliderValue("Search range", 4.0f, 1, 8, 0.1f, this);
    private final RotationHandler rotationHandler = new RotationHandler(this);
    private final BoolValue onlyOnClick = new BoolValue("Only on click", true, this);
    private final SliderValue resetTime = new SliderValue("Reset time", 500, 0, 1000, 1, this, onlyOnClick::get);
    private final BoolValue teamCheck = new BoolValue("Team check", false, this);
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);

    private EntityLivingBase target;
    private boolean angleCalled;
    private final TimerUtils resetTimer = new TimerUtils();

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        rotationHandler.updateRotSpeed(e);

        if (onlyOnClick.get() && Mouse.isButtonDown(0) && angleCalled) {
            resetTimer.reset();
        }

        if (!onlyOnClick.get() || !resetTimer.hasTimeElapsed(resetTime.get())) {
            target = PlayerUtils.getTarget(searchRange.get(), teamCheck.get());
        } else {
            target = null;
        }

        if (target == null) {
            return;
        }

        if (angleCalled && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
            rotationHandler.setRotation(rotationHandler.getSimpleRotationsToEntity(target));
        }

        angleCalled = false;
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        angleCalled = true;
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (target != null) {
            RotationUtils.enabled = true;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (target != null && targetESP.get()) {
            RenderUtils.drawTargetCircle(target);
        }
    }
}
