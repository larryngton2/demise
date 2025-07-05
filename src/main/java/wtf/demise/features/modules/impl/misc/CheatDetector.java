package wtf.demise.features.modules.impl.misc;

import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.AntiBot;
import wtf.demise.features.modules.impl.misc.cheatdetector.Check;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat.InvalidInteract;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat.VelocityCheck;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.combat.aim.AimCheck;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.OmniSprintCheck;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.motion.MotionCheck;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.movement.noslow.NoSlowCheck;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.LegitScaffoldCheck;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.NoFallCheck;
import wtf.demise.features.modules.impl.misc.cheatdetector.impl.player.scaffold.ScaffoldCheck;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.PlayerUtils;

import java.util.*;
import java.util.stream.Collectors;

@ModuleInfo(name = "CheatDetector", description = "Detects cheaters in your game.")
public class CheatDetector extends Module {
    public final MultiBoolValue options = new MultiBoolValue("Checks", Arrays.asList(
            new BoolValue("Aim", true),
            new BoolValue("Invalid interact", true),
            new BoolValue("Motion", true),
            new BoolValue("No fall", true),
            new BoolValue("No slow", true),
            new BoolValue("Omni sprint", true),
            new BoolValue("Scaffold", true),
            new BoolValue("Velocity", true),
            new BoolValue("Legit scaffold", true)
    ), this);

    public final BoolValue selfCheck = new BoolValue("Check self", false, this);
    public final SliderValue alertCoolDown = new SliderValue("Alert Cooldown", 1000, 0, 2000, 1, this);

    private final Set<EntityPlayer> cheaters = new HashSet<>();
    private final ArrayList<Check> checks = new ArrayList<>();

    public CheatDetector() {
        addChecks(
                new AimCheck(),
                new InvalidInteract(),
                new MotionCheck(),
                new NoFallCheck(),
                new NoSlowCheck(),
                new ScaffoldCheck(),
                new VelocityCheck(),
                new OmniSprintCheck(),
                new LegitScaffoldCheck()
        );
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.theWorld == null) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            // chunks are 16x16 blocks wide, just doing -1 to be sure
            if (PlayerUtils.getCustomDistanceToEntityBox(player.getPositionEyes(1), mc.thePlayer) > 16 * mc.gameSettings.renderDistanceChunks)
                continue;

            for (Check check : checks) {
                if ((selfCheck.get() || player != mc.thePlayer) && !player.isDead && !Demise.INSTANCE.getFriendManager().isFriend(player)) {
                    if (isEnabled(AntiBot.class) && getModule(AntiBot.class).bots.contains(player))
                        continue;

                    if (options.isEnabled(check.getName())) {
                        check.onUpdate(player);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (mc.theWorld == null) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (PlayerUtils.getCustomDistanceToEntityBox(player.getPositionEyes(1), mc.thePlayer) > 16 * mc.gameSettings.renderDistanceChunks)
                continue;

            for (Check check : checks) {
                if ((selfCheck.get() || player != mc.thePlayer) && !player.isDead && !Demise.INSTANCE.getFriendManager().isFriend(player)) {
                    if (isEnabled(AntiBot.class) && getModule(AntiBot.class).bots.contains(player))
                        continue;

                    if (options.isEnabled(check.getName())) {
                        check.onPacket(e, player);
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        cheaters.clear();
    }

    public void addChecks(Check... checks) {
        this.checks.addAll(Arrays.asList(checks));
    }

    public void mark(EntityPlayer ent) {
        cheaters.add(ent);
    }

    public boolean isCheater(EntityPlayer ent) {
        for (EntityPlayer player : cheaters) {
            if (!ent.getName().equals(player.getName())) continue;
            return true;
        }
        return false;
    }

    public void cleanup() {
        Set<UUID> onlineUUIDs = mc.theWorld.playerEntities.stream()
                .map(EntityPlayer::getUniqueID)
                .collect(Collectors.toSet());

        checks.forEach(check -> check.cleanup(onlineUUIDs));
    }
}