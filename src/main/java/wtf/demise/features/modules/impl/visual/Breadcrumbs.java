package wtf.demise.features.modules.impl.visual;

import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Breadcrumbs", category = ModuleCategory.Visual)
public final class Breadcrumbs extends Module {

    List<Vec3> path = new ArrayList<>();

    private final BoolValue timeoutBool = new BoolValue("Timeout", true, this);
    private final SliderValue timeout = new SliderValue("Time", 15, 1, 150, 1, this);

    @Override
    public void onEnable() {
        path.clear();
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPre()) {
            if (mc.thePlayer.lastTickPosX != mc.thePlayer.posX || mc.thePlayer.lastTickPosY != mc.thePlayer.posY || mc.thePlayer.lastTickPosZ != mc.thePlayer.posZ) {
                path.add(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
            }

            if (timeoutBool.get()) {
                while (path.size() > (int) timeout.get()) {
                    path.remove(0);
                }
            }
        }
    }

    @EventTarget
    public void onRender3DEvent(Render3DEvent e) {
        RenderUtils.renderBreadCrumbs(path);
    }
}