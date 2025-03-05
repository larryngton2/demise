package wtf.demise.features.modules.impl.player;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.*;
import net.minecraft.util.*;
import org.lwjglx.input.Keyboard;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.movement.Speed;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.notification.NotificationType;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.EnumFacingOffset;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.player.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static wtf.demise.utils.player.PlayerUtils.getBlock;

@ModuleInfo(name = "Scaffold", category = ModuleCategory.Player)
public class Scaffold extends Module {
    private final ModeValue rotations = new ModeValue("Rotations", new String[]{"None", "Normal", "Reverse", "Predicted", "Rounded", "GodBridge", "Snap", "Pitch Abuse"}, "Normal", this);
    private final BoolValue updateOnBlockPlace = new BoolValue("Update on Place", true, this, () -> !rotations.is("None"));
    private final BoolValue randomiseRotationSpeedOnEnable = new BoolValue("Randomise rotation speed on enable", false, this, () -> !rotations.is("None"));
    private final SliderValue rotationSpeed = new SliderValue("Rotation Speed", 55, 1, 180, 1, this, () -> !rotations.is("None"));
    private final SliderValue randomisation = new SliderValue("Randomisation", 1, 0, 6, 0.1f, this, () -> !rotations.is("None"));

    private final ModeValue tower = new ModeValue("Tower", new String[]{"None", "Vanilla", "Slow", "Verus", "NCP"}, "None", this);
    private final BoolValue towerMove = new BoolValue("Tower Move", false, this);

    private final BoolValue movementFix = new BoolValue("Movement fix", true, this);
    private final BoolValue sprint = new BoolValue("Sprint", true, this);
    private final BoolValue safewalk = new BoolValue("Safe walk", true, this);
    private final BoolValue strafe = new BoolValue("Strafe", false, this);
    private final BoolValue sameY = new BoolValue("Same Y", false, this);
    private final SliderValue speedMultiplier = new SliderValue("Speed multi", 1, 0, 2, 0.05f, this);
    private final ModeValue speedMode = new ModeValue("Speed mode", new String[]{"NCP", "None"}, "None", this);

    private final SliderValue range = new SliderValue("Range", 3, 1, 6, 0.5f, this);
    private final SliderValue placeDelay = new SliderValue("Place delay", 0, 0, 5, 0.1f, this);
    private final BoolValue randomisePlaceDelay = new BoolValue("Randomise place delay", false, this);

    private final BoolValue eagle = new BoolValue("Eagle", false, this);
    private final SliderValue eagleBlocks = new SliderValue("Eagle blocks", 4, 0, 15, 1, this, eagle::get);
    private final BoolValue eaglePacket = new BoolValue("Packet Eagle", false, this, eagle::get);

    private final BoolValue telly = new BoolValue("Telly", false, this);
    private final ModeValue tellyMode = new ModeValue("Telly mode", new String[]{"Jump", "Rotate"}, "Jump", this, telly::get);
    private final SliderValue straightTicks = new SliderValue("Straight ticks", 1, 1, 5, 1, this, telly::get);

    public final ModeValue counter = new ModeValue("Counter", new String[]{"Normal", "Exhibition", "Simple", "None"}, "None", this);
    private final ModeValue placeTiming = new ModeValue("Place timing", new String[]{"Legit", "Post"}, "Legit", this);
    private final ModeValue rayCast = new ModeValue("Ray cast", new String[]{"Legit", "Normal", "None"}, "Normal", this);
    private final BoolValue swing = new BoolValue("Swing", true, this);
    private final BoolValue disableOnWorldChange = new BoolValue("Disable on World Change", false, this);
    private final BoolValue disableSpeed = new BoolValue("Disable Speed", false, this);
    private final BoolValue dragClick = new BoolValue("Drag click", false, this);
    private final BoolValue jitter = new BoolValue("Jitter", false, this);
    private final SliderValue expand = new SliderValue("Expand", 0, 0, 5, 0.1f, this);

    public Vec3 targetBlock;
    private List<Vec3> placePossibilities = new ArrayList<>();
    private EnumFacingOffset enumFacing;
    private BlockPos blockFace;
    private int oldSlot;
    private boolean isOnRightSide;

    private float targetYaw, targetPitch, yaw;
    public static float pitch;

