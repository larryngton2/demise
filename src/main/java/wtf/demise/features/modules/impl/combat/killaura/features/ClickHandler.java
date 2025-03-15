package wtf.demise.features.modules.impl.combat.killaura.features;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.Demise;
import wtf.demise.events.impl.player.AttackEvent;
import wtf.demise.features.modules.impl.combat.killaura.KillAura;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.PlayerUtils;

import java.util.concurrent.ThreadLocalRandom;

import static wtf.demise.features.modules.impl.combat.killaura.KillAura.currentTarget;
import static wtf.demise.utils.packet.PacketUtils.sendPacketNoEvent;

public class ClickHandler implements InstanceAccess {
    private static final KillAura killAura = Demise.INSTANCE.getModuleManager().getModule(KillAura.class);
    public static final TimerUtils lastTargetTime = new TimerUtils();

    public static void sendAttack() {
        if (!isWithinAttackRange()) {
            return;
        }

        if (!isAttackReady()) {
            return;
        }

        boolean rayCastFailed = mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY;

        if (killAura.rayCast.get() && rayCastFailed) {
            handleFailSwing();
        } else {
            attack();
        }

        lastTargetTime.reset();
    }

    public static boolean isWithinAttackRange() {
        return PlayerUtils.getDistanceToEntityBox(currentTarget) <= killAura.attackRange.get();
    }

    public static boolean isAttackReady() {
        boolean check = lastTargetTime.hasTimeElapsed(1000 / (ThreadLocalRandom.current().nextInt((int) killAura.minCPS.get(), (int) killAura.maxCPS.get() + 1) * 1.5));

        if (killAura.smartClicking.get() && check) {
            return shouldClick();
        } else {
            return check;
        }
    }

    public static void handleFailSwing() {
        MovingObjectPosition mov = mc.objectMouseOver;

        if (killAura.failSwing.get() && killAura.failSwing.canDisplay()) {
            switch (killAura.clickMode.get()) {
                case "Packet":
                    switch (killAura.swingMode.get()) {
                        case "Normal", "Client":
                            AttackOrder.sendConditionalSwing(mov);
                            break;
                        case "Server":
                            sendPacketNoEvent(new C0APacketAnimation());
                            break;
                    }
                    break;
                case "Legit":
                    mc.clickMouse();
                    break;
                case "PlayerController":
                    AttackOrder.sendConditionalSwing(mov);
                    break;
            }
        }
    }

    private static boolean shouldClick() {
        return mc.thePlayer.hurtTime >= 5 || mc.thePlayer.motionY >= 0 || currentTarget.hurtTime <= 5;
    }

    private static void attack() {
        int attackCount = 1;

        if (killAura.extraClicks.get() && killAura.rand.nextInt(100) <= killAura.eChance.get()) {
            attackCount = (int) killAura.clicks.get() + 1;
        }

        for (int i = 0; i < attackCount; i++) {
            switch (killAura.clickMode.get()) {
                case "Legit":
                    mc.clickMouse();
                    break;
                case "Packet":
                    if (ViaLoadingBase.getInstance().getTargetVersion().isOlderThanOrEqualTo(ProtocolVersion.v1_8)) {
                        switch (killAura.swingMode.get()) {
                            case "Normal", "Client":
                                mc.thePlayer.swingItem();
                                break;
                            case "Server":
                                sendPacketNoEvent(new C0APacketAnimation());
                                break;
                        }
                        PacketUtils.sendPacket(new C02PacketUseEntity(currentTarget, C02PacketUseEntity.Action.ATTACK));
                    } else {
                        PacketUtils.sendPacket(new C02PacketUseEntity(currentTarget, C02PacketUseEntity.Action.ATTACK));
                        switch (killAura.swingMode.get()) {
                            case "Normal", "Client":
                                mc.thePlayer.swingItem();
                                break;
                            case "Server":
                                sendPacketNoEvent(new C0APacketAnimation());
                                break;
                        }
                    }

                    Demise.INSTANCE.getEventManager().call(new AttackEvent(currentTarget));
                    break;
                case "PlayerController":
                    AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
                    break;
            }
        }
    }
}
