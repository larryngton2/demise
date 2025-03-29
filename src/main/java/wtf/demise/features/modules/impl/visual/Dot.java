package wtf.demise.features.modules.impl.visual;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.RotationUtils;
import wtf.demise.utils.render.RenderUtils;

@ModuleInfo(name = "Dot", category = ModuleCategory.Visual)
public class Dot extends Module {
    private final BoolValue onlySilent = new BoolValue("Only silent", false, this);
    private final BoolValue notOnMiss = new BoolValue("Not on miss", false, this);

    private Vec3 pos, lastPos;


    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPost() || mc.objectMouseOver == null || mc.objectMouseOver.hitVec == null) {
            return;
        }

        mc.entityRenderer.getMouseOver(1);

        if (notOnMiss.get() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
            pos = lastPos = null;
            return;
        }

        lastPos = pos;
        pos = mc.objectMouseOver.hitVec;
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (lastPos != null && pos != null) {
            if (onlySilent.get() && !RotationUtils.shouldRotate()) {
                return;
            }

            Vec3 interpolatedPosition = MathUtils.interpolate(lastPos, pos, mc.timer.renderPartialTicks);

            RenderUtils.renderBreadCrumb(interpolatedPosition, 0.07f);
        }
    }
}
