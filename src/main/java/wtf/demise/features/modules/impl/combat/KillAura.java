package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.*;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.legit.AutoRod;
import wtf.demise.features.modules.impl.misc.Targets;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.player.clicking.ClickHandler;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.player.clicking.ClickManager;
import wtf.demise.utils.player.rotation.RotationHandler;
import wtf.demise.utils.player.rotation.RotationManager;
import wtf.demise.utils.player.rotation.RotationUtils;
import wtf.demise.utils.render.RenderUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "KillAura", description = "Automatically attack targets.")
public class KillAura extends Module {

    // reach
    public final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 4.0f, 1, 8, 0.1f, this);

    private final ClickManager clickManager = new ClickManager(this);

    // autoBlock
    public final BoolValue autoBlock = new BoolValue("AutoBlock", true, this);
    public final SliderValue autoBlockRange = new SliderValue("AutoBlock range", 3.5f, 1, 8, 0.1f, this, autoBlock::get);
    public final ModeValue autoBlockMode = new ModeValue("AutoBlock mode", new String[]{"Fake", "Vanilla", "Release", "Force", "Blink", "NCP"}, "Vanilla", this, autoBlock::get);
    private final SliderValue reblockTicks = new SliderValue("Reblock ticks", 0, 0, 5, 1, this, autoBlock::get);
    private final BoolValue alwaysRenderBlocking = new BoolValue("Always render blocking", false, this, autoBlock::get);

    // rotation
    private final RotationManager rotationManager = new RotationManager(this);

    // aim point
    private final ModeValue aimPos = new ModeValue("Aim position", new String[]{"Head", "Torso", "Straight"}, "Straight", this);
    private final BoolValue setMinAimPoint = new BoolValue("Set min aim point", true, this, () -> aimPos.is("Straight"));
    private final SliderValue yTrim = new SliderValue("Y trim", 0, 0, 0.5f, 0.01f, this);

    private final BoolValue aimAtNearestVisiblePoint = new BoolValue("Aim at nearest visible point", false, this);

    private final SliderValue targetOffset = new SliderValue("Target pos offset", 0, -5, 5, 0.01f, this);

    private final BoolValue predictionHeuristics = new BoolValue("Prediction heuristics", false, this);

    private final BoolValue delayed = new BoolValue("Delayed target pos", false, this);
    private final SliderValue delayUpdates = new SliderValue("Delay updates", 1, 1, 50, 1, this, () -> delayed.get() && delayed.canDisplay());
    private final BoolValue delayOnHurtTime = new BoolValue("Delay on hurtTime", true, this, () -> delayed.get() && delayed.canDisplay());
    private final SliderValue hurtTime = new SliderValue("Delay hurtTime", 5, 1, 10, 1, this, () -> delayOnHurtTime.get() && delayOnHurtTime.canDisplay());

    private final ModeValue offsetMode = new ModeValue("Offset mode", new String[]{"None", "Gaussian", "Noise", "Jitter"}, "None", this);
    private final BoolValue notOnFirstHit = new BoolValue("Not on first hit", false, this, () -> !offsetMode.is("None"));
    private final BoolValue onlyOnRotation = new BoolValue("Only on rotation", true, this, () -> !offsetMode.is("None"));
    private final SliderValue oChance = new SliderValue("Offset chance", 75, 1, 100, 1, this, () -> !offsetMode.is("None") && !offsetMode.is("Jitter"));
    private final SliderValue minYawFactor = new SliderValue("Min Yaw Factor", 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None"));
    private final SliderValue maxYawFactor = new SliderValue("Max Yaw Factor", 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None"));
    private final SliderValue minPitchFactor = new SliderValue("Min Pitch Factor", 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None"));
    private final SliderValue maxPitchFactor = new SliderValue("Max Pitch Factor", 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None"));
    private final BoolValue interpolateVec = new BoolValue("Interpolate vec", false, this, () -> !offsetMode.is("None"));
    private final SliderValue amount = new SliderValue("Amount", 0.5f, 0.01f, 1, 0.01f, this, () -> interpolateVec.get() && interpolateVec.canDisplay());

    private final SliderValue xOffset = new SliderValue("Static X offset", 0, -0.4f, 0.4f, 0.01f, this);
    private final SliderValue yOffset = new SliderValue("Static Y offset", 0, -2, 0.4f, 0.01f, this);

    private final BoolValue pauseInHitbox = new BoolValue("Pause rotations in hitbox", false, this);

    // target
    private final ModeValue targetMode = new ModeValue("Target selection mode", new String[]{"Single", "Switch"}, "Single", this);
    private final ModeValue targetPriority = new ModeValue("Target Priority", new String[]{"None", "Distance", "Health", "HurtTime", "Angle"}, "Distance", this, () -> targetMode.is("Single"));
    private final SliderValue targetSwitchDelay = new SliderValue("Target Switch Delay (ms)", 500, 50, 1000, 50, this, () -> targetMode.is("Switch"));

    // visual
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);

    private final Queue<Vec3> positionHistory = new LinkedList<>();
    private final TimerUtils lastSwitchTime = new TimerUtils();
    public static EntityLivingBase currentTarget;
    public static boolean isBlocking = false;
    private Vec3 targetVec;
    public Vec3 currentVec;
    private double lastXOffset;
    private double lastYOffset;
    private double lastZOffset;
    private boolean shouldRandomize;
    private Vec3 offsetVec = new Vec3(0, 0, 0);
    private float[] prevRot;
    private int totalBlockingTicks;
    private boolean firstHit = true;
    private Vec3 delayedVec = new Vec3(0, 0, 0);
    private boolean tickRotating;
    private int runTicks;

    @Override
    public void onEnable() {
        lastSwitchTime.reset();
        firstHit = true;
    }

    @Override
    public void onDisable() {
        if (isBlocking) {
            setBlocking(false);
        }

        isBlocking = false;

        if (BlinkComponent.blinking) {
            BlinkComponent.dispatch(true);
        }

        currentTarget = null;
        positionHistory.clear();
    }

    private EntityLivingBase findNextTarget() {
        List<Entity> targets = new ArrayList<>();

        MultiBoolValue allowedTargets = getModule(Targets.class).allowedTargets;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity != mc.thePlayer) {
                if (!(entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager || entity instanceof EntityPlayer)) {
                    continue;
                }

                if (entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager) {
                    if (!allowedTargets.isEnabled("Non players")) continue;
                }

                if (entity.isInvisible() && !allowedTargets.isEnabled("Invisibles")) continue;
                if (entity.isDead && !allowedTargets.isEnabled("Dead")) continue;

                if (entity instanceof EntityPlayer) {
                    if (!allowedTargets.isEnabled("Players")) continue;
                    if (Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity)) continue;
                    if (!allowedTargets.isEnabled("Bots") && getModule(AntiBot.class).isBot((EntityPlayer) entity))
                        continue;
                    if (PlayerUtils.isInTeam(entity) && !allowedTargets.isEnabled("Teams")) continue;
                }

                double distanceToEntity = PlayerUtils.getDistanceToEntityBox(entity);

                if (distanceToEntity <= searchRange.get()) {
                    targets.add(entity);
                }
            }
        }

        if (targets.isEmpty()) {
            return null;
        }

        int index = targets.indexOf(currentTarget);
        return (EntityLivingBase) targets.get((index + 1) % targets.size());
    }

    public EntityLivingBase findTarget() {
        EntityLivingBase target = null;
        double closestDistance = searchRange.get() + 0.4;
        double leastHealth = Float.MAX_VALUE;
        double leastHurtTime = 10;
        double closestAngle = Double.MAX_VALUE;

        MultiBoolValue allowedTargets = getModule(Targets.class).allowedTargets;

        for (Entity e : mc.theWorld.loadedEntityList) {
            if (!(e instanceof EntityLivingBase entity)) continue;

            double distanceToEntity = PlayerUtils.getDistanceToEntityBox(entity);

            if (entity != mc.thePlayer && distanceToEntity <= searchRange.get()) {
                if (!(entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager || entity instanceof EntityPlayer)) {
                    continue;
                }

                if (entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityVillager) {
                    if (!allowedTargets.isEnabled("Non players")) continue;
                }

                if (entity.isInvisible() && !allowedTargets.isEnabled("Invisibles")) continue;
                if (entity.isDead && !allowedTargets.isEnabled("Dead")) continue;

                if (entity instanceof EntityPlayer) {
                    if (!allowedTargets.isEnabled("Players")) continue;
                    if (Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity)) continue;
                    if (!allowedTargets.isEnabled("Bots") && getModule(AntiBot.class).isBot((EntityPlayer) entity))
                        continue;
                    if (PlayerUtils.isInTeam(entity) && !allowedTargets.isEnabled("Teams")) continue;
                }

                switch (targetPriority.get()) {
                    case "Distance":
                        if (distanceToEntity < closestDistance) {
                            target = entity;
                            closestDistance = distanceToEntity;
                        }
                        break;
                    case "Health":
                        if (PlayerUtils.getActualHealth(entity) < leastHealth) {
                            target = entity;
                            leastHealth = PlayerUtils.getActualHealth(entity);
                        }
                        break;
                    case "HurtTime":
                        if (entity.hurtTime <= leastHurtTime) {
                            target = entity;
                            leastHurtTime = entity.hurtTime;
                        }
                        break;
                    case "Angle":
                        if (RotationUtils.getRotationDifferenceClientRot(entity) < closestAngle) {
                            target = entity;
                            closestAngle = RotationUtils.getRotationDifferenceClientRot(entity);
                        }
                        break;
                }
            }
        }

        return target;
    }

    @EventTarget
    public void onGameUpdate(GameEvent e) {
        setTag(targetMode.get());

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        switch (targetMode.get()) {
            case "Single":
                currentTarget = findTarget();
                break;
            case "Switch":
                if (lastSwitchTime.hasTimeElapsed(targetSwitchDelay.get())) {
                    currentTarget = findNextTarget();
                    lastSwitchTime.reset();
                }
                break;
        }

        if (currentTarget != null) {
            if (!isTargetInvalid()) {
                if (notOnFirstHit.get() && PlayerUtils.getDistanceToEntityBox(currentTarget) > attackRange.get() && mc.thePlayer.hurtTime == 0 && currentTarget.hurtTime == 0) {
                    firstHit = true;
                }

                clickManager.click(attackRange.get(), currentTarget);

                double distance = PlayerUtils.getDistanceToEntityBox(currentTarget);

                if (distance > autoBlockRange.get() && isBlocking) {
                    setBlocking(false);
                }
            } else {
                currentTarget = null;
            }
        } else {
            positionHistory.clear();

            if (isBlocking) {
                setBlocking(false);
            }

            if (BlinkComponent.blinking) {
                BlinkComponent.dispatch(true);
            }
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (currentTarget != null && !isTargetInvalid()) {
            double distance = PlayerUtils.getDistanceToEntityBox(currentTarget);

            if (getModule(AutoRod.class).rotating && getModule(AutoRod.class).overrideAuraRots.get() && getModule(AutoRod.class).isEnabled()) {
                return;
            }

            if (distance <= searchRange.get()) {
                rotationManager.setRotation(getRotations());
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        shouldRandomize = rand.nextInt(100) <= oChance.get();

        rotationManager.updateRotSpeed(e);

        if (currentTarget != null && !isTargetInvalid() && delayed.get()) {
            positionHistory.offer(targetVec);
        } else {
            positionHistory.clear();
            delayedVec = targetVec;
        }

        if (currentTarget != null) {
            double diffYaw = 0;
            double diffPitch = 0;

            if (RotationHandler.currentRotation != null && prevRot != null) {
                diffYaw = MathHelper.wrapAngleTo180_float(RotationHandler.currentRotation[0] - prevRot[0]);
                diffPitch = MathHelper.wrapAngleTo180_float(RotationHandler.currentRotation[1] - prevRot[1]);
            }

            float yawSpeed = (rotationManager.getRandYawSpeed() * rotationManager.getExtraSmoothFactor().get()) / 2;
            float pitchSpeed = (rotationManager.getRandPitchSpeed() * rotationManager.getExtraSmoothFactor().get()) / 2;

            tickRotating = Math.abs(diffYaw) > yawSpeed || Math.abs(diffPitch) > pitchSpeed;
        }
    }

    @EventTarget
    public void onRenderTick(Render3DEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (currentTarget != null && !isTargetInvalid() && targetESP.get()) {
            RenderUtils.drawTargetCircle(currentTarget);
        }
    }

    // shut up intellij
    private boolean isTargetInvalid() {
        return currentTarget.isDead || PlayerUtils.getDistanceToEntityBox(currentTarget) > searchRange.get() || currentTarget.getEntityWorld() != mc.thePlayer.getEntityWorld();
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        if (canAutoBlock()) {
            onAttack();
        }

        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            firstHit = false;
        }
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent e) {
        e.setRange(attackRange.get());
    }

    public void preAttack() {
        if (canAutoBlock()) {
            totalBlockingTicks++;

            switch (autoBlockMode.get()) {
                case "None", "Release":
                    if (isBlocking) {
                        setBlocking(false);
                    }
                    break;
                case "Fake", "NCP", "Force":
                    isBlocking = true;
                    break;
                case "Vanilla":
                    setBlocking(shouldBlock());
                    break;
                case "Blink":
                    BlinkComponent.blinking = true;
                    if (isBlocking) {
                        setBlocking(false);
                    }
                    break;
            }
        } else if (isBlocking) {
            setBlocking(false);
            totalBlockingTicks = 0;
            isBlocking = false;
        }
    }

    private void onAttack() {
        if (canAutoBlock()) {
            switch (autoBlockMode.get()) {
                case "Force":
                    setBlocking(true);
                    break;
            }
        } else if (isBlocking) {
            setBlocking(false);
        }
    }

    public void postAttack() {
        if (canAutoBlock()) {
            switch (autoBlockMode.get()) {
                case "Release":
                    if (!isBlocking) {
                        setBlocking(true);
                    }
                    break;
                case "Blink":
                    interact();
                    if (!isBlocking) {
                        setBlocking(true);
                    }
                    BlinkComponent.dispatch(true);
                    break;
            }
        } else if (isBlocking) {
            setBlocking(false);
        }
    }

    private void interact() {
        MovingObjectPosition ray = mc.objectMouseOver;

        if (ray == null) return;

        if (ray.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            sendPacket(new C02PacketUseEntity(ray.entityHit, ray.hitVec));
        }
    }

    private void setBlocking(boolean state) {
        if (mc.currentScreen != null) return;

        switch (autoBlockMode.get()) {
            case "Blink", "Release", "NCP", "Force":
                if (state) {
                    if (shouldBlock()) {
                        sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                        if (!alwaysRenderBlocking.get()) isBlocking = true;
                    }
                } else {
                    sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    if (!alwaysRenderBlocking.get()) isBlocking = false;
                }
                break;
            default:
                if (state) {
                    if (shouldBlock()) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                        if (!alwaysRenderBlocking.get()) isBlocking = true;
                    }
                } else {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    if (!alwaysRenderBlocking.get()) isBlocking = false;
                }
                break;
        }

        if (alwaysRenderBlocking.get()) {
            isBlocking = state;
        }
    }

    private boolean shouldBlock() {
        return totalBlockingTicks % (reblockTicks.get() + 1) != 0;
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (autoBlockMode.is("NCP") && canAutoBlock()) {
            if (e.isPre()) {
                sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            }

            if (e.isPost()) {
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem());
            }
        }
    }

    private boolean canAutoBlock() {
        return PlayerUtils.isHoldingSword() && autoBlock.get() && currentTarget != null && PlayerUtils.getDistanceToEntityBox(currentTarget) <= autoBlockRange.get();
    }

    private boolean targetRunningAway() {
        double lookX = currentTarget.posX - mc.thePlayer.posX;
        double lookY = currentTarget.posZ - mc.thePlayer.posZ;

        float yaw = (float) (Math.atan2(lookY, lookX) * 180.0D / Math.PI) - 90.0F;
        yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);

        if (Math.abs(yaw - MathHelper.wrapAngleTo180_float(currentTarget.rotationYaw)) < 90 && currentTarget.getDistance(currentTarget.lastTickPosX, currentTarget.posY, currentTarget.lastTickPosZ) > 0.1) {
            runTicks++;
        } else {
            runTicks = 0;
        }

        return runTicks >= 5;
    }

    private float[] getRotations() {
        Vec3 playerPos = mc.thePlayer.getPositionEyes(1);

        float targetOffset = this.targetOffset.get();

        double dist = PlayerUtils.getDistanceToEntityBox(currentTarget);

        if (predictionHeuristics.get() && ((targetRunningAway() && dist > attackRange.get()) || dist > attackRange.get() + 1.5)) {
            targetOffset = (float) (Math.min(Math.max((dist - attackRange.get()) * 3, 0), 10));
            rotationManager.setSpeedMulti(0.166f);
        } else {
            rotationManager.setSpeedMulti(1);
        }

        Vec3 prediction = currentTarget.getPositionVector().subtract(new Vec3(currentTarget.prevPosX, currentTarget.prevPosY, currentTarget.prevPosZ)).multiply(targetOffset);

        AxisAlignedBB bb = currentTarget.getHitbox().offset(prediction).contract(0, yTrim.get(), 0);
        AxisAlignedBB finalBoundingBox = new AxisAlignedBB(bb.minX, bb.minY + (setMinAimPoint.get() ? currentTarget.height * 0.45 : 0), bb.minZ, bb.maxX, bb.maxY, bb.maxZ);

        Vec3 boxCenter = bb.getCenter();
        Vec3 entityPos = new Vec3(boxCenter.xCoord, bb.minY, boxCenter.zCoord);
        Vec3 vec;

        switch (aimPos.get()) {
            case "Head": {
                vec = entityPos.add(0.0, currentTarget.getEyeHeight(), 0.0);
            }
            break;
            case "Torso": {
                vec = entityPos.add(0.0, currentTarget.height * 0.75, 0.0);
            }
            break;
            case "Straight": {
                double ex = (finalBoundingBox.maxX + finalBoundingBox.minX) / 2;
                double ey = MathHelper.clamp_double(playerPos.yCoord, finalBoundingBox.minY, finalBoundingBox.maxY);
                double ez = (finalBoundingBox.maxZ + finalBoundingBox.minZ) / 2;

                vec = new Vec3(ex, ey, ez);
            }
            break;
            default:
                vec = entityPos.add(0, 0, 0);
                break;
        }

        if (!mc.thePlayer.canPosBeSeen(entityPos.add(0, currentTarget.getEyeHeight(), 0)) && aimAtNearestVisiblePoint.get()) {
            vec = RotationUtils.findNearestVisiblePoint(vec, finalBoundingBox);
        }

        if ((!firstHit || !notOnFirstHit.get()) && (!onlyOnRotation.get() || tickRotating)) {
            double minXZ = -0.5;
            double maxXZ = 0.5;
            double minY = -2;
            double maxY = 0.5;

            double yawFactor = MathUtils.randomizeDouble(minYawFactor.get(), maxYawFactor.get());
            double pitchFactor = MathUtils.randomizeDouble(minPitchFactor.get(), maxPitchFactor.get());

            switch (offsetMode.get()) {
                case "Gaussian": {
                    double meanXZ = (minXZ + maxXZ) / 2;
                    double stdDevXZ = (maxXZ - minXZ) / 4;
                    double meanY = (minY + maxY) / 2;
                    double stdDevY = (maxY - minY) / 4;

                    double xOffset = ThreadLocalRandom.current().nextGaussian(meanXZ, stdDevXZ) * yawFactor;
                    double yOffset = ThreadLocalRandom.current().nextGaussian(meanY, stdDevY) * pitchFactor;
                    double zOffset = ThreadLocalRandom.current().nextGaussian(meanXZ, stdDevXZ) * yawFactor;

                    if (shouldRandomize) {
                        offsetVec = MathUtils.interpolate(offsetVec, new Vec3(xOffset, yOffset, zOffset), interpolateVec.get() ? amount.get() : mc.timer.partialTicks);

                        lastXOffset = (float) xOffset;
                        lastYOffset = (float) yOffset;
                        lastZOffset = (float) zOffset;
                    } else {
                        offsetVec = MathUtils.interpolate(offsetVec, new Vec3(lastXOffset, lastYOffset, lastZOffset), interpolateVec.get() ? amount.get() : mc.timer.partialTicks);
                    }
                }
                break;
                case "Noise": {
                    double xOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;
                    double yOffset = MathUtils.randomizeDouble(minY, maxY) * pitchFactor;
                    double zOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;

                    if (shouldRandomize) {
                        offsetVec = MathUtils.interpolate(offsetVec, new Vec3(xOffset, yOffset, zOffset), interpolateVec.get() ? amount.get() : mc.timer.partialTicks);

                        lastXOffset = (float) xOffset;
                        lastYOffset = (float) yOffset;
                        lastZOffset = (float) zOffset;
                    } else {
                        offsetVec = MathUtils.interpolate(offsetVec, new Vec3(lastXOffset, lastYOffset, lastZOffset), interpolateVec.get() ? amount.get() : mc.timer.partialTicks);
                    }
                }
                break;
                case "Jitter":
                    if (ClickHandler.clickingNow) {
                        double xOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;
                        double yOffset = MathUtils.randomizeDouble(minY, maxY) * pitchFactor;
                        double zOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;

                        offsetVec = MathUtils.interpolate(offsetVec, new Vec3(lastXOffset, lastYOffset, lastZOffset), interpolateVec.get() ? amount.get() : mc.timer.partialTicks);

                        lastXOffset = (float) xOffset;
                        lastYOffset = (float) yOffset;
                        lastZOffset = (float) zOffset;
                    } else {
                        offsetVec = MathUtils.interpolate(offsetVec, new Vec3(lastXOffset, lastYOffset, lastZOffset), interpolateVec.get() ? amount.get() : mc.timer.partialTicks);
                    }
                    break;
                default:
                    offsetVec = new Vec3(0, 0, 0);
            }
        } else {
            offsetVec = MathUtils.interpolate(offsetVec, new Vec3(0, 0, 0), (interpolateVec.get() ? amount.get() : mc.timer.partialTicks) / 4);
        }

        targetVec = new Vec3(vec.xCoord, vec.yCoord, vec.zCoord).add(offsetVec).add(xOffset.get(), yOffset.get(), xOffset.get());
        if (delayed.get() && targetVec != null) {
            positionHistory.offer(targetVec);
        }

        if (targetVec != null) {
            targetVec.xCoord = MathHelper.clamp_double(targetVec.xCoord, finalBoundingBox.minX, finalBoundingBox.maxX);
            targetVec.yCoord = MathHelper.clamp_double(targetVec.yCoord, finalBoundingBox.minY, finalBoundingBox.maxY);
            targetVec.zCoord = MathHelper.clamp_double(targetVec.zCoord, finalBoundingBox.minZ, finalBoundingBox.maxZ);
        }

        if (delayed.get()) {
            while (positionHistory.size() > delayUpdates.get()) {
                positionHistory.poll();
            }

            if (positionHistory.size() < delayUpdates.get()) {
                delayedVec = targetVec;
            }

            if (positionHistory.size() >= delayUpdates.get()) {
                if (!delayOnHurtTime.get() || currentTarget.hurtTime >= hurtTime.get()) {
                    delayedVec = positionHistory.poll();
                } else {
                    delayedVec = targetVec;
                }
            }

            currentVec = delayedVec != null ? delayedVec : targetVec;
        } else {
            currentVec = targetVec;
        }

        assert currentVec != null;

        double deltaX = currentVec.xCoord - playerPos.xCoord;
        double deltaY = currentVec.yCoord - playerPos.yCoord;
        double deltaZ = currentVec.zCoord - playerPos.zCoord;

        float yaw = (float) -(Math.atan2(deltaX, deltaZ) * (180.0 / Math.PI));
        float pitch = (float) (-Math.toDegrees(Math.atan2(deltaY, Math.hypot(deltaX, deltaZ))));

        if (mc.thePlayer.getEntityBoundingBox().intersectsWith(currentTarget.getEntityBoundingBox()) && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && pauseInHitbox.get()) {
            yaw = prevRot[0];
            pitch = prevRot[1];
        }

        prevRot = new float[]{yaw, pitch};
        return new float[]{yaw, pitch};
    }
}