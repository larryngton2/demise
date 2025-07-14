package wtf.demise.features.modules.impl.player;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.AngleEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.SimulatedPlayer;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationUtils;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "NoFall", description = "Mitigates or removes fall damage.")
public class NoFall extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Packet", "On ground", "Off ground", "Clutch"}, "Packet", this);
    private final SliderValue fallDistance = new SliderValue("Fall distance", 3, 0, 10, 1, this, () -> !mode.is("Off ground"));
    private final SliderValue predictTicks = new SliderValue("Max predict ticks", 5, 0, 100, 1, this, () -> mode.is("Clutch"));
    private final SliderValue keepTicks = new SliderValue("Keep ticks", 2, 0, 10, 1, this, () -> mode.is("Clutch"));

    private final List<PlayerUtils.PredictProcess> selfPrediction = new ArrayList<>();
    private Vec3 targetPos;
    private int oldSlot;
    private boolean setSlot;
    private final TimerUtils keepSlotTimer = new TimerUtils();
    private boolean clicked;

    @EventTarget
    public void onMotion(MotionEvent e) {
        setTag(mode.get());

        switch (mode.get()) {
            case "Packet":
                if (mc.thePlayer.fallDistance > fallDistance.get()) {
                    PacketUtils.sendPacket(new C03PacketPlayer(true));
                    mc.thePlayer.fallDistance = 0;
                }
                break;
            case "On ground":
                if (mc.thePlayer.fallDistance > fallDistance.get()) {
                    e.setOnGround(true);
                    mc.thePlayer.fallDistance = 0;
                }
                break;
            case "Off ground":
                e.setOnGround(false);
                break;
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (mode.is("Clutch")) {
            if (mc.thePlayer.fallDistance > fallDistance.get()) {
                for (PlayerUtils.PredictProcess predictProcess : selfPrediction) {
                    if (predictProcess.isInWater) {
                        continue;
                    }

                    if (predictProcess.onGround) {
                        targetPos = predictProcess.position;
                        break;
                    }
                }

                mc.thePlayer.inventory.currentItem = getBucketSlot();
                SpoofSlotUtils.startSpoofing(oldSlot);
                setSlot = false;

                if (mc.thePlayer.inventory.getCurrentItem().getItem() == Items.water_bucket) {
                    RotationHandler.setBasicRotation(RotationUtils.getRotations(new BlockPos(targetPos), EnumFacing.UP), true, 180, 180);

                    if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !mc.thePlayer.isInWater()) {
                        if (!clicked) {
                            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                            clicked = true;
                        }
                        keepSlotTimer.reset();
                    }
                }
            } else {
                if (keepSlotTimer.hasTimeElapsed(keepTicks.get() * 50L)) {
                    if (!setSlot) {
                        mc.thePlayer.inventory.currentItem = oldSlot;
                        SpoofSlotUtils.stopSpoofing();
                        setSlot = true;
                    }

                    oldSlot = mc.thePlayer.inventory.currentItem;
                } else {
                    RotationHandler.setBasicRotation(RotationUtils.getRotations(new BlockPos(targetPos), EnumFacing.UP), true, 180, 180);
                }

                clicked = false;
            }
        }
    }

    public int getBucketSlot() {
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack == null) {
                continue;
            }

            if (itemStack.getItem() == Items.water_bucket) {
                return i;
            }
        }

        return mc.thePlayer.inventory.currentItem;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (!mode.is("Clutch")) {
            return;
        }

        selfPrediction.clear();

        SimulatedPlayer simulatedSelf = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput, 1);

        simulatedSelf.rotationYaw = RotationHandler.currentRotation != null ? RotationHandler.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < predictTicks.get(); i++) {
            simulatedSelf.tick();

            PlayerUtils.PredictProcess predictProcess = new PlayerUtils.PredictProcess(
                    simulatedSelf.getPos(),
                    simulatedSelf.fallDistance,
                    simulatedSelf.onGround,
                    simulatedSelf.isCollidedHorizontally,
                    simulatedSelf.isInWater()
            );

            predictProcess.tick = i;

            selfPrediction.add(predictProcess);
        }
    }
}