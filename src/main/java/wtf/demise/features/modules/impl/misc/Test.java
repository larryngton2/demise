package wtf.demise.features.modules.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationManager;

@ModuleInfo(name = "Test", category = ModuleCategory.Misc)
public class Test extends Module {
    private final BoolValue silent = new BoolValue("Silent", true, this);

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        Entity target = PlayerUtils.getTarget(8, false);

        if (target == null) return;

        float[] rotation = getSimpleRotationsToEntity(target);

        //RotationManager.setRotation(rotation, silent.get());
    }

    public float[] getSimpleRotationsToEntity(Entity entity) {
        float yaw;
        float pitch;
        Vec3 currentVec;

        Vec3 playerPos = mc.thePlayer.getPositionEyes(1);

        AxisAlignedBB bb = entity.getHitbox();

        Vec3 boxCenter = bb.getCenter();
        Vec3 entityPos = new Vec3(boxCenter.xCoord, bb.minY, boxCenter.zCoord);

        currentVec = entityPos.add(0.0, entity.getEyeHeight(), 0.0);

        double deltaX = currentVec.xCoord - playerPos.xCoord;
        double deltaY = currentVec.yCoord - playerPos.yCoord;
        double deltaZ = currentVec.zCoord - playerPos.zCoord;

        yaw = (float) -(Math.atan2(deltaX, deltaZ) * (180.0 / Math.PI));
        pitch = (float) (-Math.toDegrees(Math.atan2(deltaY, Math.hypot(deltaX, deltaZ))));

        pitch = MathHelper.clamp_float(pitch, -90, 90);

        return new float[]{yaw, pitch};
    }
}