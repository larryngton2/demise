package wtf.demise.features.modules;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.Value;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.animations.Translate;
import wtf.demise.utils.animations.impl.EaseInOutQuad;
import wtf.demise.utils.packet.PacketUtils;

import java.util.*;

public abstract class Module implements InstanceAccess {
    private static final String FUNNY = "§7 chatgpt pro billionaire bypass india vs pakistan";

    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    @Setter
    private int keyBind;
    @Getter
    private String tag = "";
    @Getter
    private final List<Value> values = new ArrayList<>();
    @Getter
    @Setter
    private boolean hidden;
    private boolean state;
    @Getter
    @Setter
    private boolean expanded;
    @Getter
    private final EaseInOutQuad animation = new EaseInOutQuad(175, 1);
    @Getter
    private final Translate translate = new Translate(0.0, 0.0);
    protected final Random rand = new Random();

    protected Module() {
        ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
        Objects.requireNonNull(moduleInfo, "ModuleInfo annotation is missing on " + getClass().getName());
        this.name = moduleInfo.name();
        this.description = moduleInfo.description();
        this.keyBind = moduleInfo.key();
    }

    /**
     * Sets the module's tag based on the global tag configuration.
     *
     * @param tag The tag to set.
     */
    public void setTag(String tag) {
        Interface interfaceModule = getModule(Interface.class);
        if (interfaceModule != null && interfaceModule.funy.get() && !Objects.equals(tag, "")) {
            this.tag = FUNNY;
            return;
        }

        if (tag == null || tag.isEmpty()) {
            this.tag = "";
            return;
        }

        this.tag = " §7" + tag;
    }

    public boolean isEnabled() {
        return state;
    }

    public boolean isDisabled() {
        return !state;
    }

    public <M extends Module> boolean isEnabled(Class<M> module) {
        Module mod = Demise.INSTANCE.getModuleManager().getModule(module);
        return mod != null && mod.isEnabled();
    }

    public <M extends Module> boolean isDisabled(Class<M> module) {
        return !isEnabled(module);
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    public void setEnabled(boolean enabled) {
        if (this.state != enabled) {
            this.state = enabled;
            if (enabled) {
                enable();
            } else {
                disable();
            }
        }
    }

    private void enable() {
        Demise.INSTANCE.getEventManager().register(this);
        try {
            onEnable();
            Demise.INSTANCE.getNotificationManager().post(NotificationType.ENABLE, "Enabled", "Module " + getName());
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void disable() {
        Demise.INSTANCE.getEventManager().unregister(this);
        try {
            onDisable();
            Demise.INSTANCE.getNotificationManager().post(NotificationType.DISABLE, "Disabled", "Module " + getName());
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) {
        if (mc.thePlayer != null) {
            e.printStackTrace();
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public <M extends Module> M getModule(Class<M> clazz) {
        return Demise.INSTANCE.getModuleManager().getModule(clazz);
    }

    public void addValues(Value... settings) {
        values.addAll(Arrays.asList(settings));
    }

    public void addValue(Value value) {
        addValues(value);
    }

    public void sendPacket(Packet packet) {
        PacketUtils.sendPacket(packet);
    }

    public void sendPacketNoEvent(Packet packet) {
        PacketUtils.sendPacketNoEvent(packet);
    }

    public Value getValue(String valueName) {
        return values.stream()
                .filter(value -> value.getName().equalsIgnoreCase(valueName))
                .findFirst()
                .orElse(null);
    }
}