    private int ticksOnAir;
    private double startY;
    public int slot;
    private int blocksPlaced;
    private boolean sneaking;
    private boolean shiftPressed;
    private boolean shouldEnableSpeed;

    @Override
    public void onEnable() {
        if (this.getModule(Speed.class).isEnabled() && disableSpeed.get()) {
            shouldEnableSpeed = true;
            this.getModule(Speed.class).setEnabled(false);
            Demise.INSTANCE.getNotificationManager().post(NotificationType.WARNING, "Scaffold", "Disabled Speed to prevent flags");
        } else {
            shouldEnableSpeed = false;
        }

        slot = oldSlot = mc.thePlayer.inventory.currentItem;

        if (rotations.is("Pitch Abuse")) {
            targetYaw = mc.thePlayer.rotationYaw;
            targetPitch = 94;
        } else {
            targetYaw = MoveUtil.getYawFromKeybind() - 180;
            targetPitch = 80;
        }

        startY = mc.thePlayer.posY;
        targetBlock = null;
        placePossibilities.clear();

        if (randomiseRotationSpeedOnEnable.get()) {
            rotationSpeed.setValue((float) (50 + (85 - 50) * Math.random()));
        }
    }

    @EventTarget
    public void onWorldChange(final WorldChangeEvent event) {
        if (disableOnWorldChange.get()) {
            this.toggle();
        }
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        event.setCancelled(safewalk.get());
    }

