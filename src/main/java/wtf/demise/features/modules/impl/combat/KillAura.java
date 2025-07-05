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
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.player.ClickHandler;
import wtf.demise.utils.player.PlayerUtils;
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

    // attack
    private final ModeValue clickMode = new ModeValue("Click mode", new String[]{"Legit", "Packet", "PlayerController"}, "Packet", this);
    private final BoolValue smartClicking = new BoolValue("Smart clicking", false, this);
    private final BoolValue forceAttackOnBacktrack = new BoolValue("Force attack on BackTrack", false, this, smartClicking::get);
    private final BoolValue ignoreBlocking = new BoolValue("Ignore blocking", true, this);
    private final SliderValue minCPS = new SliderValue("CPS (min)", 12, 0, 20, 1, this);
    private final SliderValue maxCPS = new SliderValue("CPS (max)", 16, 0, 20, 1, this);
    public final BoolValue rayTrace = new BoolValue("RayTrace", false, this);
    private final BoolValue failSwing = new BoolValue("Fail swing", false, this);
    private final SliderValue swingRange = new SliderValue("Swing range", 3.5f, 1, 8, 0.1f, this, failSwing::get);

    // autoBlock
    public final BoolValue autoBlock = new BoolValue("AutoBlock", true, this);
    public final SliderValue autoBlockRange = new SliderValue("AutoBlock range", 3.5f, 1, 8, 0.1f, this, autoBlock::get);
    public final ModeValue autoBlockMode = new ModeValue("AutoBlock mode", new String[]{"Fake", "Vanilla", "Release", "Force", "Blink", "NCP"}, "Vanilla", this, autoBlock::get);
    private final SliderValue reblockTicks = new SliderValue("Reblock ticks", 0, 0, 5, 1, this, autoBlock::get);
    private final BoolValue alwaysRenderBlocking = new BoolValue("Always render blocking", false, this, autoBlock::get);
    public final BoolValue unBlockOnRayCastFail = new BoolValue("Unblock on rayCast fail", false, this, () -> autoBlock.get() && rayTrace.get());

    // rotation
    private final RotationManager rotationManager = new RotationManager(this);

    // aim point
    private final ModeValue aimPos = new ModeValue("Aim position", new String[]{"Head", "Torso", "Legs", "Nearest", "Straight", "Assist"}, "Straight", this);
    private final BoolValue setMinAimPoint = new BoolValue("Set min aim point", true, this, () -> aimPos.is("Straight"));
    private final SliderValue yTrim = new SliderValue("Y trim", 0, 0, 0.5f, 0.01f, this);
    private final BoolValue predict = new BoolValue("Rotation prediction", false, this);
    private final SliderValue predictTicks = new SliderValue("Predict ticks", 2, 1, 3, 1, this, () -> predict.get() && predict.canDisplay());
    private final SliderValue targetOffset = new SliderValue("Normal target pos offset", 0, -5, 5, 0.01f, this);
    private final BoolValue staticMissOffset = new BoolValue("Static miss offset", true, this);
    private final SliderValue missTargetOffset = new SliderValue("Miss target pos offset", 0, -5, 5, 0.01f, this, staticMissOffset::get);
    private final SliderValue minMissOffset = new SliderValue("Min miss offset", 0, -5, 5, 0.01f, this, () -> !staticMissOffset.get());
    private final SliderValue maxMissOffset = new SliderValue("max miss offset", 0, -5, 5, 0.01f, this, () -> !staticMissOffset.get());
    private final SliderValue missOffsetLerp = new SliderValue("Miss offset lerp", 0.1f, 0.01f, 1, 0.01f, this, () -> !staticMissOffset.get());
    private final BoolValue delayed = new BoolValue("Delayed target pos", false, this);
    private final SliderValue delayUpdates = new SliderValue("Delay updates", 1, 1, 50, 1, this, () -> delayed.get() && delayed.canDisplay());
    private final BoolValue delayOnHurtTime = new BoolValue("Delay on hurtTime", true, this, () -> delayed.get() && delayed.canDisplay());
    private final SliderValue hurtTime = new SliderValue("Delay hurtTime", 5, 1, 10, 1, this, () -> delayOnHurtTime.get() && delayOnHurtTime.canDisplay());
    private final ModeValue offsetMode = new ModeValue("Offset mode", new String[]{"None", "Gaussian", "Noise", "Jitter"}, "None", this);
    private final BoolValue notOnFirstHit = new BoolValue("Not on first hit", false, this, () -> !offsetMode.is("None"));
    private final SliderValue oChance = new SliderValue("Offset chance", 75, 1, 100, 1, this, () -> !offsetMode.is("None") && !offsetMode.is("Jitter"));
    private final SliderValue minYawFactor = new SliderValue("Min Yaw Factor", 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None"));
    private final SliderValue maxYawFactor = new SliderValue("Max Yaw Factor", 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None"));
    private final SliderValue minPitchFactor = new SliderValue("Min Pitch Factor", 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None"));
    private final SliderValue maxPitchFactor = new SliderValue("Max Pitch Factor", 0.25f, 0, 1, 0.01f, this, () -> !offsetMode.is("None"));
    private final BoolValue interpolateVec = new BoolValue("Interpolate vec", false, this, () -> !offsetMode.is("None"));
    private final SliderValue amount = new SliderValue("Amount", 0.5f, 0.01f, 1, 0.01f, this, () -> interpolateVec.get() && interpolateVec.canDisplay());
    private final SliderValue xOffset = new SliderValue("Static X offset", 0, -0.4f, 0.4f, 0.01f, this);
    private final SliderValue yOffset = new SliderValue("Static Y offset", 0, -2, 0.4f, 0.01f, this);
    private final BoolValue onlyUpdateOnMiss = new BoolValue("Only update on miss", false, this);
    private final BoolValue onlyRotateOnMiss = new BoolValue("Only rotate on miss", false, this);

    // target
    private final ModeValue targetMode = new ModeValue("Target selection mode", new String[]{"Single", "Switch"}, "Single", this);
    private final ModeValue targetPriority = new ModeValue("Target Priority", new String[]{"None", "Distance", "Health", "HurtTime", "Angle"}, "Distance", this, () -> targetMode.is("Single"));
    private final SliderValue targetSwitchDelay = new SliderValue("Target Switch Delay (ms)", 500, 50, 1000, 50, this, () -> targetMode.is("Switch"));

    // visual
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);

    // targetS
    private final MultiBoolValue allowedTargets = new MultiBoolValue("Allowed targets", Arrays.asList(
            new BoolValue("Players", true),
            new BoolValue("Non players", true),
            new BoolValue("Teams", true),
            new BoolValue("Bots", false),
            new BoolValue("Invisibles", false),
            new BoolValue("Dead", false)
    ), this);

    private PlayerUtils.PredictProcess selfPrediction;
    private final Queue<Vec3> positionHistory = new LinkedList<>();
    private final TimerUtils lastSwitchTime = new TimerUtils();
    public List<EntityLivingBase> targets = new ArrayList<>();
    public static EntityLivingBase currentTarget;
    public static boolean isBlocking = false;
    private Vec3 targetVec;
    public Vec3 currentVec;
    private double lastXOffset;
    private double lastYOffset;
    private double lastZOffset;
    private float currentMissOffset;
    private boolean shouldRandomize;
    private Vec3 offsetVec = new Vec3(0, 0, 0);
    private float[] prevRot;
    private int totalBlockingTicks;
    private boolean firstHit = true;
    private Vec3 delayedVec = new Vec3(0, 0, 0);

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
        targets.clear();
        positionHistory.clear();
    }

    private EntityLivingBase findNextTarget() {
        List<Entity> targets = new ArrayList<>();

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

                ClickHandler.ClickMode mode = ClickHandler.ClickMode.valueOf(clickMode.get());
                ClickHandler.initHandler(minCPS.get(), maxCPS.get(), rayTrace.get() && rayTrace.canDisplay(), smartClicking.get(), forceAttackOnBacktrack.get(), ignoreBlocking.get(), failSwing.get(), attackRange.get(), swingRange.get(), mode, currentTarget);

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

        if (Objects.equals(clickMode.get(), "Packet")) {
            targets.clear();
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityLivingBase && PlayerUtils.getDistanceToEntityBox(entity) <= searchRange.get()) {
                    targets.add((EntityLivingBase) entity);
                }
            }
        }

        targets.removeIf(target -> isTargetInvalid());
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
        if (currentTarget != null && !isTargetInvalid()) {
            double distance = PlayerUtils.getDistanceToEntityBox(currentTarget);

            if (distance <= searchRange.get()) {
                rotationManager.setRotation(getRotations(currentTarget));
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
        if (extraCheck() && canAutoBlock()) {
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
        if (extraCheck() && canAutoBlock()) {
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
        if (extraCheck() && canAutoBlock()) {
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

    private boolean extraCheck() {
        return !ClickHandler.rayTraceFailed() || !unBlockOnRayCastFail.get() || !rayTrace.get();
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
        return totalBlockingTicks % (reblockTicks.get() + 1) == 0;
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (autoBlockMode.is("NCP") && canAutoBlock() && extraCheck()) {
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

    private float[] getRotations(EntityLivingBase entity) {
        Vec3 playerPos;

        if (predict.get() && selfPrediction != null) {
            playerPos = new Vec3(selfPrediction.position.xCoord, selfPrediction.position.yCoord + mc.thePlayer.getEyeHeight(), selfPrediction.position.zCoord);
        } else {
            playerPos = mc.thePlayer.getPositionEyes(1);
        }

        double predictionAmount;

        if (!mc.objectMouseOver.typeOfHit.equals(MovingObjectPosition.MovingObjectType.ENTITY) || PlayerUtils.getDistanceToEntityBox(entity) > attackRange.get()) {
            if (staticMissOffset.get()) {
                predictionAmount = missTargetOffset.get();
            } else {
                currentMissOffset = MathUtils.interpolate(currentMissOffset, maxMissOffset.get(), missOffsetLerp.get());
                predictionAmount = currentMissOffset;
            }
        } else {
            predictionAmount = targetOffset.get();
            currentMissOffset = minMissOffset.get();
        }

        Vec3 prediction = entity.getPositionVector().subtract(new Vec3(entity.prevPosX, entity.prevPosY, entity.prevPosZ)).multiply(predictionAmount);

        AxisAlignedBB bb = entity.getHitbox().offset(prediction).contract(0, yTrim.get(), 0);
        AxisAlignedBB finalBoundingBox = new AxisAlignedBB(bb.minX, bb.minY + (setMinAimPoint.get() ? entity.height * 0.45 : 0), bb.minZ, bb.maxX, bb.maxY, bb.maxZ);

        Vec3 boxCenter = bb.getCenter();
        Vec3 entityPos = new Vec3(boxCenter.xCoord, bb.minY, boxCenter.zCoord);
        Vec3 vec;

        switch (aimPos.get()) {
            case "Head": {
                vec = entityPos.add(0.0, entity.getEyeHeight(), 0.0);
            }
            break;
            case "Torso": {
                vec = entityPos.add(0.0, entity.height * 0.75, 0.0);
            }
            break;
            case "Legs": {
                vec = entityPos.add(0.0, entity.height * 0.45, 0.0);
            }
            break;
            case "Nearest": {
                vec = RotationUtils.getBestHitVec(entity);
            }
            break;
            case "Straight": {
                final double ex = (finalBoundingBox.maxX + finalBoundingBox.minX) / 2;
                final double ey = MathHelper.clamp_double(playerPos.yCoord, finalBoundingBox.minY, finalBoundingBox.maxY);
                final double ez = (finalBoundingBox.maxZ + finalBoundingBox.minZ) / 2;

                vec = new Vec3(ex, ey, ez);
            }
            break;
            case "Assist": {
                final Vec3 pos = playerPos.add(mc.thePlayer.getLookVec());

                final double ex = MathHelper.clamp_double(pos.xCoord, finalBoundingBox.minX, finalBoundingBox.maxX);
                final double ey = MathHelper.clamp_double(pos.yCoord, finalBoundingBox.minY, finalBoundingBox.maxY);
                final double ez = MathHelper.clamp_double(pos.zCoord, finalBoundingBox.minZ, finalBoundingBox.maxZ);

                vec = new Vec3(ex, ey, ez);
            }
            break;
            default:
                vec = entityPos.add(0, 0, 0);
                break;
        }

        if (!firstHit || !notOnFirstHit.get()) {
            double minXZ = -0.4;
            double maxXZ = 0.4;
            double minY = -2;
            double maxY = 0.4;

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
            offsetVec = new Vec3(0, 0, 0);
        }

        if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || !onlyUpdateOnMiss.get()) {
            targetVec = new Vec3(vec.xCoord, vec.yCoord, vec.zCoord).add(offsetVec).add(xOffset.get(), yOffset.get(), xOffset.get());
            if (delayed.get() && targetVec != null) {
                positionHistory.offer(targetVec);
            }
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

        pitch = MathHelper.clamp_float(pitch, -90, 90);

        if (!(mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || !onlyRotateOnMiss.get())) {
            yaw = prevRot[0];
            pitch = prevRot[1];
        }

        if (mc.thePlayer.getEntityBoundingBox().intersectsWith(entity.getEntityBoundingBox()) && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            yaw = prevRot[0];
            pitch = prevRot[1];
        }

        prevRot = new float[]{yaw, pitch};
        return new float[]{yaw, pitch};
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        selfPrediction = PlayerUtils.predictPlayerPosition((int) predictTicks.get());
    }
}