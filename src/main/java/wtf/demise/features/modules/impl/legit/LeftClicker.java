package wtf.demise.features.modules.impl.legit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.AntiBot;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.player.ClickHandler;
import wtf.demise.utils.player.PlayerUtils;

import java.util.Arrays;

@ModuleInfo(name = "LeftClicker", category = ModuleCategory.Legit)
public class LeftClicker extends Module {
    public final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 4.0f, 1, 8, 0.1f, this);

    private final BoolValue onlyAttackOnLMB = new BoolValue("Only attack on LMB", true, this);
    private final ModeValue clickMode = new ModeValue("Click mode", new String[]{"Legit", "Packet", "PlayerController"}, "Packet", this);
    private final BoolValue smartClicking = new BoolValue("Smart clicking", false, this);
    private final BoolValue ignoreBlocking = new BoolValue("Ignore blocking", true, this);
    private final SliderValue minCPS = new SliderValue("CPS (min)", 12, 0, 20, 1, this);
    private final SliderValue maxCPS = new SliderValue("CPS (max)", 16, 0, 20, 1, this);
    private final SliderValue cpsUpdateDelay = new SliderValue("CPS update delay", 5, 0, 50, 1, this);
    public final BoolValue rayTrace = new BoolValue("RayTrace", false, this);
    private final BoolValue failSwing = new BoolValue("Fail swing", false, this, rayTrace::get);
    private final SliderValue swingRange = new SliderValue("Swing range", 3.5f, 1, 8, 0.1f, this, () -> failSwing.get() && failSwing.canDisplay());

    private final ModeValue targetMode = new ModeValue("Target selection mode", new String[]{"Single", "Switch"}, "Single", this);
    private final ModeValue targetPriority = new ModeValue("Target Priority", new String[]{"None", "Distance", "Health", "HurtTime"}, "Distance", this, () -> targetMode.is("Single"));

    private final MultiBoolValue allowedTargets = new MultiBoolValue("Allowed targets", Arrays.asList(
            new BoolValue("Players", true),
            new BoolValue("Non players", true),
            new BoolValue("Teams", true),
            new BoolValue("Bots", false),
            new BoolValue("Invisibles", false),
            new BoolValue("Dead", false)
    ), this);

    private EntityLivingBase currentTarget;

    private EntityLivingBase findTarget() {
        EntityLivingBase target = null;
        double closestDistance = searchRange.get() + 0.4;
        double leastHealth = Float.MAX_VALUE;
        double leastHurtTime = 10;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            double distanceToEntity = PlayerUtils.getDistanceToEntityBox(entity);

            if (entity != mc.thePlayer && distanceToEntity <= searchRange.get()) {
                if (!(entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager || entity instanceof EntityPlayer)) {
                    continue;
                }

                if (entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager) {
                    if (!allowedTargets.isEnabled("Non players")) continue;
                }

                if (entity.isInvisible() && !allowedTargets.isEnabled("Invisibles")) continue;
                if (entity.isDead && !allowedTargets.isEnabled("Dead")) continue;

                if (entity instanceof EntityPlayer) {
                    if (!allowedTargets.isEnabled("Players")) continue;
                    if (Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity)) continue;
                    if (!allowedTargets.isEnabled("Bots") && getModule(AntiBot.class).isBot((EntityPlayer) entity))
                        continue;
                    if (PlayerUtils.isInTeam(entity) && !allowedTargets.isEnabled("Teams")) continue;
                }

                switch (targetPriority.get()) {
                    case "Distance":
                        if (distanceToEntity < closestDistance) {
                            target = (EntityLivingBase) entity;
                            closestDistance = distanceToEntity;
                        }
                        break;
                    case "Health":
                        EntityLivingBase potentialTarget = (EntityLivingBase) entity;
                        float potentialHealth = PlayerUtils.getActualHealth(potentialTarget);
                        if (potentialHealth < leastHealth) {
                            target = potentialTarget;
                            leastHealth = potentialHealth;
                        }
                        break;
                    case "HurtTime":
                        EntityLivingBase potentialTarget2 = (EntityLivingBase) entity;
                        float potentionalHurtTime = potentialTarget2.hurtTime;
                        if (potentionalHurtTime <= leastHurtTime) {
                            target = potentialTarget2;
                            leastHurtTime = potentionalHurtTime;
                        }
                        break;
                }
            }
        }

        return target;
    }

    private boolean isTargetInvalid() {
        return currentTarget.isDead || PlayerUtils.getDistanceToEntityBox(currentTarget) > searchRange.get() || currentTarget.getEntityWorld() != mc.thePlayer.getEntityWorld();
    }

    @EventTarget
    public void onGameTick(GameEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        setTag(minCPS.get() + " - " + maxCPS.get());

        currentTarget = findTarget();

        if (onlyAttackOnLMB.get() && !mc.gameSettings.keyBindAttack.isKeyDown()) {
            return;
        }

        if (currentTarget != null && !isTargetInvalid()) {
            ClickHandler.ClickMode mode = ClickHandler.ClickMode.valueOf(clickMode.get());

            ClickHandler.initHandler(minCPS.get(), maxCPS.get(), cpsUpdateDelay.get(), rayTrace.get() && rayTrace.canDisplay(), smartClicking.get(), ignoreBlocking.get(), failSwing.get(), attackRange.get(), swingRange.get(), mode, currentTarget);
        }
    }
}
