package wtf.demise.utils.player.clicking;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.StaticTickEvent;
import wtf.demise.events.impl.misc.TickEvent;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.modules.impl.legit.BackTrack;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.PlayerUtils;

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
    public static boolean clickingNow;
    private int cachedClicks;
    private static boolean cooldown;
    private int lastHitTicks;

    public enum ClickMode {
        Legit,
        Packet,
        PlayerController
    }

    public static void initHandler(float minCPS, float maxCPS, boolean rayTrace, boolean smartClicking, boolean forceClickOnBackTrack, boolean ignoreBlocking, boolean failSwing, boolean cooldown, float attackRange, float swingRange, ClickMode clickMode, EntityLivingBase target) {
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
        ClickHandler.cooldown = cooldown;

        initialized = true;
    }

    private void generateClickPattern() {
        clickPattern.clear();
        double cps = MathUtils.randomizeDouble(minCPS, maxCPS);

        int totalTicks = 20;
        double clicksToDistribute = cps;

        for (int i = 0; i < totalTicks; i++) {
            double probability = clicksToDistribute / (totalTicks - i);

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
    public void onMotion(MotionEvent e) {
        if (e.isPre() && cooldown) {
            lastHitTicks++;
        }
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
    }

    /*
    @EventTarget
    public void onRender2D(Render2DEvent e) {
        StringBuilder str = new StringBuilder();

        for (Integer i : clickPattern) {
            str.append(i);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        Fonts.interRegular.get(20).drawCenteredStringWithShadow(str.toString(), sr.getScaledWidth() / 2f, (sr.getScaledHeight() / 2f) + 10, -1);
    }
    */

    @EventTarget
    public void onTickEvent(TickEvent e) {
        clickingNow = false;
        finalizeHandler();
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

            float cooldown;

            if (ClickHandler.cooldown) {
                cooldown = 4;

                if (mc.thePlayer.getHeldItem() != null) {
                    Item item = mc.thePlayer.getHeldItem().getItem();

                    if (item instanceof ItemSpade || item == Items.golden_axe || item == Items.diamond_axe || item == Items.wooden_hoe || item == Items.golden_hoe)
                        cooldown = 20;

                    if (item == Items.wooden_axe || item == Items.stone_axe)
                        cooldown = 25;

                    if (item instanceof ItemSword)
                        cooldown = 12;

                    if (item instanceof ItemPickaxe)
                        cooldown = 17;

                    if (item == Items.iron_axe)
                        cooldown = 22;

                    if (item == Items.stone_hoe)
                        cooldown = 10;

                    if (item == Items.iron_hoe)
                        cooldown = 7;
                }

                cooldown *= Math.max(1, mc.timer.timerSpeed);

                if (lastHitTicks <= cooldown || mc.thePlayer.fallDistance < 0) {
                    cachedClicks = 0;
                    initialized = false;
                    clickingNow = false;
                    return;
                }
            }

            for (int i = 0; i < cachedClicks; i++) {
                lastTargetTime.reset();

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

                lastHitTicks = 0;
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
        if (!mc.thePlayer.isBlocking() || ignoreBlocking) {
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

        if (PlayerUtils.getDistToTargetFromMouseOver(target.getPositionEyes(1), target.getLook(1), mc.thePlayer, mc.thePlayer.getHitbox()) < 3) {
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
        if (!mc.thePlayer.isBlocking() || ignoreBlocking) {
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