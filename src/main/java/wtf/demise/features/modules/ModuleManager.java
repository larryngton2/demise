package wtf.demise.features.modules;

import lombok.Getter;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.KeyPressEvent;
import wtf.demise.features.modules.impl.combat.*;
import wtf.demise.features.modules.impl.exploit.*;
import wtf.demise.features.modules.impl.exploit.Timer;
import wtf.demise.features.modules.impl.legit.*;
import wtf.demise.features.modules.impl.misc.*;
import wtf.demise.features.modules.impl.misc.bloxdphysics.BloxdPhysics;
import wtf.demise.features.modules.impl.movement.*;
import wtf.demise.features.modules.impl.player.*;
import wtf.demise.features.modules.impl.visual.*;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all modules within the client.
 * Responsible for initializing, registering, and handling modules.
 */
@Getter
public class ModuleManager {
    private static final Set<Class<? extends Module>> COMBAT_MODULES = Set.of(
            AntiBot.class, Criticals.class, FakeLag.class, KeepSprint.class, KillAura.class, TickBase.class,
            TimerRange.class, Velocity.class, LagRange.class
    );

    private static final Set<Class <? extends Module>> LEGIT_MODULES = Set.of(
            AimAssist.class, AutoClicker.class, AutoHeal.class, AutoRod.class, AutoWeapon.class, BackTrack.class,
            CombatHelper.class, HitBox.class, HitSelect.class, JumpReset.class, LegitScaffold.class, NoHitDelay.class,
            Reach.class, SprintReset.class
    );

    private static final Set<Class<? extends Module>> MOVEMENT_MODULES = Set.of(
            AutoWalk.class, Fly.class, Freeze.class, InvMove.class, Jesus.class, JumpDelay.class, LongJump.class,
            MotionModifier.class, MoveHelper.class, NoSlow.class, Phase.class, SafeWalk.class, Sneak.class, Speed.class,
            Sprint.class, Step.class, Strafe.class, TargetStrafe.class, WallClimb.class
    );

    private static final Set<Class<? extends Module>> PLAYER_MODULES = Set.of(
        AntiVoid.class, AutoTool.class, BedNuker.class, FastBow.class, FastBreak.class,
            FastPlace.class, FastUse.class, Manager.class, NoFall.class, Scaffold.class, Stealer.class
    );

    private static final Set<Class<? extends Module>> MISC_MODULES = Set.of(
        AnnoyUtils.class, AutoQueue.class, AutoRegister.class, ExplosionBlock.class, FlagDetector.class,
            InventorySync.class, MurderMystery.class, Test.class, Twerk.class, BloxdPhysics.class, Gambling.class, AutoMeow.class
    );

    private static final Set<Class<? extends Module>> EXPLOIT_MODULES = Set.of(
        AutoBan.class, Blink.class, ClientSpoofer.class, ComboOneHit.class, Disabler.class, NoGuiClose.class,
            Regen.class, ResetVL.class, Timer.class
    );

    private static final Set<Class<? extends Module>> VISUAL_MODULES = Set.of(
        Atmosphere.class, BlockOnSwing.class, BlockOverlay.class, Breadcrumbs.class, BreakProgress.class, Cape.class,
            ChestESP.class, ChinaHat.class, ClickGUI.class, CustomSkin.class, CustomWidgetsModule.class, ESP.class,
            ForceDinnerbone.class, FreeCam.class, FullBright.class, ImageESP.class, Interface.class, ItemESP.class,
            ItemGlow.class, ItemPhysics.class, MainMenuOptions.class, MotionBlur.class, MoveStatus.class,
            NoHurtCam.class, NoRenderOffsetReset.class, Particles.class, Rotation.class, Shaders.class,
            ThirdPersonDistance.class, Trajectories.class, ViewBobbing.class, VisualAimPoint.class
    );

    private final Map<ModuleCategory, List<Module>> modulesByCategory;
    private final List<Module> allModules;

    public ModuleManager() {
        this.modulesByCategory = new ConcurrentHashMap<>();
        this.allModules = new ArrayList<>();

        initializeModules();
        sortModules();
        registerEventListener();
    }

    private void initializeModules() {
        Arrays.stream(ModuleCategory.values()).forEach(category -> modulesByCategory.put(category, new ArrayList<>()));

        initializeCategoryModules(COMBAT_MODULES, ModuleCategory.Combat);
        initializeCategoryModules(LEGIT_MODULES, ModuleCategory.Legit);
        initializeCategoryModules(MOVEMENT_MODULES, ModuleCategory.Movement);
        initializeCategoryModules(PLAYER_MODULES, ModuleCategory.Player);
        initializeCategoryModules(MISC_MODULES, ModuleCategory.Misc);
        initializeCategoryModules(EXPLOIT_MODULES, ModuleCategory.Exploit);
        initializeCategoryModules(VISUAL_MODULES, ModuleCategory.Visual);
    }

    private void initializeCategoryModules(Set<Class<? extends Module>> moduleClasses, ModuleCategory category) {
        moduleClasses.stream()
                .map(this::instantiateModule)
                .filter(Objects::nonNull)
                .forEach(module -> {
                    modulesByCategory.get(category).add(module);
                    allModules.add(module);
                });
    }

    private Module instantiateModule(Class<? extends Module> moduleClass) {
        try {
            return moduleClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Demise.LOGGER.error("Failed to instantiate module: {}", moduleClass.getSimpleName(), e);
            return null;
        }
    }

    private void sortModules() {
        allModules.sort(Comparator.comparing(Module::getName));
        modulesByCategory.values()
                .forEach(modules -> modules.sort(Comparator.comparing(Module::getName)));
    }

    private void registerEventListener() {
        Demise.INSTANCE.getEventManager().register(this);
    }

    public <T extends Module> T getModule(Class<T> moduleClass) {
        return allModules.stream()
                .filter(m -> m.getClass().equals(moduleClass))
                .map(moduleClass::cast)
                .findFirst()
                .orElse(null);
    }

    public Module getModule(String name) {
        return allModules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Module> getModulesByCategory(ModuleCategory category) {
        return Collections.unmodifiableList(modulesByCategory.getOrDefault(category, Collections.emptyList()));
    }

    @EventTarget
    public void onKey(KeyPressEvent event) {
        allModules.stream()
                .filter(module -> module.getKeyBind() == event.getKey())
                .forEach(Module::toggle);
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(allModules);
    }

    public List<Module> getEnabledModules() {
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : getModules()) {
            Animation moduleAnimation = module.getAnimation();
            moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
            enabledModules.add(module);
        }
        return enabledModules;
    }
}