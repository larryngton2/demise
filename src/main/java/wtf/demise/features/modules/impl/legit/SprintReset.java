package wtf.demise.features.modules.impl.legit;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "SprintReset", description = "Makes you deal increased knockback to targets.", category = ModuleCategory.Legit)
public class SprintReset extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"ReSprint", "WTap", "Sneak", "Block", "Packet", "LessPacket"}, "ReSprint", this);
    private final BoolValue fast = new BoolValue("Fast", false, this, () -> mode.is("ReSprint"));
    private final SliderValue reSprintTime = new SliderValue("ReSprint time", 50, 1, 300, 1, this, () -> mode.is("WTap") || mode.is("Block") || mode.is("Sneak"));
    private final ModeValue fallbackMode = new ModeValue("Fallback mode", new String[]{"ReSprint", "WTap", "Packet", "LessPacket"}, "WTap", this, () -> mode.is("Block"));
    private final BoolValue diffCheck = new BoolValue("Angle diff check", false, this);
    private final BoolValue notWhileHurt = new BoolValue("Not while hurt", false, this);

    private final TimerUtils timer = new TimerUtils();
    private boolean isBlocking;
    private EntityLivingBase target;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());

        target = PlayerUtils.getTarget(8, false);

        if (target == null) {
            return;
        }

        float calcYaw = (float) (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0);
        float diffX = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - target.rotationYawHead));

        if ((diffCheck.get() && diffX > 120) || (notWhileHurt.get() && mc.thePlayer.hurtTime != 0)) {
            return;
        }

        if (target.hurtTime == 10) {
            switch (mode.get()) {
                case "WTap", "Sneak":
                    timer.reset();
                    break;
                case "Block":
                    if (PlayerUtils.isHoldingSword()) {
                        timer.reset();
                    } else {
                        reset(true);
                    }
                    break;
            }

            if (!mode.is("WTap") && !mode.is("Block") && !mode.is("Sneak")) {
                reset(false);
            }
        }
    }

    private void reset(boolean fallback) {
        switch (fallback ? fallbackMode.get() : mode.get()) {
            case "ReSprint":
                if (!fast.get()) {
                    mc.thePlayer.reSprint = 2;
                } else {
                    mc.thePlayer.sprintingTicksLeft = 0;
                }
                break;
            case "WTap", "Sneak":
                timer.reset();
                break;
            case "Packet":
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.serverSprintState = true;
                break;
            case "LessPacket":
                if (mc.thePlayer.isSprinting()) {
                    mc.thePlayer.setSprinting(false);
                }
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.serverSprintState = true;
                break;
        }
    }

    @EventTarget
    public void onGameEvent(GameEvent e) {
        if (mode.is("Block")) {
            if (target != null) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), !timer.hasTimeElapsed(reSprintTime.get()));
                isBlocking = !timer.hasTimeElapsed(reSprintTime.get());
            } else if (isBlocking) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (!timer.hasTimeElapsed(reSprintTime.get())) {
            if (mode.is("WTap") || (mode.is("Block") && fallbackMode.is("WTap"))) {
                e.setForward(0);
            }

            if (mode.is("Sneak") || (mode.is("Block") && fallbackMode.is("Sneak"))) {
                e.setSneaking(true);
            }
        }
    }
}