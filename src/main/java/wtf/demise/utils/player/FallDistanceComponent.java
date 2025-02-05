package wtf.demise.utils.player;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;

import static wtf.demise.utils.InstanceAccess.mc;

public final class FallDistanceComponent {
    public static float distance;
    private float lastDistance;

    @EventTarget
    private void onMotion(final MotionEvent event) {
        if (event.isPre()) {
            final float fallDistance = mc.thePlayer.fallDistance;
            if (fallDistance == 0.0f) {
                FallDistanceComponent.distance = 0.0f;
            }
            FallDistanceComponent.distance += fallDistance - this.lastDistance;
            this.lastDistance = fallDistance;
        }
    }
}
