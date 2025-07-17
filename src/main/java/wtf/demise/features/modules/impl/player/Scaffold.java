package wtf.demise.features.modules.impl.player;

import lombok.AllArgsConstructor;
import net.minecraft.block.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.events.impl.render.ShaderEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.modules.impl.movement.Speed;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.misc.SpoofSlotUtils;
import wtf.demise.utils.player.InventoryUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks bellow you.")
public class Scaffold extends Module {
    public final ModeValue mode = new ModeValue("Mode", new String[]{"Normal", "Breezily", "Telly"}, "Normal", this);
    private final SliderValue straightTellyTicks = new SliderValue("Straight telly ticks", 4, 1, 5, this, () -> mode.is("Telly"));
    private final SliderValue diagonalTellyTicks = new SliderValue("Diagonal telly ticks", 2, 1, 5, this, () -> mode.is("Telly"));
    private final ModeValue rotations = new ModeValue("Rotations", new String[]{"Normal", "Center", "GodBridge", "Reverse"}, "Normal", this, () -> !mode.is("Breezily"));
    private final BoolValue staticify = new BoolValue("Static-ify", false, this, () -> !rotations.is("Normal") && !rotations.is("Center") && rotations.canDisplay());
    private final BoolValue clutch = new BoolValue("Clutch", false, this);
    private final ModeValue clutchCriteria = new ModeValue("Clutch criteria", new String[]{"MouseOver", "Prediction"}, "MouseOver", this, clutch::get);
    private final ModeValue clutchRotMode = new ModeValue("Clutch rotation mode", new String[]{"Normal", "Center"}, "Center", this, clutch::get);
    private final MultiBoolValue instantRotsCriteria = new MultiBoolValue("Instant rot criteria", Arrays.asList(
            new BoolValue("Always", false),
            new BoolValue("On miss", false),
            new BoolValue("Predict", false)
    ), this, clutch::get);
    private final BoolValue autoSneakOnClutch = new BoolValue("Auto sneak on clutch", false, this, clutch::get);
    private final SliderValue groundTicksToSneak = new SliderValue("Ground ticks to sneak", 5, 0, 10, this, () -> clutch.get() && autoSneakOnClutch.get());
    private final RotationManager rotationManager = new RotationManager(this);
    public final ModeValue sprintMode = new ModeValue("Sprint mode", new String[]{"Normal", "Silent", "Ground", "Air", "None"}, "Normal", this);
    private final ModeValue speedMode = new ModeValue("Speed mode", new String[]{"Tick", "Exempt", "Diagonal", "None"}, "None", this);
    private final ModeValue rayTraceMode = new ModeValue("Raytrace mode", new String[]{"None", "Normal", "Strict"}, "None", this);
    private final SliderValue maxPlaceRange = new SliderValue("Max place range", 4.5f, 1, 12, this, () -> rayTraceMode.is("None"));
    private final MultiBoolValue addons = new MultiBoolValue("Addons", Arrays.asList(
            new BoolValue("Swing", true),
            new BoolValue("KeepY", false),
            new BoolValue("Speed keepY", false),
            new BoolValue("Jump", false),
            new BoolValue("Safewalk", false),
            new BoolValue("Safewalk when no data", false),
            new BoolValue("Hover", false),
            new BoolValue("Sneak", false),
            new BoolValue("Target block ESP", false)
    ), this);
    private final SliderValue blockToJump = new SliderValue("Blocks to jump", 6, 0, 15, this, () -> addons.isEnabled("Jump"));
    private final BoolValue onlyStraight = new BoolValue("Only straight", true, this, () -> addons.isEnabled("Jump"));
    private final SliderValue blocksToSneak = new SliderValue("Blocks to sneak", 7, 0, 15, this, () -> addons.isEnabled("Sneak"));
    private final BoolValue onlySneakOnGround = new BoolValue("Only sneak on ground", true, this, () -> addons.isEnabled("Sneak"));
    private final ModeValue tower = new ModeValue("Tower", new String[]{"Jump", "Vanilla", "PullDown", "NCP"}, "Jump", this, () -> mode.is("Normal"));
    private final ModeValue towerMove = new ModeValue("Tower move", new String[]{"Jump", "Vanilla", "PullDown", "NCP"}, "Jump", this, () -> mode.is("Normal"));
    private final SliderValue pullDownMotion = new SliderValue("PullDown motion", 0.95f, 0.5f, 1, 0.01f, this, () -> towerMove.is("PullDown") && towerMove.canDisplay());

