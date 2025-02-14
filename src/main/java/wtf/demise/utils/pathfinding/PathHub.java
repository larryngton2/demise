package wtf.demise.utils.pathfinding;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Vec3;

import java.util.ArrayList;

@Setter
public class PathHub {
    @Getter
    private Vec3 loc;
    private PathHub parentPathHub;
    @Getter
    private ArrayList<Vec3> pathway;
    @Getter
    private double sqDist;
    @Getter
    private double currentCost;
    @Getter
    private double maxCost;

    public PathHub(final Vec3 loc, final PathHub parentPathHub, final ArrayList<Vec3> pathway,
                   final double sqDist, final double currentCost, final double maxCost) {
        this.loc = loc;
        this.parentPathHub = parentPathHub;
        this.pathway = pathway;
        this.sqDist = sqDist;
        this.currentCost = currentCost;
        this.maxCost = maxCost;
    }

    public PathHub getLastHub() {
        return this.parentPathHub;
    }

}
