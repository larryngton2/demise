package wtf.demise.features.modules.impl.combat.killaura.features;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.combat.killaura.KillAura;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.player.PlayerUtils;

import static wtf.demise.features.modules.impl.combat.killaura.KillAura.currentTarget;
import static wtf.demise.features.modules.impl.combat.killaura.KillAura.isBlocking;
import static wtf.demise.utils.packet.PacketUtils.sendPacket;

public class AutoBlockHandler implements InstanceAccess {
    private static final KillAura killAura = Demise.INSTANCE.getModuleManager().getModule(KillAura.class);
    private static int blockTicks = 0;

    public static void preAttack() {
        if (extraCheck() && canAutoBlock()) {
            switch (killAura.autoBlockMode.get()) {
                case "None":
                    if (isBlocking) {
                        setBlocking(false);
                    }
                    break;
                case "Fake", "NCP":
                    isBlocking = true;
                    break;
                case "Vanilla":
                    setBlocking(true);
                    break;
                case "Release":
                   setBlocking(false);
                    break;
                case "VanillaForce":
                    vanillaReblockAb(currentTarget);
                    break;
                case "Smart":
                    smartAb(currentTarget);
                    break;
                case "Blink":
                    blinkAb(currentTarget);
                    break;
            }
        } else if (isBlocking) {
            setBlocking(false);
        }
    }

    public static void onAttack() {
        if (extraCheck() && canAutoBlock()) {
            switch (killAura.autoBlockMode.get()) {
                case "NCP":
                    setBlocking(true);
                    break;
            }
        } else if (isBlocking) {
            setBlocking(false);
        }
    }

    public static void postAttack() {
        if (extraCheck() && canAutoBlock()) {
            switch (killAura.autoBlockMode.get()) {
                case "Release":
                    setBlocking(true);
                    break;
                case "VanillaForce":
                    vanillaReblockAb(currentTarget);
                    break;
            }
        } else if (isBlocking) {
            setBlocking(false);
        }
    }

    private static boolean extraCheck() {
        boolean rayCastFailed = mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY;

        return !rayCastFailed || !killAura.unBlockOnRayCastFail.get() || !killAura.rayCast.get();
    }

    private static void vanillaReblockAb(Entity e) {
        setBlocking(true);
        if (!mc.gameSettings.keyBindUseItem.isKeyDown() || !mc.thePlayer.isBlocking()) {
            setBlocking(true);
        }
    }

    private static void smartAb(EntityLivingBase e) {
        setBlocking(((mc.thePlayer.hurtTime <= 5 && mc.thePlayer.hurtTime != 0) && mc.thePlayer.motionY >= 0) || e.hurtTime > 5);
    }

    private static void blinkAb(Entity e) {
        if (blockTicks >= 3) {
            blockTicks = 0;
        } else {
            blockTicks++;
        }

        switch (blockTicks) {
            case 1:
                BlinkComponent.blinking = true;
                setBlocking(false);
                break;
            case 2:
                setBlocking(true);
                BlinkComponent.dispatch(true);
                break;
        }
    }

    public static void setBlocking(boolean state) {
        if (!killAura.autoBlockMode.is("NCP")) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), state);
        } else {
            if (state) {
                sendPacket(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 255, null, 0.0f, 0.0f, 0.0f));
            } else {
                sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            }
        }

        isBlocking = state;
    }

    public static boolean canAutoBlock() {
        return killAura.isHoldingSword() && killAura.autoBlock.get() && PlayerUtils.getDistanceToEntityBox(currentTarget) <= killAura.autoBlockRange.get();
    }
}
