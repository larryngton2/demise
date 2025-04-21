package wtf.demise.utils.player;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.PlayerTickEvent;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.PacketUtils;

import java.util.concurrent.ThreadLocalRandom;

public class ClickHandler implements InstanceAccess {
    public final TimerUtils lastTargetTime = new TimerUtils();
    private final TimerUtils lastCPSUpdateTime = new TimerUtils();
    private double currentCPS;
    private static boolean rayTrace;
    private static float minCPS;
    private static float maxCPS;
    private static float CPSUpdateDelay;
    private static float attackRange;
    private static boolean smartClicking;
    private static boolean ignoreBlocking;
    private static EntityLivingBase target;
    private static boolean initialized;
    private static boolean failSwing;
    private static ClickMode clickMode = ClickMode.PlayerController;
    private int clicks;
    private static float swingRange;

    public enum ClickMode {
        Legit,
        Packet,
        PlayerController
    }

    public static void initHandler(float minCPS, float maxCPS, float CPSUpdateDelay, boolean rayTrace, boolean smartClicking, boolean ignoreBlocking, boolean failSwing, float attackRange, float swingRange, ClickMode clickMode, EntityLivingBase target) {
        ClickHandler.minCPS = minCPS;
        ClickHandler.maxCPS = maxCPS;
        ClickHandler.CPSUpdateDelay = CPSUpdateDelay;
        ClickHandler.rayTrace = rayTrace;
        ClickHandler.attackRange = attackRange;
        ClickHandler.target = target;
        ClickHandler.smartClicking = smartClicking;
        ClickHandler.ignoreBlocking = ignoreBlocking;
        ClickHandler.failSwing = failSwing;
        ClickHandler.clickMode = clickMode;
        ClickHandler.swingRange = swingRange;

        initialized = true;
    }

    private void sendAttack() {
        if (rayTrace && rayTraceFailed()) {
            handleFailSwing();
        } else {
            attack();
        }
    }

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (target != null) {
            if (isAttackReady() && initialized) {
                clicks++;
                lastTargetTime.reset();
            }
        } else {
            clicks = 0;
        }
    }

    @EventTarget
    public void onPlayerTickEvent(PlayerTickEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (e.getState() == PlayerTickEvent.State.PRE && target != null && initialized) {
            float distance = (float) PlayerUtils.getDistanceToEntityBox(target);

            KillAura killAura = Demise.INSTANCE.getModuleManager().getModule(KillAura.class);

            if (killAura.isEnabled()) {
                killAura.preAttack();
            }

            for (int i = 0; i < clicks; i++) {
                if (rayTrace) {
                    if (((distance > attackRange || rayTraceFailed()) && distance <= swingRange) && failSwing) {
                        handleFailSwing();
                    }

                    if (!rayTraceFailed()) {
                        sendAttack();
                    }

                } else {
                    sendAttack();
                }

                clicks--;
            }

            if (killAura.isEnabled()) {
                killAura.postAttack();
            }

            initialized = false;
        }
    }

    public static boolean rayTraceFailed() {
        mc.entityRenderer.getMouseOver(1);

        return mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY;
    }

    private boolean isAttackReady() {
        if (lastCPSUpdateTime.hasTimeElapsed(CPSUpdateDelay * 100L)) {
            currentCPS = ThreadLocalRandom.current().nextDouble(minCPS, maxCPS + 1);
            lastCPSUpdateTime.reset();
        }

        boolean check = lastTargetTime.hasTimeElapsed(1000 / currentCPS);

        if (smartClicking && check) {
            return shouldClick();
        } else {
            return check;
        }
    }

    private void handleFailSwing() {
        if (ignoreBlocking || !mc.thePlayer.isUsingItem()) {
            switch (clickMode) {
                case Packet, PlayerController:
                    AttackOrder.sendConditionalSwing(mc.objectMouseOver);
                    break;
                case Legit:
                    if (mc.currentScreen == null) mc.clickMouse();
                    break;
            }
        }
    }

    private boolean shouldClick() {
        return mc.thePlayer.hurtTime != 0 || mc.thePlayer.fallDistance > 0 || target.hurtTime <= 3;
    }

    private void attack() {
        if (ignoreBlocking || !mc.thePlayer.isUsingItem()) {
            switch (clickMode) {
                case Legit:
                    if (mc.currentScreen == null) mc.clickMouse();
                    break;
                case Packet:
                    if (ViaLoadingBase.getInstance().getTargetVersion().isOlderThanOrEqualTo(ProtocolVersion.v1_8)) {
                        mc.thePlayer.swingItem();
                        PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                    } else {
                        PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                        mc.thePlayer.swingItem();
                        break;
                    }

                    Demise.INSTANCE.getEventManager().call(new AttackEvent(target));
                    break;
                case PlayerController:
                    AttackOrder.sendFixedAttack(mc.thePlayer, target);
                    break;
            }
        }
    }
}