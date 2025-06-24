package wtf.demise.utils.math;

import net.minecraft.util.Vec3;

public class TimedVec3 {
    public Vec3 vec;
    public long time;

    public TimedVec3(Vec3 vec, long time) {
        this.vec = vec;
        this.time = time;
    }
}