package wtf.demise.utils.player.clicking;

import lombok.Getter;
import net.minecraft.entity.EntityLivingBase;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.InstanceAccess;

@Getter
public class ClickManager implements InstanceAccess {
    final ModeValue clickMode;
    final BoolValue smartClicking;
    final BoolValue forceAttackOnBacktrack;
    final BoolValue ignoreBlocking;
    final SliderValue minCPS;
    final SliderValue maxCPS;
    final BoolValue cooldown;
    final BoolValue rayTrace;
    final BoolValue failSwing;
    final SliderValue swingRange;

    public ClickManager(Module module) {
        clickMode = new ModeValue("Click mode", new String[]{"Legit", "Packet", "PlayerController"}, "Packet", module);
        smartClicking = new BoolValue("Smart clicking", false, module);
        forceAttackOnBacktrack = new BoolValue("Force attack on BackTrack", false, module, smartClicking::get);
        ignoreBlocking = new BoolValue("Ignore blocking", true, module);
        minCPS = new SliderValue("Min CPS", 12, 0, 20, 1, module);
        maxCPS = new SliderValue("Max CPS", 16, 0, 20, 1, module);
        cooldown = new BoolValue("1.9+ cooldown", false, module);
        rayTrace = new BoolValue("Raytrace", false, module);
        failSwing = new BoolValue("Fail swing", false, module);
        swingRange = new SliderValue("Swing range", 3.5f, 1, 8, 0.1f, module, failSwing::get);
    }

    public void click(float attackRange, EntityLivingBase target) {
        ClickHandler.ClickMode mode = ClickHandler.ClickMode.valueOf(clickMode.get());
        ClickHandler.initHandler(minCPS.get(), maxCPS.get(), rayTrace.get() && rayTrace.canDisplay(), smartClicking.get(), forceAttackOnBacktrack.get(), ignoreBlocking.get(), failSwing.get(), cooldown.get(), attackRange, swingRange.get(), mode, target);
    }
}