package wtf.demise.features.modules;

import lombok.Getter;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.KeyPressEvent;
import wtf.demise.features.modules.impl.combat.*;
import wtf.demise.features.modules.impl.combat.killaura.KillAura;
import wtf.demise.features.modules.impl.exploit.*;
import wtf.demise.features.modules.impl.legit.*;
import wtf.demise.features.modules.impl.misc.*;
import wtf.demise.features.modules.impl.movement.*;
import wtf.demise.features.modules.impl.player.*;
import wtf.demise.features.modules.impl.visual.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all modules within the client.
 * Responsible for initializing, registering, and handling modules.
 */
@Getter
public class ModuleManager {
    private final List<Module> modules = new CopyOnWriteArrayList<>();

    /**
     * Initializes the ModuleManager by adding all available modules,
     * sorting them by name, and registering event listeners.
     */
    public ModuleManager() {
        addModules(
                // Combat
                AntiBot.class,
                AutoWeapon.class,
                KeepSprint.class,
                KillAura.class,
                Reach.class,
                TickBase.class,
                Velocity.class,
                AutoHead.class,
                Criticals.class,
                FakeLag.class,
                MoreKB.class,
                HitBox.class,
                AutoRod.class,
                TimerRange.class,

                // Legit
                AutoClicker.class,
                NoHitDelay.class,
                LegitScaffold.class,
                BackTrack.class,
                JumpReset.class,

                // Exploit
                Blink.class,
                ClientSpoofer.class,
                Disabler.class,
                PingSpoof.class,
                Timer.class,
                NoGuiClose.class,
                Regen.class,
                TPAura.class,
                ResetVL.class,
                ComboOneHit.class,

                // Misc
                Twerk.class,
                AntiCheat.class,
                AutoMeow.class,
                MurderMystery.class,
                InventorySync.class,
                AutoQueue.class,

                // Movement
                Freeze.class,
                InvMove.class,
                LongJump.class,
                JumpDelay.class,
                NoSlow.class,
                SafeWalk.class,
                Speed.class,
                Sprint.class,
                Step.class,
                Strafe.class,
                Fly.class,
                Phase.class,
                Sneak.class,
                MotionModifier.class,
                AutoWalk.class,
                Jesus.class,

                // Player
                AntiFireball.class,
                AutoTool.class,
                FastPlace.class,
                InvManager.class,
                NoFall.class,
                Stealer.class,
                BedNuker.class,
                NoRot.class,
                FastUse.class,
                FastBreak.class,
                Scaffold.class,

                // Visual
                Atmosphere.class,
                Animations.class,
                BedPlates.class,
                BlockOverlay.class,
                Camera.class,
                Chams.class,
                ChestESP.class,
                ClickGUI.class,
                ESP.class,
                FreeLook.class,
                FullBright.class,
                Interface.class,
                Rotation.class,
                Shaders.class,
                Trajectories.class,
                Breadcrumbs.class,
                ChinaHat.class,
                Particles.class,
                MoveStatus.class,
                ItemPhysics.class,
                ItemESP.class,
                FreeCam.class,
                ViewBobbing.class,
                BreakProgress.class,
                Dot.class,
                Cape.class
        );

        // Sort modules by name for better organization
        modules.sort(Comparator.comparing(Module::getName));

        // Register the ModuleManager to listen for events
        Demise.INSTANCE.getEventManager().register(this);
        //  Demise.LOGGER.INFO("ModuleManager initialized with {} modules.", modules.size());
    }

    /**
     * Adds multiple modules to the manager by instantiating their classes.
     *
     * @param moduleClasses Varargs of module classes to add.
     */
    @SafeVarargs
    public final void addModules(Class<? extends Module>... moduleClasses) {
        for (Class<? extends Module> moduleClass : moduleClasses) {
            try {
                Module module = moduleClass.getDeclaredConstructor().newInstance();
                modules.add(module);
                //  Demise.LOGGER.INFO("Added module: {}", module.getName());
            } catch (Exception e) {
                //  Demise.LOGGER.INFO("Failed to instantiate module: {}", moduleClass.getSimpleName(), e);
            }
        }
    }

    /**
     * Retrieves a module instance based on its class type.
     *
     * @param moduleClass The class of the module to retrieve.
     * @param <T>         The type of the module.
     * @return An instance of the requested module or null if not found.
     */
    public <T extends Module> T getModule(Class<T> moduleClass) {
        Optional<Module> module = modules.stream()
                .filter(m -> m.getClass().equals(moduleClass))
                .findFirst();

        return module.map(moduleClass::cast).orElse(null);
    }

    /**
     * Retrieves a module instance based on its name.
     *
     * @param name The name of the module to retrieve.
     * @return The module instance if found, otherwise null.
     */
    public Module getModule(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves all modules that belong to a specific category.
     *
     * @param category The category to filter modules by.
     * @return A list of modules within the specified category.
     */
    public List<Module> getModulesByCategory(ModuleCategory category) {
        List<Module> categorizedModules = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                categorizedModules.add(module);
            }
        }
        return categorizedModules;
    }

    /**
     * Event handler for key press events.
     * Toggles the corresponding module if its keybind matches the pressed key.
     *
     * @param event The key press event.
     */
    @EventTarget
    public void onKey(KeyPressEvent event) {
        modules.stream()
                .filter(module -> module.getKeyBind() == event.getKey())
                .forEach(Module::toggle);
    }

    /**
     * Retrieves all modules managed by this manager.
     *
     * @return An unmodifiable list of all modules.
     */
    public List<Module> getAllModules() {
        return List.copyOf(modules);
    }
}