    @Override
    public void onDisable() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
        blocksPlaced = 0;
        if (shouldEnableSpeed) {
            if (!this.getModule(Speed.class).isEnabled()) {
                this.getModule(Speed.class).setEnabled(true);
                Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "Scaffold", "Enabled Speed since it was previously enabled");
            }
        }

        SpoofSlotUtils.stopSpoofing();
        mc.thePlayer.inventory.currentItem = oldSlot;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);

        if (sneaking) {
            sneaking = false;
            PacketUtils.sendPacket(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
        }

        mc.thePlayer.omniSprint = false;
    }

    @EventTarget
    public void onStrafe(final StrafeEvent e) {
        if (strafe.get()) MoveUtil.strafe();
    }

    @EventTarget
    public void onPreMotion(final MotionEvent event) {
        if (event.isPost()) return;

        double forward = 0, posX = 0, posZ = 0;

        float direction = (float) MoveUtil.getDirection();
        posX = -MathHelper.sin(direction) * forward + mc.thePlayer.posX;
        posZ = MathHelper.cos(direction) * forward + mc.thePlayer.posZ;

        for (forward = 0; forward <= expand.get() && !(getBlock(posX, mc.thePlayer.posY - 1, posZ) instanceof BlockAir); forward += 0.1) {
            forward = expand.get();
            direction = (float) MoveUtil.getDirection();
            posX = -MathHelper.sin(direction) * forward + mc.thePlayer.posX;
            posZ = MathHelper.cos(direction) * forward + mc.thePlayer.posZ;
        }

        if (!MoveUtil.isMoving()) {
            forward = 0;
            posX = -MathHelper.sin(direction) * forward + mc.thePlayer.posX;
            posZ = MathHelper.cos(direction) * forward + mc.thePlayer.posZ;
        }

        if (getBlock(posX, mc.thePlayer.posY - 1, posZ) instanceof BlockAir) ticksOnAir++;
        else ticksOnAir = 0;

        for (int slot = 0; slot < 9; slot++) {
            if (mc.thePlayer.inventory.getStackInSlot(slot) != null && mc.thePlayer.inventory.getStackInSlot(slot).getItem() instanceof ItemBlock) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);

                if (stack.stackSize == 0) mc.thePlayer.inventory.setInventorySlotContents(slot, null);
            }
        }

        if (mc.thePlayer.onGround || (mc.gameSettings.keyBindJump.isKeyDown() && !sameY.get()))
            startY = mc.thePlayer.posY;

        final int blockSlot = BlockUtil.findBlock() - 36;

        if (blockSlot < 0 || blockSlot > 9) return;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), sprint.get());

        switch (speedMode.get()) {
            case "NCP": {
                if (mc.thePlayer.onGround) {
                    if (mc.thePlayer.ticksExisted % 15 == 0) {
                        mc.thePlayer.motionX *= 1.5 + Math.random() / 100D;
                        mc.thePlayer.motionZ *= 1.5 + Math.random() / 100D;
                    } else if (mc.thePlayer.ticksExisted - 2 % 15 == 0) {
                        mc.thePlayer.motionX *= 0.7;
                        mc.thePlayer.motionZ *= 0.7;
                    }
                }
            }
        }

        placePossibilities = getPlacePossibilities();

        if (placePossibilities.isEmpty()) return;

        double finalPosX = posX;
        double finalPosZ = posZ;
        placePossibilities.sort(Comparator.comparingDouble(vec3 -> {
            final double d0 = finalPosX - vec3.xCoord;
            final double d1 = mc.thePlayer.posY - 1 - vec3.yCoord;
            final double d2 = finalPosZ - vec3.zCoord;
            return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);
        }));

        targetBlock = placePossibilities.get(0);

        enumFacing = getEnumFacing(targetBlock);

        if (sameY.get() && mc.thePlayer.posY < startY) startY = mc.thePlayer.posY;

        if (enumFacing == null) return;

        final BlockPos position = new BlockPos(targetBlock.xCoord, targetBlock.yCoord, targetBlock.zCoord);

        blockFace = position.add(enumFacing.getOffset().xCoord, enumFacing.getOffset().yCoord, enumFacing.getOffset().zCoord);

        if (blockFace == null) return;

        if (eagle.get()) {
            shiftPressed = mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)).getBlock() instanceof BlockAir && mc.thePlayer.onGround && blocksPlaced == eagleBlocks.get();

            if (eaglePacket.get()) {
                PacketUtils.sendPacket(new C0BPacketEntityAction(mc.thePlayer, shiftPressed ? C0BPacketEntityAction.Action.START_SNEAKING : C0BPacketEntityAction.Action.STOP_SNEAKING));
            }

            if (blocksPlaced > eagleBlocks.get()) blocksPlaced = 1;
        } else {
            shiftPressed = false;
        }

        yaw = mc.thePlayer.rotationYaw;
        pitch = mc.thePlayer.rotationPitch;

        if (placePossibilities.isEmpty() || targetBlock == null || enumFacing == null || blockFace == null || slot < 0 || slot > 9)
            return;

        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && !this.getModule(Speed.class).isEnabled() && mc.currentScreen == null) {
            if (!tower.is("None")) KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);

            if (!towerMove.get() && !tower.is("None"))
                mc.thePlayer.setPosition(Math.floor(mc.thePlayer.posX) + 0.5, mc.thePlayer.posY, Math.floor(mc.thePlayer.posZ) + 0.5);

            switch (tower.get()) {
                case "Vanilla":
                    mc.thePlayer.motionY = 0.42F;
                    break;
                case "Slow":
                    if (mc.thePlayer.onGround) mc.thePlayer.motionY = 0.4F;
                    else if (PlayerUtils.blockRelativeToPlayer(0, -1, 0) instanceof BlockAir)
                        mc.thePlayer.motionY -= 0.4F;

                    MoveUtil.stopXZ();
                    break;
                case "Verus":
                    if (mc.thePlayer.ticksExisted % 2 == 0) {
                        mc.thePlayer.motionY = 0.42F;
                    }
                    break;
                case "NCP":
                    if (mc.thePlayer.posY % 1 <= 0.00153598) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                        mc.thePlayer.motionY = 0.41998F - getRandomHypixelValues() / 2;
                    } else if (mc.thePlayer.posY % 1 < 0.1 && mc.thePlayer.offGroundTicks != 0) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                    }
                    break;
            }
        }

        mc.thePlayer.omniSprint = movementFix.get() && sprint.get();
    }

    @EventTarget
    public void onMoveButton(final MoveInputEvent event) {
        if (jitter.get()) {
            final BlockPos b = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.5, mc.thePlayer.posZ);
            if (mc.thePlayer.getHorizontalFacing(mc.thePlayer.rotationYaw + 180) == EnumFacing.EAST) {
                if (b.getZ() + 0.5 > mc.thePlayer.posZ) {
                    event.setStrafe(1.0f);
                } else {
                    event.setStrafe(-1.0f);
                }
            } else if (mc.thePlayer.getHorizontalFacing(mc.thePlayer.rotationYaw + 180) == EnumFacing.WEST) {
                if (b.getZ() + 0.5 < mc.thePlayer.posZ) {
                    event.setStrafe(1.0f);
                } else {
                    event.setStrafe(-1.0f);
                }
            } else if (mc.thePlayer.getHorizontalFacing(mc.thePlayer.rotationYaw + 180) == EnumFacing.SOUTH) {
                if (b.getX() + 0.5 < mc.thePlayer.posX) {
                    event.setStrafe(1.0f);
                } else {
                    event.setStrafe(-1.0f);
                }
            } else if (b.getX() + 0.5 > mc.thePlayer.posX) {
                event.setStrafe(1.0f);
            } else {
                event.setStrafe(-1.0f);
            }
        }

        if (telly.get() && MoveUtil.isMoving() && mc.thePlayer.onGround) {
            switch (tellyMode.get()) {
                case "Jump", "Rotate":
                    event.setJumping(true);
                    break;
            }
        }

        if (eagle.get()) {
            if (eaglePacket.get()) return;

            event.setSneaking(shiftPressed);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        mc.thePlayer.motionX *= speedMultiplier.get();
        mc.thePlayer.motionZ *= speedMultiplier.get();

        final float[] rotation = BlockUtil.getDirectionToBlock(blockFace.getX(), blockFace.getY(), blockFace.getZ(), enumFacing.getEnumFacing());

        if ((ticksOnAir > placeDelay.get() && updateOnBlockPlace.get()) || !updateOnBlockPlace.get() || rotations.is("Snap") || rotations.is("None")) {
            switch (rotations.get()) {
                case "Predicted":
                    targetYaw = rotation[0];
                    targetPitch = rotation[1];
                    break;

                case "Rounded": {
                    float movingYaw = MoveUtil.isMoving() ? MoveUtil.getYawFromKeybind() - 180 : mc.thePlayer.rotationYaw - 180;

                    targetYaw = Math.round(movingYaw / 45) * 45;
                    targetPitch = MoveUtil.isMoving() ? 80 : 90;
                    break;
                }

                case "GodBridge": {
                    float movingYaw = MoveUtil.isMoving() ? MoveUtil.getYawFromKeybind() - 180 : mc.thePlayer.rotationYaw - 180;

                    if (mc.thePlayer.onGround) {
                        isOnRightSide = Math.floor(mc.thePlayer.posX + Math.cos(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posX) ||
                                Math.floor(mc.thePlayer.posZ + Math.sin(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posZ);

                        BlockPos posInDirection = mc.thePlayer.getPosition().offset(EnumFacing.fromAngle(movingYaw), 1);

                        boolean isLeaningOffBlock = mc.theWorld.getBlockState(mc.thePlayer.getPosition().down()) instanceof BlockAir;
                        boolean nextBlockIsAir = mc.theWorld.getBlockState(posInDirection.down()).getBlock() instanceof BlockAir;

                        if (isLeaningOffBlock && nextBlockIsAir) {
                            isOnRightSide = !isOnRightSide;
                        }
                    }

                    float yaw = MoveUtil.isMovingStraight() ? (movingYaw + (isOnRightSide ? 45 : -45)) : movingYaw;

                    targetYaw = Math.round(yaw / 45) * 45;
                    targetPitch = MoveUtil.isMoving() ? 75.6f : 90;
                    break;
                }

                case "Normal":
                    targetYaw = (float) (MoveUtil.getYawFromKeybind() + 180 - Math.random() * 10);
                    targetPitch = (float) (80 + Math.random() / 100f);
                    break;

                case "Snap":
                    targetYaw = rotation[0];
                    targetPitch = rotation[1];
                    if (ticksOnAir <= placeDelay.get()) {
                        targetYaw = (float) (MoveUtil.getYawFromKeybind() + Math.random());
                        targetPitch = mc.thePlayer.rotationPitch;
                    }
                    break;

                case "Pitch Abuse":
                    boolean found = false;

                    boolean strict = !rayCast.is("None");

                    for (float yaw = mc.thePlayer.rotationYaw; yaw <= mc.thePlayer.rotationYaw + 360 && !found; yaw += 5) {
                        for (float pitch = 120; pitch < 180 && !found; pitch += 1) {
                            if (BlockUtil.lookingAtBlock(blockFace, yaw, pitch, enumFacing.getEnumFacing(), strict)) {
                                targetYaw = yaw;
                                targetPitch = pitch;
                                found = true;
                            }
                        }
                    }

                    if (!found) {
                        targetYaw = mc.thePlayer.rotationYaw;
                        targetPitch = 94;
                    }
                    break;

                case "None":
                    targetYaw = mc.thePlayer.rotationYaw;
                    targetPitch = mc.thePlayer.rotationPitch;
                    break;

                case "Reverse":
                    targetYaw = MoveUtil.getYawFromKeybind() + 180;
                    targetPitch = 80;
                    break;
            }
        }

        targetPitch += (float) (this.randomisation.get() * (Math.random() - 0.5) * 3);
        targetYaw += (float) (this.randomisation.get() * (Math.random() - 0.5) * 3);

        if (!rotations.is("Pitch Abuse")) targetPitch = MathHelper.clamp_float(targetPitch, -89.9f, 89.9f);

        float[] finalRot = new float[]{targetYaw, targetPitch};

        if (telly.get() && tellyMode.is("Rotate") && mc.thePlayer.offGroundTicks < straightTicks.get()) return;

        RotationUtils.setRotation(finalRot, movementFix.get() ? MovementCorrection.SILENT : MovementCorrection.OFF, rotationSpeed.get(), rotationSpeed.get());
    }

    public static double getRandomHypixelValues() {
        SecureRandom secureRandom = new SecureRandom();
        double value = secureRandom.nextDouble() * (1.0 / System.currentTimeMillis());
        for (int i = 0; i < MathUtils.randomizeDouble(MathUtils.randomizeDouble(4, 6), MathUtils.randomizeDouble(8, 20)); i++)
            value *= (1.0 / System.currentTimeMillis());
        return value;
    }

    public static double[] yawPos(float yaw, double value) {
        return new double[]{-MathHelper.sin(yaw) * value, MathHelper.cos(yaw) * value};
    }

    public void placeBlock() {
        final int blockSlot = BlockUtil.findBlock() - 36;

        if (blockSlot < 0 || blockSlot > 9) return;

        boolean switchedSlot = false;
        if (slot != blockSlot) {
            slot = blockSlot;
            oldSlot = mc.thePlayer.inventory.currentItem;
            SpoofSlotUtils.startSpoofing(mc.thePlayer.inventory.currentItem);
            mc.thePlayer.inventory.currentItem = slot;
            switchedSlot = true;
        }

        if (placePossibilities.isEmpty() || targetBlock == null || enumFacing == null || blockFace == null || slot < 0 || slot > 9)
            return;

        final MovingObjectPosition mov = mc.objectMouseOver;
        if (mov == null) return;

        final boolean sameY = (this.sameY.get() || (this.getModule(Speed.class).isEnabled() && !mc.gameSettings.keyBindJump.isKeyDown())) && MoveUtil.isMoving();
        if (((int) startY - 1 != (int) targetBlock.yCoord && sameY)) return;

        final Vec3 hitVec = mov.hitVec;
        final ItemStack item = mc.thePlayer.inventoryContainer.getSlot(slot + 36).getStack();

        boolean strictCheck = mov.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mov.getBlockPos().equals(blockFace) && mov.sideHit == enumFacing.getEnumFacing();
        boolean normalCheck = mov.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mov.getBlockPos().equals(blockFace);

        if (!switchedSlot && ticksOnAir > placeDelay.get() + (randomisePlaceDelay.get() && !mc.gameSettings.keyBindJump.isKeyDown() ? Math.random() * 3 : 0)) {
            if (rayCast.is("Legit") && !strictCheck) return;
            if (rayCast.is("Normal") && !normalCheck) return;

            if (!BlockUtil.lookingAtBlock(blockFace, yaw, pitch, enumFacing.getEnumFacing(), !rayCast.is("None"))) {
                hitVec.yCoord = Math.random() + blockFace.getY();
                hitVec.zCoord = Math.random() + blockFace.getZ();
                hitVec.xCoord = Math.random() + blockFace.getX();
            }


            if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() > 47) {
                if (swing.get()) mc.thePlayer.swingItem();
                else PacketUtils.sendPacket(new C0APacketAnimation());
            }

            mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, item, blockFace, enumFacing.getEnumFacing(), hitVec);

            if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() <= 47) {
                if (swing.get()) mc.thePlayer.swingItem();
                else PacketUtils.sendPacket(new C0APacketAnimation());
            }

            blocksPlaced++;
        } else if (dragClick.get() && Math.random() > 0.5)
            PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(item));
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent e) {
        e.setBlockRange(range.get());
    }

    @EventTarget
    public void onGameTick(GameEvent e) {
        if (placeTiming.is("Post")) {
            this.placeBlock();
        }
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent e) {
        if (placeTiming.is("Legit")) {
            this.placeBlock();
        }
    }

    private EnumFacingOffset getEnumFacing(final Vec3 position) {
        List<EnumFacingOffset> possibleFacings = new ArrayList<>();
        for (int z2 = -1; z2 <= 1; z2 += 2) {
            if (!(getBlock(position.xCoord, position.yCoord, position.zCoord + z2).isReplaceable(mc.theWorld, new BlockPos(position.xCoord, position.yCoord, position.zCoord + z2)))) {
                if (z2 < 0) {
                    possibleFacings.add(new EnumFacingOffset(EnumFacing.SOUTH, new Vec3(0, 0, z2)));
                } else {
                    possibleFacings.add(new EnumFacingOffset(EnumFacing.NORTH, new Vec3(0, 0, z2)));
                }
            }
        }

        for (int x2 = -1; x2 <= 1; x2 += 2) {
            if (!(getBlock(position.xCoord + x2, position.yCoord, position.zCoord).isReplaceable(mc.theWorld, new BlockPos(position.xCoord + x2, position.yCoord, position.zCoord)))) {
                if (x2 > 0) {
                    possibleFacings.add(new EnumFacingOffset(EnumFacing.WEST, new Vec3(x2, 0, 0)));
                } else {
                    possibleFacings.add(new EnumFacingOffset(EnumFacing.EAST, new Vec3(x2, 0, 0)));
                }
            }
        }

        possibleFacings.sort(Comparator.comparingDouble(enumFacing -> {
            double enumFacingRotations = Math.toDegrees(Math.atan2(enumFacing.getOffset().zCoord,
                    enumFacing.getOffset().xCoord)) % 360;
            double rotations = RotationUtils.currentRotation[0] % 360 + 90;

            return Math.abs(MathUtils.wrappedDifference(enumFacingRotations, rotations));
        }));

        if (!possibleFacings.isEmpty()) return possibleFacings.get(0);

        for (int y2 = -1; y2 <= 1; y2 += 2) {
            if (!(getBlock(position.xCoord, position.yCoord + y2, position.zCoord).isReplaceable(mc.theWorld, new BlockPos(position.xCoord, position.yCoord + y2, position.zCoord)))) {
                if (y2 < 0) {
                    return new EnumFacingOffset(EnumFacing.UP, new Vec3(0, y2, 0));
                }
            }
        }

        return null;
    }

    private List<Vec3> getPlacePossibilities() {
        final List<Vec3> possibilities = new ArrayList<>();
        final int range = (int) Math.ceil(this.range.get() + this.expand.get() + 1);

        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = PlayerUtils.blockRelativeToPlayer(x, y, z);

                    if (!(block instanceof BlockAir)) {
                        for (int x2 = -1; x2 <= 1; x2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x + x2, mc.thePlayer.posY + y, mc.thePlayer.posZ + z));

                        for (int y2 = -1; y2 <= 1; y2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y + y2, mc.thePlayer.posZ + z));

                        for (int z2 = -1; z2 <= 1; z2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z + z2));
                    }
                }
            }
        }

        possibilities.removeIf(vec3 -> !(getBlock(vec3.xCoord, vec3.yCoord, vec3.zCoord) instanceof BlockAir) || (mc.thePlayer.posX == vec3.xCoord && mc.thePlayer.posY + 1 == vec3.yCoord && mc.thePlayer.posZ == vec3.zCoord));

        return possibilities;
    }

    public int getBlockCount() {
        int blockCount = 0;

        for (int i = 36; i < 45; ++i) {
            if (!mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) continue;

            final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if (!(is.getItem() instanceof ItemBlock && !BlockUtil.BLOCK_BLACKLIST.contains(((ItemBlock) is.getItem()).getBlock()))) {
                continue;
            }

            blockCount += is.stackSize;
        }

        return blockCount;
    }
}