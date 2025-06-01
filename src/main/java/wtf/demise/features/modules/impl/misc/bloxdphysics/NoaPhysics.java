package wtf.demise.features.modules.impl.misc.bloxdphysics;

import com.sun.javafx.geom.Vec3d;

public class NoaPhysics {
    public Vec3d impulseVector;
    public Vec3d forceVector;
    public Vec3d velocityVector;
    public Vec3d gravityVector;
    public double gravityMul = 2d;
    public final double mass = 1;
    private final double delta = 1 / 30d;

    public NoaPhysics() {
        this.impulseVector = new Vec3d(0, 0, 0);
        this.forceVector = new Vec3d(0, 0, 0);
        this.velocityVector = new Vec3d(0, 0, 0);
        this.gravityVector = new Vec3d(0, -10, 0);
    }

    public Vec3d getMotionForTick() {
        // forces
        double massDiv = 1 / mass;
        this.forceVector.mul(massDiv);
        // gravity
        this.forceVector.add(this.gravityVector);
        this.forceVector.mul(this.gravityMul);

        // impulses
        this.impulseVector.mul(massDiv);
        this.forceVector.mul(this.delta);
        this.impulseVector.add(this.forceVector);
        // velocity
        this.velocityVector.add(this.impulseVector);

        this.forceVector.set(0, 0, 0);
        this.impulseVector.set(0, 0, 0);

        return this.velocityVector;
    }
}