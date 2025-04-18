package wtf.demise.features.modules.impl.combat;

import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.HitSlowDownEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "KeepSprint", category = ModuleCategory.Combat)
public class KeepSprint extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Smart"}, "Normal", this);
    private final SliderValue motion = new SliderValue("Motion", 0.6f, 0.6f, 1, 0.01f, this);
    private final SliderValue hurtTime = new SliderValue("HurtTime", 7, 0, 10, 1, this, () -> mode.is("Smart"));
    private final BoolValue sprint = new BoolValue("Sprint", true, this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get());
    }

    @EventTarget
    public void onHitSlowDown(HitSlowDownEvent e) {
        Velocity velocity = getModule(Velocity.class);

        if (velocity.isEnabled() && velocity.mode.isEnabled("Reduce")) return;

        switch (mode.get()) {
            case "Normal":
                e.setSlowDown(motion.get());
                e.setSprint(sprint.get());
                break;
            case "Smart":
                e.setSlowDown(mc.thePlayer.hurtTime >= hurtTime.get() ? motion.get() : 1);
                e.setSprint(!(mc.thePlayer.hurtTime >= hurtTime.get()) || sprint.get());
        }
    }
}