    public Scaffold() {
        staticify.setDescription("Makes your pitch static when enabled.");
        rayTraceMode.setDescription("None: places blocks bellow you without any checks, Normal: checks if you are aiming at the target block before placing, Strict: checks if you are aiming at the target block and at the correct face before placing");
    }

    public BlockPos targetBlock;
    private double onGroundY;
    private int oldSlot = 0;
    private int blocksPlacedSneak, blocksPlacedJump;
    private int tellyTicks;
    private PlaceData data;
    private boolean isOnRightSide;
    private float yaw, pitch;
    private float initialYaw, initialPitch;
    private boolean clutching;
    private boolean startClutch;
    private final TimerUtils clutchTimer = new TimerUtils();
    private final TimerUtils sneakTimer = new TimerUtils();
    private boolean shaderCalled;
    private final TimerUtils shitCodeTimer = new TimerUtils();

    private static final EnumFacing[] FACINGS = {
            EnumFacing.EAST, EnumFacing.WEST,
            EnumFacing.NORTH, EnumFacing.SOUTH,
            EnumFacing.UP
    };

    private static final BlockPos[] OFFSETS = {
            new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(0, -1, 0)
    };

    @Override
    public void onEnable() {
        oldSlot = mc.thePlayer.inventory.currentItem;
        onGroundY = mc.thePlayer.getEntityBoundingBox().minY;
    }

    @Override
    public void onDisable() {
        mc.thePlayer.inventory.currentItem = oldSlot;
        SpoofSlotUtils.stopSpoofing();

        blocksPlacedJump = 0;
        blocksPlacedSneak = 0;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        mc.thePlayer.inventory.currentItem = getBlockSlot();
        SpoofSlotUtils.startSpoofing(oldSlot);

        data = null;

        if (mc.thePlayer.onGround) {
            onGroundY = mc.thePlayer.getEntityBoundingBox().minY;
        }

        double posY = mc.thePlayer.getEntityBoundingBox().minY;

        if ((addons.isEnabled("KeepY") || addons.isEnabled("Speed keepY") && isEnabled(Speed.class) && !mc.gameSettings.keyBindJump.isKeyDown())) {
            posY = onGroundY;
        }

        if (towerMoving() || towering()) {
            onGroundY = posY = mc.thePlayer.getEntityBoundingBox().minY;
        }

        targetBlock = new BlockPos(mc.thePlayer.posX, posY - 1, mc.thePlayer.posZ);

        data = findBlock(targetBlock);

        if (mode.is("Telly")) {
            tellyTicks = (int) (MoveUtil.isMovingStraight() ? straightTellyTicks.get() : diagonalTellyTicks.get());
        }

        if (isEnabled(KillAura.class) && KillAura.currentTarget != null)
            return;

        switch (sprintMode.get()) {
            case "Normal", "Silent":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                break;
            case "Ground":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), mc.thePlayer.onGround);

