package wtf.demise.utils.packet;

import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.TimerUtils;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PingSpoofComponent implements InstanceAccess {
    static Tuple<Class[], Boolean> regular = new Tuple<>(new Class[]{C0FPacketConfirmTransaction.class, C00PacketKeepAlive.class, S1CPacketEntityMetadata.class}, false);
    static Tuple<Class[], Boolean> velocity = new Tuple<>(new Class[]{S12PacketEntityVelocity.class, S27PacketExplosion.class}, false);
    static Tuple<Class[], Boolean> teleports = new Tuple<>(new Class[]{S08PacketPlayerPosLook.class, S39PacketPlayerAbilities.class, S09PacketHeldItemChange.class}, false);
    static Tuple<Class[], Boolean> players = new Tuple<>(new Class[]{S13PacketDestroyEntities.class, S14PacketEntity.class, S14PacketEntity.S16PacketEntityLook.class, S14PacketEntity.S15PacketEntityRelMove.class, S14PacketEntity.S17PacketEntityLookMove.class, S18PacketEntityTeleport.class, S20PacketEntityProperties.class, S19PacketEntityHeadLook.class}, false);
    static Tuple<Class[], Boolean> blink = new Tuple<>(new Class[]{C02PacketUseEntity.class, C0DPacketCloseWindow.class, C0EPacketClickWindow.class, C0CPacketInput.class, C0BPacketEntityAction.class, C08PacketPlayerBlockPlacement.class, C07PacketPlayerDigging.class, C09PacketHeldItemChange.class, C13PacketPlayerAbilities.class, C15PacketClientSettings.class, C16PacketClientStatus.class, C17PacketCustomPayload.class, C18PacketSpectate.class, C19PacketResourcePackStatus.class, C03PacketPlayer.class, C03PacketPlayer.C04PacketPlayerPosition.class, C03PacketPlayer.C05PacketPlayerLook.class, C03PacketPlayer.C06PacketPlayerPosLook.class, C0APacketAnimation.class}, false);
    static Tuple<Class[], Boolean> movement = new Tuple<>(new Class[]{C03PacketPlayer.class, C03PacketPlayer.C04PacketPlayerPosition.class, C03PacketPlayer.C05PacketPlayerLook.class, C03PacketPlayer.C06PacketPlayerPosLook.class}, false);
    public static Tuple<Class[], Boolean>[] types = new Tuple[]{regular, velocity, teleports, players, blink, movement};

    public static ConcurrentLinkedQueue<TimedPacket> packets = new ConcurrentLinkedQueue<>();
    static TimerUtils enabledTimer = new TimerUtils();
    public static boolean enabled;
    static long amount;
    private static boolean picked;
    private static double x, y, z;
    private static double realX, realY, realZ;

    @EventTarget
    public void onPacketC(PacketEvent event) {
        if (event.getState() == PacketEvent.State.OUTGOING) {
            event.setCancelled(onPacket(event.getPacket(), event).isCancelled());
        }
    }

    @EventTarget
    public void onPacketS(PacketEvent event) {
        if (event.getState() == PacketEvent.State.INCOMING) {
            event.setCancelled(onPacket(event.getPacket(), event).isCancelled());
        }
    }

    public PacketEvent onPacket(Packet<?> packet, PacketEvent event) {
        if (!event.isCancelled() && enabled && Arrays.stream(types).anyMatch(tuple -> tuple.getSecond() && Arrays.stream(tuple.getFirst()).anyMatch(regularpacket -> regularpacket == packet.getClass()))) {
            event.setCancelled(true);
            packets.add(new TimedPacket(packet));
        }

        return event;
    }

    public static void dispatch() {
        if (!packets.isEmpty()) {
            // Stops the packets from being called twice
            boolean enabled = PingSpoofComponent.enabled;
            PingSpoofComponent.enabled = false;
            packets.forEach(timedPacket -> PacketUtils.queue(timedPacket.getPacket()));
            PingSpoofComponent.enabled = enabled;
            packets.clear();
        }
    }

    public static void disable() {
        enabled = false;
        enabledTimer.setTime(enabledTimer.getTime() - 999999999);
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        disable();
        dispatch();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!(enabled = !enabledTimer.hasTimeElapsed(100) && !(mc.currentScreen instanceof GuiDownloadTerrain))) {
            dispatch();
        } else {
            // Stops the packets from being called twice
            enabled = false;

            packets.forEach(packet -> {
                if (packet.getMillis() + amount < System.currentTimeMillis()) {
                    if (packet.getPacket() instanceof C03PacketPlayer c03PacketPlayer) {
                        realX = c03PacketPlayer.getPositionX();
                        realY = c03PacketPlayer.getPositionY();
                        realZ = c03PacketPlayer.getPositionZ();
                    }
                    PacketUtils.queue(packet.getPacket());
                    packets.remove(packet);
                }
            });

            enabled = true;
        }
    }

    public static Vec3 getRealPos() {
        if (realX != 0 && realY != 0 && realZ != 0) {
            x = realX;
            y = realY;
            z = realZ;
        } else {
            if (!picked) {
                x = mc.thePlayer.posX;
                y = mc.thePlayer.posY;
                z = mc.thePlayer.posZ;
                picked = true;
            }
        }

        return new Vec3(x, y, z);
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players) {
        spoof(amount, regular, velocity, teleports, players, false);
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean blink, boolean movement) {
        enabledTimer.reset();

        PingSpoofComponent.regular.setSecond(regular);
        PingSpoofComponent.velocity.setSecond(velocity);
        PingSpoofComponent.teleports.setSecond(teleports);
        PingSpoofComponent.players.setSecond(players);
        PingSpoofComponent.blink.setSecond(blink);
        PingSpoofComponent.movement.setSecond(movement);
        PingSpoofComponent.amount = amount;
    }

    public static void spoof(int amount, boolean regular, boolean velocity, boolean teleports, boolean players, boolean blink) {
        spoof(amount, regular, velocity, teleports, players, blink, false);
    }

    public static void blink() {
        spoof(9999999, true, false, false, false, true);
    }
}
