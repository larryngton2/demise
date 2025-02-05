package wtf.demise.utils.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;

import java.util.ArrayList;
import java.util.List;

public class ParticleRenderer implements InstanceAccess {
    public static final List<THParticle> particles = new ArrayList<>();
    public static int rendered;
    public static final TimerUtils timer = new TimerUtils();
    private static boolean sentParticles;

    public static void renderParticle(EntityLivingBase target, float x, float y, float test) {

        for (THParticle p : particles) {
            GlStateManager.color(1, 1, 1, 1);
            if (p.opacity > 4) p.render2D();
        }
        if (timer.hasTimeElapsed(1000 / 60)) {
            for (THParticle p : particles) {
                p.updatePosition();
                if (p.opacity < 1) particles.remove(p);
            }
            timer.reset();
        }
        if (target.hurtTime == 9 && !sentParticles) {
            for (int i = 0; i <= 10; i++) {
                THParticle particle = new THParticle();
                particle.init(x + 20, y + 20, (float) (((Math.random() - 0.5) * 2) * 1.4), (float) (((Math.random() - 0.5) * 2) * 1.4),
                        (float) (MathUtils.randomizeDouble(4, 5)), i % 2 == 0 ? INSTANCE.getModuleManager().getModule(Interface.class).color(i * 100, 255) :INSTANCE.getModuleManager().getModule(Interface.class).color(-i * 100, 255));
                particles.add(particle);
            }
            sentParticles = true;
        }
        if (target.hurtTime == 8) sentParticles = false;
    }
}
