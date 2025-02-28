package wtf.demise.features.modules.impl.player;

import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemFireball;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.movement.LongJump;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.RotationUtils;

@ModuleInfo(name = "AntiFireball", category = ModuleCategory.Player)
public class AntiFireball extends Module {
    private final SliderValue aps = new SliderValue("Aps", 9, 1, 20, this);
    public final SliderValue range = new SliderValue("Range", 6.0F, 2.0F, 6F, .1f, this);
    private final BoolValue customRotationSetting = new BoolValue("Custom Rotation Setting", false, this);
    private final SliderValue minYawRotSpeed = new SliderValue("Min Yaw Rotation Speed", 180, 0, 180, 1, this, () -> customRotationSetting.get());
    private final SliderValue minPitchRotSpeed = new SliderValue("Min Pitch Rotation Speed", 180, 0, 180, 1, this, () -> customRotationSetting.get());
    private final SliderValue maxYawRotSpeed = new SliderValue("Max Yaw Rotation Speed", 180, 0, 180, 1, this, () -> customRotationSetting.get());
    private final SliderValue maxPitchRotSpeed = new SliderValue("Max Pitch Rotation Speed", 180, 0, 180, 1, this, () -> customRotationSetting.get());
    private final BoolValue moveFix = new BoolValue("Move Fix", false, this);
    private final TimerUtils attackTimer = new TimerUtils();

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (isEnabled(LongJump.class) || isEnabled(Scaffold.class) || mc.thePlayer.getHeldItem().getItem() instanceof ItemFireball)
            return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityFireball && entity.getDistanceToEntity(mc.thePlayer) < range.get()) {
                if (attackTimer.hasTimeElapsed((long) (1000L / (aps.get() + 4)))) {

                    float[] finalRotation = RotationUtils.getAngles(entity);

                    if (customRotationSetting.get()) {
                        RotationUtils.setRotation(finalRotation, moveFix.get() ? MovementCorrection.SILENT : MovementCorrection.OFF, MathUtils.randomizeInt(minYawRotSpeed.get(), maxYawRotSpeed.get()), MathUtils.randomizeInt(minPitchRotSpeed.get(), maxPitchRotSpeed.get()));
                    } else {
                        RotationUtils.setRotation(finalRotation, moveFix.get() ? MovementCorrection.SILENT : MovementCorrection.OFF);
                    }

                    AttackOrder.sendFixedAttack(mc.thePlayer, entity);
                    attackTimer.reset();
                }
            }
        }
    }
}
