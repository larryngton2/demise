package wtf.demise.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wtf.demise.events.impl.CancellableEvent;
import wtf.demise.utils.InstanceAccess;

@Setter
@Getter
@AllArgsConstructor
public class StrafeEvent extends CancellableEvent implements InstanceAccess {
    private float strafe;
    private float forward;
    private float friction;
    private float yaw;

    public void setSpeed(final double speed, final double motionMultiplier) {
        setFriction((float) (getForward() != 0 && getStrafe() != 0 ? speed * 0.98F : speed));
        mc.thePlayer.motionX *= motionMultiplier;
        mc.thePlayer.motionZ *= motionMultiplier;
    }
}