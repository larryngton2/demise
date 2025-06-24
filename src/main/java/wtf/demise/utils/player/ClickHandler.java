package wtf.demise.utils.player;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.StaticTickEvent;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.modules.impl.legit.BackTrack;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.PacketUtils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class ClickHandler implements InstanceAccess {
    public final TimerUtils lastTargetTime = new TimerUtils();
    private static boolean rayTrace;
    private static float minCPS;
    private static float maxCPS;
    private static float attackRange;
    private static boolean smartClicking;
    private static boolean ignoreBlocking;
    private static EntityLivingBase target;
    private static boolean initialized;
    private static boolean failSwing;
    private static ClickMode clickMode = ClickMode.PlayerController;
    private final Queue<Integer> clickPattern = new LinkedList<>();
    private static float swingRange;
    private static boolean forceblahblahblah;
    private final TimerUtils patternUpdateTimer = new TimerUtils();
    private final TimerUtils blockTimer = new TimerUtils();
    public static boolean clickingNow;
    private int cachedClicks;

    public enum ClickMode {
        Legit,
        Packet,
        PlayerController
    }

    public static void initHandler(float minCPS, float maxCPS, boolean rayTrace, boolean smartClicking, boolean forceClickOnBackTrack, boolean ignoreBlocking, boolean failSwing, float attackRange, float swingRange, ClickMode clickMode, EntityLivingBase target) {
        ClickHandler.minCPS = minCPS;
        ClickHandler.maxCPS = maxCPS;
        ClickHandler.rayTrace = rayTrace;
        ClickHandler.attackRange = attackRange;
        ClickHandler.target = target;
        ClickHandler.smartClicking = smartClicking;
        ClickHandler.ignoreBlocking = ignoreBlocking;
        ClickHandler.failSwing = failSwing;
        ClickHandler.clickMode = clickMode;
        ClickHandler.swingRange = swingRange;
        ClickHandler.forceblahblahblah = forceClickOnBackTrack;

        initialized = true;
    }

    private void generateClickPattern() {
        clickPattern.clear();
        double cps = ThreadLocalRandom.current().nextDouble(minCPS, maxCPS + 1);
        int clicksPerSecond = (int) Math.round(cps);

        int totalTicks = 20;
        int clicksToDistribute = clicksPerSecond;

        for (int i = 0; i < totalTicks; i++) {
            double probability = (double) clicksToDistribute / (totalTicks - i);

            if (ThreadLocalRandom.current().nextDouble() < probability) {
                clickPattern.add(1);
                clicksToDistribute--;
                continue;
            }

            clickPattern.add(0);
        }

        patternUpdateTimer.reset();
    }

    private boolean shouldClickThisTick() {
        if (clickPattern.isEmpty() || patternUpdateTimer.hasTimeElapsed(1000)) {
            generateClickPattern();
        }

        Integer nextClick = clickPattern.poll();

        boolean check = nextClick != null && nextClick == 1;

        if (smartClicking && check) {
            return shouldClick();
        }

        return check;
    }

    private void sendAttack() {
        if (rayTrace && rayTraceFailed()) {
            handleFailSwing();
        } else {
            attack();
        }

        clickingNow = true;
    }

    @EventTarget
    public void onStaticTick(StaticTickEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (target != null && initialized) {
            if (shouldClickThisTick()) {
                cachedClicks++;
            }
        } else {
            cachedClicks = 0;
        }

        if (mc.skippedTick) {
            finalizeHandler();
        }
    }

    @EventTarget
    public void onTickEvent(TickEvent e) {
        clickingNow = false;

        // just to make sure lol
        if (!mc.skippedTick) {
            finalizeHandler();
        }
    }

    private void finalizeHandler() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (target != null && initialized) {
            KillAura killAura = Demise.INSTANCE.getModuleManager().getModule(KillAura.class);

            if (killAura.isEnabled()) {
                killAura.preAttack();
            }

            for (int i = 0; i < cachedClicks; i++) {
                lastTargetTime.reset();

                if (!ignoreBlocking && mc.thePlayer.isUsingItem()) {
                    blockTimer.reset();
                }

                float distance = (float) PlayerUtils.getDistanceToEntityBox(target);
                if (distance > attackRange && distance <= swingRange && failSwing) {
                    handleFailSwing();
                } else if (distance <= attackRange) {
                    if (!rayTrace || !rayTraceFailed()) {
                        sendAttack();
                    } else if (failSwing) {
                        handleFailSwing();
                    }
                }
            }

            cachedClicks = 0;

            if (killAura.isEnabled()) {
                killAura.postAttack();
            }

            initialized = false;
        } else {
            clickingNow = false;
        }
    }

    public static boolean rayTraceFailed() {
        mc.entityRenderer.getMouseOver(1);
        return mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY;
    }

    private void handleFailSwing() {
        if (blockTimer.hasTimeElapsed(50)) {
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
        float calcYaw = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
        float diffX = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - target.rotationYaw));

        if (PlayerUtils.getCustomDistanceToEntityBox(target.getPositionVector().add(0, target.getEyeHeight(), 0), mc.thePlayer) < 3 && diffX < 90) {
            return true;
        }

        boolean hurtTime = target.hurtTime <= 3;

        if (diffX > 180) {
            hurtTime = target.hurtTime == 0;
        }

        boolean forceOnBacktrack = forceblahblahblah && BackTrack.shouldLag && Demise.INSTANCE.getModuleManager().getModule(BackTrack.class).isEnabled();

        return mc.thePlayer.hurtTime != 0 || hurtTime || forceOnBacktrack;
    }

    private void attack() {
        if (blockTimer.hasTimeElapsed(50)) {
            switch (clickMode) {
                case Legit:
                    if (mc.currentScreen == null) mc.clickMouse();
                    break;
                case Packet:
                    if (ViaLoadingBase.getInstance().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                        mc.thePlayer.swingItem();
                        PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                    } else {
                        PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                        mc.thePlayer.swingItem();
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