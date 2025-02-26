package wtf.demise.features.modules.impl.visual;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.EvictingList;
import wtf.demise.utils.render.Particle;
import wtf.demise.utils.render.RenderUtils;

import java.util.List;

@ModuleInfo(name = "Particles", category = ModuleCategory.Visual)
public class Particles extends Module {
    private final SliderValue amount = new SliderValue("Amount", 10, 1, 20, 1, this);
    private final BoolValue physics = new BoolValue("Physics", true, this);
    private final SliderValue simulationSpeed = new SliderValue("Simulation speed", 0.25f, 0.01f, 1, 0.01f, this);

    private final List<Particle> particles = new EvictingList<>(100);
    private final TimerUtils timer = new TimerUtils();
    private EntityLivingBase target;

    @EventTarget
    public void onAttackEvent(final AttackEvent event) {
        if (event.getTargetEntity() instanceof EntityLivingBase) target = (EntityLivingBase) event.getTargetEntity();
    }

    @EventTarget
    public void onPreMotion(final MotionEvent event) {
        if (event.isPre()) {
            if (target != null && target.hurtTime >= 9 && mc.thePlayer.getDistance(target.posX, target.posY, target.posZ) < 10) {
                for (int i = 0; i < amount.get(); i++) {
                    particles.add(new Particle(new Vec3(target.posX + (Math.random() - 0.5) * 0.5, target.posY + Math.random() * 1 + 0.5, target.posZ + (Math.random() - 0.5) * 0.5)));
                }

                target = null;
            }
        }
    }

    @EventTarget
    public void onRender3DEvent(final Render3DEvent event) {
        if (particles.isEmpty())
            return;

        for (int i = 0; i <= timer.getTime() / simulationSpeed.get(); i++) {
            if (physics.get()) {
                particles.forEach(Particle::update);
            } else {
                particles.forEach(Particle::updateWithoutPhysics);
            }
        }

        particles.removeIf(particle -> mc.thePlayer.getDistanceSq(particle.getPosition().xCoord, particle.getPosition().yCoord, particle.getPosition().zCoord) > 50 * 10);

        timer.reset();

        RenderUtils.renderParticles(particles);
    }
}