                if (!mc.thePlayer.onGround) {
                    mc.thePlayer.setSprinting(false);
                }
                break;
            case "Air":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), !mc.thePlayer.onGround);

                if (mc.thePlayer.onGround) {
                    mc.thePlayer.setSprinting(false);
                }
                break;
            case "None":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
                mc.thePlayer.setSprinting(false);
                break;
        }

        if (tower.canDisplay() && (!tower.is("Jump") && towering() || !towerMove.is("Jump") && towerMoving())) {
            blocksPlacedJump = 0;
            blocksPlacedSneak = 0;
        }

        mc.entityRenderer.getMouseOver(1);

        boolean rotateCheck = mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mc.objectMouseOver.getBlockPos().equalsBlockPos(data.blockPos.offset(data.facing));

        if (rotateCheck) {
            switch (mode.get()) {
                case "Normal", "Telly":
                    switch (rotations.get()) {
                        case "Normal":
                            initialYaw = getNearestRotation(RotationHandler.currentRotation)[0];
                            initialPitch = getNearestRotation(RotationHandler.currentRotation)[1];
                            break;
                        case "Center":
                            Vec3 hitVec = RotationUtils.getVec3(data.blockPos, data.facing);

                            initialYaw = RotationUtils.getRotations(hitVec)[0];
                            initialPitch = RotationUtils.getRotations(hitVec)[1];
                            break;
                        case "GodBridge":
                            float movingYaw = MoveUtil.isMoving() ? MoveUtil.getYawFromKeybind() - 180 : mc.thePlayer.rotationYaw - 180;

                            if (mc.thePlayer.onGround) {
                                isOnRightSide = Math.floor(mc.thePlayer.posX + Math.cos(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posX) ||
                                        Math.floor(mc.thePlayer.posZ + Math.sin(Math.toRadians(movingYaw)) * 0.5) != Math.floor(mc.thePlayer.posZ);
                            }

                            float yaw = MoveUtil.isMovingStraight() ? (movingYaw + (isOnRightSide ? 45 : -45)) : movingYaw;

                            initialYaw = Math.round(yaw / 45) * 45;
                            initialPitch = staticify.get() ? MoveUtil.isMoving() ? 75.7f : 90f : getNearestRotation(RotationHandler.currentRotation)[1];
                            break;
                        case "Reverse":
                            initialYaw = MoveUtil.getYawFromKeybind() - 180;
                            initialPitch = staticify.get() ? 80 : getNearestRotation(RotationHandler.currentRotation)[1];
                            break;
                    }
                    break;
                case "Breezily": {
                    float yaw = MoveUtil.getYawFromKeybind() - 180;
                    initialYaw = Math.round(yaw / 45) * 45;
                    initialPitch = MoveUtil.isMoving() ? MoveUtil.isMovingStraight() ? 80 : 75.7f : 90;
                }
                break;
            }

            float[] rotation = new float[]{initialYaw, initialPitch};

            if (clutch.get()) {
                switch (clutchCriteria.get()) {
                    case "MouseOver":
                        MovingObjectPosition ray = PlayerUtils.rayTraceBlock(rotation, 4.5, 1);

                        if (ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !MoveUtil.isMoving() || (ray.getBlockPos() != null && ray.getBlockPos().distanceSq(data.blockPos) > 1)) {
                            setClutchRot();
                            clutching = true;
                        } else {
                            clutching = false;
                        }

                        break;
                    case "Prediction":
                        boolean isLeaningOffBlock = PlayerUtils.getBlock(new BlockPos(mc.thePlayer.getPositionVector().subtract(0, 1, 0)).offset(mc.thePlayer.getHorizontalFacing(initialYaw).getOpposite())) instanceof BlockAir;
                        boolean nextBlockIsAir = mc.theWorld.getBlockState(mc.thePlayer.getPosition().offset(EnumFacing.fromAngle(initialYaw), 1).down()).getBlock() instanceof BlockAir;

                        if ((isLeaningOffBlock && nextBlockIsAir) || !MoveUtil.isMoving()) {
                            startClutch = true;
                        } else if (startClutch) {
                            clutchTimer.reset();
                            startClutch = false;
                        }

                        clutching = startClutch || !clutchTimer.hasTimeElapsed(200);

                        if (clutching) {
                            setClutchRot();
                        }
                        break;
                }
            } else {
                clutching = false;
            }
        }

        if (!mode.is("Telly") || mode.is("Telly") && mc.thePlayer.offGroundTicks >= tellyTicks) {
            rotationManager.setRotation(new float[]{clutching ? yaw : initialYaw, clutching ? pitch : initialPitch});
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
            switch (speedMode.get()) {
                case "Tick":
                    if (mc.thePlayer.onGround) {
                        if (mc.thePlayer.ticksExisted % 15 == 0) {
                            mc.thePlayer.motionX *= 1.5 + Math.random() / 100D;
                            mc.thePlayer.motionZ *= 1.5 + Math.random() / 100D;
                        } else if (mc.thePlayer.ticksExisted - 2 % 15 == 0) {
                            mc.thePlayer.motionX *= 0.7;
                            mc.thePlayer.motionZ *= 0.7;
                        }
                    }
                    break;
                case "Exempt":
                    if (mc.thePlayer.onGround) {
                        MoveUtil.strafe(mc.thePlayer.movementInput.moveStrafe == 0 ? 0.1591f : 0.1565f);
                    }
                    break;
                case "Diagonal":
                    MoveUtil.useDiagonalSpeed();
                    break;
            }
        }
    }

    private void setClutchRot() {
        Vec3 hitVec = RotationUtils.getVec3(data.blockPos, data.facing);

        switch (clutchRotMode.get()) {
            case "Normal": {
                float[] currentRotation = new float[]{initialYaw, initialPitch};
                this.yaw = getNearestRotation(currentRotation)[0];
                this.pitch = getNearestRotation(currentRotation)[1];
            }
            break;
            case "Center": {
                this.yaw = RotationUtils.getRotations(hitVec)[0];
                this.pitch = RotationUtils.getRotations(hitVec)[1];
            }
            break;
        }

        boolean isLeaningOffBlock = PlayerUtils.getBlock(data.blockPos.offset(data.facing)) instanceof BlockAir;
        boolean nextBlockIsAir = mc.theWorld.getBlockState(mc.thePlayer.getPosition().offset(EnumFacing.fromAngle(initialYaw), 1).down()).getBlock() instanceof BlockAir;

        boolean instantRotation = instantRotsCriteria.isEnabled("Predict") && isLeaningOffBlock && nextBlockIsAir;

        if (instantRotsCriteria.isEnabled("Always")) {
            instantRotation = true;
        }

        if (instantRotsCriteria.isEnabled("On miss")) {
            if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mc.objectMouseOver.getBlockPos().equalsBlockPos(data.blockPos)) {
                instantRotation = true;
            }
        }

        if (instantRotation) {
            rotationManager.setRandYawSpeed(180);
            rotationManager.setRandPitchSpeed(180);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        setTag(mode.get());
        rotationManager.updateRotSpeed(e);

        if (!mode.is("Telly") || mode.is("Telly") && mc.thePlayer.offGroundTicks >= tellyTicks) {
            place(data.blockPos, data.facing, RotationUtils.getVec3(data.blockPos, data.facing));
        }
    }

    @EventTarget
    public void onSafeWalk(SafeWalkEvent event) {
        if (addons.isEnabled("Safewalk") && mc.thePlayer.onGround || addons.isEnabled("Safewalk when no data") && data == null) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onMovementInput(MoveInputEvent e) {
        if (mode.is("Breezily")) {
            boolean isLeaningOffBlock = PlayerUtils.getBlock(data.blockPos.offset(data.facing)) instanceof BlockAir;
            BlockPos b = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
            if (isLeaningOffBlock && MoveUtil.isMoving() && MoveUtil.isMovingStraight()) {
                switch (mc.thePlayer.getHorizontalFacing(initialYaw).getOpposite()) {
                    case EAST -> {
                        if (b.getZ() + 0.5 > mc.thePlayer.posZ) {
                            e.setStrafe(1);
                        } else {
                            e.setStrafe(-1);
                        }
                    }
                    case WEST -> {
                        if (b.getZ() + 0.5 < mc.thePlayer.posZ) {
                            e.setStrafe(1);
                        } else {
                            e.setStrafe(-1);
                        }
                    }
                    case SOUTH -> {
                        if (b.getX() + 0.5 < mc.thePlayer.posX) {
                            e.setStrafe(1);
                        } else {
                            e.setStrafe(-1);
                        }
                    }
                    case NORTH -> {
                        if (b.getX() + 0.5 > mc.thePlayer.posX) {
                            e.setStrafe(1);
                        } else {
                            e.setStrafe(-1);
                        }
                    }
                }
            }
        }

        if (addons.isEnabled("Jump")) {
            if (MoveUtil.isMoving()) {
                if (blocksPlacedJump > blockToJump.get() || !mc.thePlayer.onGround || (onlyStraight.get() && !MoveUtil.isMovingStraight())) {
                    blocksPlacedJump = 0;
                }

                if (blocksPlacedJump == blockToJump.get()) {
                    e.setJumping(true);
                }
            }
        }

        if (clutch.get()) {
            if (clutching && autoSneakOnClutch.get() && mc.thePlayer.onGroundTicks >= groundTicksToSneak.get() && MoveUtil.isMoving()) {
                sneakTimer.reset();
            }

            if (!sneakTimer.hasTimeElapsed(100)) {
                e.setSneaking(true);
            }
        }

        if (addons.isEnabled("Sneak")) {
            if (!mc.thePlayer.onGround && onlySneakOnGround.get()) {
                return;
            }

            if (blocksPlacedSneak > blocksToSneak.get()) {
                blocksPlacedSneak = 0;
            }

            if (blocksPlacedSneak == blocksToSneak.get()) {
                e.setSneaking(true);
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent e) {
        if (mode.is("Telly") && mc.thePlayer.onGround && !towering() && !towerMoving() && (!isEnabled(Speed.class)) && mc.thePlayer.isSprinting() && MoveUtil.isMoving()) {
            mc.thePlayer.jump();
        }
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        if (tower.canDisplay()) {
            if (tower.is("Vanilla")) {
                if (!mc.thePlayer.isPotionActive(Potion.jump)) {
                    if (towering()) {
                        event.setY(mc.thePlayer.motionY = 0.42);
                    }
                }
            }
        }

        if (towerMove.canDisplay()) {
            if (towerMove.is("Vanilla")) {
                if (MoveUtil.isMoving() && MoveUtil.getSpeed() > 0.1 && !mc.thePlayer.isPotionActive(Potion.jump)) {
                    if (towerMoving()) {
                        mc.thePlayer.motionY = 0.42f;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPreMotion(MotionEvent event) {
        if (event.isPost())
            return;

        if (tower.canDisplay()) {
            if (tower.is("NCP") && towering()) {
                sendPacketNoEvent(new C08PacketPlayerBlockPlacement(null));

                if (mc.thePlayer.posY % 1 <= 0.00153598) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                    mc.thePlayer.motionY = 0.42F;
                } else if (mc.thePlayer.posY % 1 < 0.1 && mc.thePlayer.offGroundTicks != 0) {
                    mc.thePlayer.motionY = 0;
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                }
            }
        }

        if (towerMove.canDisplay()) {
            if (towerMove.is("NCP") && towerMoving()) {
                sendPacketNoEvent(new C08PacketPlayerBlockPlacement(null));

                if (mc.thePlayer.posY % 1 <= 0.00153598) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                    mc.thePlayer.motionY = 0.42F;
                } else if (mc.thePlayer.posY % 1 < 0.1 && mc.thePlayer.offGroundTicks != 0) {
                    mc.thePlayer.motionY = 0;
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                }
            }
        }

        if ((towerMove.canDisplay() && towerMoving() && towerMove.is("PullDown")) || (tower.canDisplay() && towering() && tower.is("PullDown"))) {
            if (mc.thePlayer.onGround) mc.thePlayer.motionY = 0.42F;
            mc.thePlayer.motionX *= pullDownMotion.get();
            mc.thePlayer.motionZ *= pullDownMotion.get();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getState() == PacketEvent.State.OUTGOING) {
            if ((towerMove.canDisplay() && towerMoving() && towerMove.is("PullDown")) || (tower.canDisplay() && towering() && tower.is("PullDown"))) {
                if (mc.thePlayer.motionY > -0.0784000015258789 && e.getPacket() instanceof C08PacketPlayerBlockPlacement wrapper) {
                    if (wrapper.getPosition().equals(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.4, mc.thePlayer.posZ))) {
                        mc.thePlayer.motionY = -0.0784000015258789;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (addons.isEnabled("Target block ESP") && !shaderCalled) {
            RenderUtils.renderBlock(data.blockPos.offset(data.facing), getModule(Interface.class).color(0, 100), false, true);
        }

        shaderCalled = false;
    }

    @EventTarget
    public void onShader(ShaderEvent e) {
        if (addons.isEnabled("Target block ESP")) {
            if (e.getShaderType() != ShaderEvent.ShaderType.GLOW) {
                RenderUtils.draw2DCube(data.blockPos.offset(data.facing), Color.black.getRGB());
            } else {
                RenderUtils.draw2DCube(data.blockPos.offset(data.facing), getModule(Interface.class).color());
            }
        }

        shaderCalled = true;
    }

    @EventTarget
    public void onWorld(WorldChangeEvent e) {
        setEnabled(false);
    }

    public boolean towering() {
        return Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && !MoveUtil.isMoving();
    }

    public boolean towerMoving() {
        return Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && MoveUtil.isMoving();
    }

    private int getBlockSlot() {
        int slot = mc.thePlayer.inventory.currentItem;

        if (getBlockCount() == 0) {
            return slot;
        }

        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];

            if (itemStack != null && itemStack.stackSize > 0) {
                final Item item = itemStack.getItem();

                if (item instanceof ItemBlock && !InventoryUtils.blacklistedBlocks.contains(((ItemBlock) item).getBlock())) {
                    slot = i;
                }
            }
        }

        return slot;
    }

    public int getBlockCount() {
        int blockCount = 0;

        for (int i = 36; i < 45; ++i) {
            if (!mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) continue;

            final ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if (!(is.getItem() instanceof ItemBlock && !InventoryUtils.blacklistedBlocks.contains(((ItemBlock) is.getItem()).getBlock()))) {
                continue;
            }

            blockCount += is.stackSize;
        }

        return blockCount;
    }

    private static boolean isInteractable(Block block) {
        return block instanceof BlockFurnace || block instanceof BlockFenceGate || block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockEnchantmentTable || block instanceof BlockBrewingStand || block instanceof BlockBed || block instanceof BlockDispenser || block instanceof BlockHopper || block instanceof BlockAnvil || block == Blocks.crafting_table;
    }

    private void place(BlockPos pos, EnumFacing facing, Vec3 hitVec) {
        MovingObjectPosition ray = PlayerUtils.rayTraceBlock(4.5, 1);
        BlockPos targetPos = pos;
        Vec3 targetHitVec = hitVec;
        boolean shouldPlace = false;

        switch (rayTraceMode.get()) {
            case "None":
                shouldPlace = true;
                break;
            case "Normal":
                if (ray.getBlockPos().equalsBlockPos(pos)) {
                    targetPos = ray.getBlockPos();
                    shouldPlace = true;
                }
                break;
            case "Strict":
                if (ray.getBlockPos().equalsBlockPos(pos) && ray.sideHit.equals(facing)) {
                    targetPos = ray.getBlockPos();
                    targetHitVec = ray.hitVec;
                    shouldPlace = true;
                }
                break;
        }

        if (shouldPlace) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), targetPos, facing, targetHitVec)) {
                if (addons.isEnabled("Swing")) {
                    mc.thePlayer.swingItem();
                    mc.getItemRenderer().resetEquippedProgress();
                } else {
                    mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                }
                blocksPlacedSneak++;
                blocksPlacedJump++;
            }
        }
    }

    private PlaceData findBlock(BlockPos pos) {
        PlaceData immediateCheck = checkPositions(pos);
        if (immediateCheck != null) {
            return immediateCheck;
        }

        for (int depth = 1; depth <= (rayTraceMode.is("None") ? maxPlaceRange.get() : 6); depth++) {
            List<BlockPos> positions = generateOffsets(pos, depth);
            if (positions.isEmpty()) continue;

            BlockPos playerPos = new BlockPos(mc.thePlayer.getPositionEyes(1));
            positions.sort(Comparator.comparingDouble(p -> p.distanceSq(playerPos)));

            for (BlockPos targetPos : positions) {
                PlaceData result = checkPositions(targetPos);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private List<BlockPos> generateOffsets(BlockPos origin, int depth) {
        List<BlockPos> result = new ArrayList<>((int) Math.pow(OFFSETS.length, depth));
        if (depth == 0) {
            result.add(origin);
            return result;
        }

        List<BlockPos> currentLevel = new ArrayList<>();
        List<BlockPos> nextLevel = new ArrayList<>();
        currentLevel.add(origin);

        for (int i = 0; i < depth; i++) {
            for (BlockPos pos : currentLevel) {
                for (BlockPos offset : OFFSETS) {
                    nextLevel.add(pos.add(offset));
                }
            }

            if (i < depth - 1) {
                currentLevel.clear();
                List<BlockPos> temp = currentLevel;
                currentLevel = nextLevel;
                nextLevel = temp;
            }
        }

        return nextLevel;
    }

    private PlaceData checkPositions(BlockPos basePos) {
        for (int i = 0; i < OFFSETS.length; i++) {
            BlockPos newPos = basePos.add(OFFSETS[i]);
            Block block = mc.theWorld.getBlockState(newPos).getBlock();

            if (block != Blocks.air) {
                if (!block.getMaterial().isReplaceable() && !isInteractable(block)) {
                    return new PlaceData(FACINGS[i], newPos);
                }
            }
        }
        return null;
    }

    private float[] getNearestRotation(float[] currentRot) {
        BlockPos blockPos = data.blockPos;
        EnumFacing face = data.facing;

        {
            MovingObjectPosition ray = PlayerUtils.rayTraceBlock(currentRot, 4.5, 1);

            if (ray.getBlockPos().equalsBlockPos(blockPos) && ray.sideHit.equals(face)) {
                return currentRot;
            }
        }

        float[] bestRot = null;
        float bestDiff = Float.MAX_VALUE;

        Vec3i faceVec = face.getDirectionVec();
        double[] xRange = getAxisMinMax(faceVec.getX());
        double[] yRange = getAxisMinMax(faceVec.getY());
        double[] zRange = getAxisMinMax(faceVec.getZ());

        for (double x = xRange[0]; x <= xRange[1]; x += 0.01) {
            for (double y = yRange[0]; y <= yRange[1]; y += 0.01) {
                for (double z = zRange[0]; z <= zRange[1]; z += 0.01) {
                    Vec3 pos = new Vec3(
                            blockPos.getX() + clampInBlock(x),
                            blockPos.getY() + clampInBlock(y),
                            blockPos.getZ() + clampInBlock(z)
                    );

                    float[] rot = RotationUtils.getRotations(pos);
                    if (Float.isNaN(rot[0]) || Float.isNaN(rot[1])) continue;

                    float diff = RotationUtils.getRotationDifference(currentRot, rot);
                    MovingObjectPosition ray = PlayerUtils.rayTraceBlock(rot, 4.5, 1);

                    if (ray.getBlockPos().equalsBlockPos(blockPos) && ray.sideHit.equals(face)) {
                        if (diff < bestDiff) {
                            bestRot = rot;
                            bestDiff = diff;
                        }
                    }
                }
            }
        }

        return bestRot != null ? bestRot : RotationUtils.getRotations(RotationUtils.getVec3(blockPos, face));
    }

    private double[] getAxisMinMax(int axisValue) {
        if (axisValue == 1) return new double[]{0.95, 0.99};
        if (axisValue == -1) return new double[]{0.01, 0.05};
        return new double[]{0.2, 0.8};
    }

    private double clampInBlock(double offset) {
        return Math.max(0.01, Math.min(0.99, offset));
    }

    @AllArgsConstructor
    private static class PlaceData {
        public EnumFacing facing;
        public BlockPos blockPos;
    }
}
