package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.GameEvent;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.events.impl.player.*;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.utils.player.ClickHandler;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.MultiBoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.player.*;

import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "KillAura", category = ModuleCategory.Combat)
public class KillAura extends Module {

    // reach
    public final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 4.0f, 1, 8, 0.1f, this);

    // attack
    private final ModeValue clickMode = new ModeValue("Click mode", new String[]{"Legit", "Packet", "PlayerController"}, "Packet", this);
    private final BoolValue smartClicking = new BoolValue("Smart clicking", false, this);
    private final BoolValue ignoreBlocking = new BoolValue("Ignore blocking", true, this);
    private final SliderValue minCPS = new SliderValue("CPS (min)", 12, 0, 20, 1, this);
    private final SliderValue maxCPS = new SliderValue("CPS (max)", 16, 0, 20, 1, this);
    private final SliderValue cpsUpdateDelay = new SliderValue("CPS update delay", 5, 0, 50, 1, this);
    public final BoolValue rayTrace = new BoolValue("RayTrace", false, this);
    private final BoolValue failSwing = new BoolValue("Fail swing", false, this, rayTrace::get);
    private final SliderValue swingRange = new SliderValue("Swing range", 3.5f, 1, 8, 0.1f, this, () -> failSwing.get() && failSwing.canDisplay());

    // autoBlock
    public final BoolValue autoBlock = new BoolValue("AutoBlock", true, this);
    public final SliderValue autoBlockRange = new SliderValue("AutoBlock range", 3.5f, 1, 8, 0.1f, this, autoBlock::get);
    public final ModeValue autoBlockMode = new ModeValue("AutoBlock mode", new String[]{"Fake", "Vanilla", "Release", "VanillaForce", "Blink", "NCP"}, "Vanilla", this, autoBlock::get);
    public final BoolValue unBlockOnRayCastFail = new BoolValue("Unblock on rayCast fail", false, this, () -> autoBlock.get() && rayTrace.get());

    // rotation
    private final ModeValue rotationMode = new ModeValue("Rotation mode", new String[]{"Silent", "Snap"}, "Silent", this);
    private final ModeValue smoothMode = new ModeValue("Smooth mode", new String[]{"Linear", "Lerp", "Bezier", "Exponential", "Test", "None"}, "Linear", this);
    private final SliderValue yawRotationSpeedMin = new SliderValue("Yaw rotation speed (min)", 1, 0.01f, 180, 0.01f, this, () -> !smoothMode.is("Acceleration") && !smoothMode.is("None"));
    private final SliderValue yawRotationSpeedMax = new SliderValue("Yaw rotation speed (max)", 1, 0.01f, 180, 0.01f, this, () -> !smoothMode.is("Acceleration") && !smoothMode.is("None"));
    private final SliderValue pitchRotationSpeedMin = new SliderValue("Pitch rotation speed (min)", 1, 0.01f, 180, 0.01f, this, () -> !smoothMode.is("Acceleration") && !smoothMode.is("None"));
    private final SliderValue pitchRotationSpeedMax = new SliderValue("Pitch rotation speed (max)", 1, 0.01f, 180, 0.01f, this, () -> !smoothMode.is("Acceleration") && !smoothMode.is("None"));
    private final SliderValue midpoint = new SliderValue("Midpoint", 0.3f, 0.01f, 1, 0.01f, this, () -> Objects.equals(smoothMode.get(), "Bezier") && !smoothMode.is("None") || smoothMode.is("Test"));
    private final ModeValue movementFix = new ModeValue("Movement fix", new String[]{"None", "Silent", "Strict"}, "None", this);

    // aim point
    private final ModeValue aimPos = new ModeValue("Aim position", new String[]{"Head", "Torso", "Legs", "Nearest", "Straight", "Assist"}, "Straight", this);
    private final SliderValue yTrim = new SliderValue("Y trim", 0, 0, 0.5f, 0.01f, this);
    private final BoolValue slowDown = new BoolValue("Slow down", false, this);
    private final SliderValue hurtTimeSubtraction = new SliderValue("HurtTime subtraction", 4, 0, 10, 1, this, () -> slowDown.canDisplay() && slowDown.get());
    private final BoolValue pauseRotation = new BoolValue("Pause rotation", false, this);
    private final SliderValue pauseChance = new SliderValue("Pause chance", 5, 1, 25, 1, this, pauseRotation::get);
    private final BoolValue predict = new BoolValue("Rotation prediction", false, this);
    private final SliderValue predictTicks = new SliderValue("Predict ticks", 2, 1, 3, 1, this, () -> predict.get() && predict.canDisplay());
    private final SliderValue simulatedMotionMulti = new SliderValue("Simulated motion multi", 1.5f, 0.1f, 5, 0.1f, this, () -> predict.get() && predict.canDisplay());
    private final SliderValue targetOffset = new SliderValue("Normal target pos offset", 0, -5, 5, 0.01f, this);
    private final BoolValue staticMissOffset = new BoolValue("Static miss offset", true, this);
    private final SliderValue missTargetOffset = new SliderValue("Miss target pos offset", 0, -5, 5, 0.01f, this, staticMissOffset::get);
    private final SliderValue minMissOffset = new SliderValue("Min miss offset", 0, -5, 5, 0.01f, this, () -> !staticMissOffset.get());
    private final SliderValue maxMissOffset = new SliderValue("max miss offset", 0, -5, 5, 0.01f, this, () -> !staticMissOffset.get());
    private final SliderValue missOffsetLerp = new SliderValue("Miss offset lerp", 0.1f, 0.01f, 1, 0.01f, this, () -> !staticMissOffset.get());
    private final BoolValue delayed = new BoolValue("Delayed target pos", false, this);
    private final SliderValue delayedTicks = new SliderValue("Delay ticks", 1, 1, 20, 1, this, () -> delayed.get() && delayed.canDisplay());
    private final BoolValue delayOnHurtTime = new BoolValue("Delay on hurtTime", true, this, () -> delayed.get() && delayed.canDisplay());
    private final SliderValue hurtTime = new SliderValue("Delay hurtTime", 5, 1, 10, 1, this, () -> delayOnHurtTime.get() && delayOnHurtTime.canDisplay());
    private final ModeValue offsetMode = new ModeValue("Offset mode", new String[]{"None", "Gaussian", "Noise", "Drift"}, "None", this);
    private final SliderValue oChance = new SliderValue("Offset chance", 75, 1, 100, 1, this, () -> rotationMode.canDisplay() && offsetMode.is("Gaussian") || offsetMode.is("Noise"));
    private final SliderValue minYawFactor = new SliderValue("Min Yaw Factor", 0.25f, 0, 1, 0.01f, this, () -> rotationMode.canDisplay() && offsetMode.is("Gaussian") || offsetMode.is("Noise"));
    private final SliderValue maxYawFactor = new SliderValue("Max Yaw Factor", 0.25f, 0, 1, 0.01f, this, () -> rotationMode.canDisplay() && offsetMode.is("Gaussian") || offsetMode.is("Noise"));
    private final SliderValue minPitchFactor = new SliderValue("Min Pitch Factor", 0.25f, 0, 1, 0.01f, this, () -> rotationMode.canDisplay() && offsetMode.is("Gaussian") || offsetMode.is("Noise"));
    private final SliderValue maxPitchFactor = new SliderValue("Max Pitch Factor", 0.25f, 0, 1, 0.01f, this, () -> rotationMode.canDisplay() && offsetMode.is("Gaussian") || offsetMode.is("Noise"));
    private final SliderValue xOffset = new SliderValue("Static X offset", 0, -0.4f, 0.4f, 0.01f, this);
    private final SliderValue yOffset = new SliderValue("Static Y offset", 0, -2, 0.4f, 0.01f, this);
    private final BoolValue onlyUpdateOnMiss = new BoolValue("Only update on miss", false, this);
    private final BoolValue onlyRotateOnMiss = new BoolValue("Only rotate on miss", false, this);

    // target
    private final ModeValue targetMode = new ModeValue("Target selection mode", new String[]{"Single", "Switch"}, "Single", this);
    private final ModeValue targetPriority = new ModeValue("Target Priority", new String[]{"None", "Distance", "Health", "HurtTime"}, "Distance", this, () -> targetMode.is("Single"));
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

    private final List<PlayerUtils.PredictProcess> predictProcesses = new ArrayList<>();
    private final Queue<Vec3> positionHistory = new LinkedList<>();
    private final TimerUtils lastSwitchTime = new TimerUtils();
    public List<EntityLivingBase> targets = new ArrayList<>();
    private final TimerUtils pauseTimer = new TimerUtils();
    public static EntityLivingBase currentTarget;
    public static boolean isBlocking = false;
    private Vec3 targetVec;
    public Vec3 currentVec;
    private double lastXOffset;
    private double lastYOffset;
    private double lastZOffset;
    private boolean pause;
    private float derpYaw;
    private double currentXOffset;
    private double currentYOffset;
    private double currentZOffset;
    private float currentMissOffset;
    private boolean shouldRandomize;
    private Vec3 offsetVec = new Vec3(0, 0, 0);
    private float randYawSpeed;
    private float randPitchSpeed;

    @Override
    public void onEnable() {
        lastSwitchTime.reset();
    }

    @Override
    public void onDisable() {
        if (isBlocking) {
            setBlocking(false);
        }

        if (BlinkComponent.blinking) {
            BlinkComponent.dispatch(true);
        }

        currentTarget = null;
        targets.clear();
        positionHistory.clear();
    }

    private void setRotationToTarget(EntityLivingBase target) {
        SmoothMode mode = SmoothMode.valueOf(smoothMode.get());
        MovementCorrection correction = MovementCorrection.valueOf(movementFix.get());

        float hSpeed;
        float vSpeed;

        float hurtTime = target.hurtTime - hurtTimeSubtraction.get();

        if (hurtTime < 1) hurtTime = 1;

        hSpeed = (slowDown.get() ? randYawSpeed / hurtTime : randYawSpeed) * mc.timer.partialTicks;
        vSpeed = (slowDown.get() ? randPitchSpeed / hurtTime : randPitchSpeed) * mc.timer.partialTicks;

        switch (mode) {
            case Linear:
                RotationUtils.setRotation(calcToEntity(target), correction, hSpeed, vSpeed, SmoothMode.Linear, midpoint.get());
                break;
            case Lerp:
                RotationUtils.setRotation(calcToEntity(target), correction, hSpeed, vSpeed, SmoothMode.Lerp, midpoint.get());
                break;
            case Bezier:
                RotationUtils.setRotation(calcToEntity(target), correction, hSpeed, vSpeed, SmoothMode.Bezier, midpoint.get());
                break;
            case Exponential:
                RotationUtils.setRotation(calcToEntity(target), correction, hSpeed, vSpeed, SmoothMode.Exponential, midpoint.get());
                break;
            case Test:
                RotationUtils.setRotation(calcToEntity(target), correction, hSpeed, vSpeed, SmoothMode.Test, midpoint.get());
                break;
            case None:
                RotationUtils.setRotation(calcToEntity(target), correction);
                break;
        }
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

        for (Entity entity : mc.theWorld.loadedEntityList) {
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
                            target = (EntityLivingBase) entity;
                            closestDistance = distanceToEntity;
                        }
                        break;
                    case "Health":
                        EntityLivingBase potentialTarget = (EntityLivingBase) entity;
                        float potentialHealth = PlayerUtils.getActualHealth(potentialTarget);
                        if (potentialHealth < leastHealth) {
                            target = potentialTarget;
                            leastHealth = potentialHealth;
                        }
                        break;
                    case "HurtTime":
                        EntityLivingBase potentialTarget2 = (EntityLivingBase) entity;
                        float potentionalHurtTime = potentialTarget2.hurtTime;
                        if (potentionalHurtTime <= leastHurtTime) {
                            target = potentialTarget2;
                            leastHurtTime = potentionalHurtTime;
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
                ClickHandler.ClickMode mode = ClickHandler.ClickMode.valueOf(clickMode.get());
                ClickHandler.initHandler(minCPS.get(), maxCPS.get(), cpsUpdateDelay.get(), rayTrace.get() && rayTrace.canDisplay(), smartClicking.get(), ignoreBlocking.get(), failSwing.get(), attackRange.get(), swingRange.get(), mode, currentTarget);

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
                switch (rotationMode.get()) {
                    case "Silent":
                        setRotationToTarget(currentTarget);
                        break;
                    case "Derp":
                        MovementCorrection correction = MovementCorrection.valueOf(movementFix.get());
                        derpYaw += MathUtils.randomizeFloat(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()) / 6;

                        RotationUtils.setRotation(new float[]{derpYaw, mc.thePlayer.rotationPitch}, correction, 180, 180, SmoothMode.Linear, midpoint.get());
                        break;
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        shouldRandomize = rand.nextInt(100) <= oChance.get();

        randYawSpeed = MathUtils.randomizeFloat(yawRotationSpeedMin.get(), yawRotationSpeedMax.get());
        randPitchSpeed = MathUtils.randomizeFloat(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get());
    }

    @EventTarget
    public void onRenderTick(Render3DEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (currentTarget != null && targetESP.get()) {
            drawCircle(currentTarget);
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
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent e) {
        e.setRange(attackRange.get());
    }

    public void preAttack() {
        if (extraCheck() && canAutoBlock()) {
            switch (autoBlockMode.get()) {
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
                case "Blink":
                    BlinkComponent.blinking = true;
                    if (isBlocking) {
                        setBlocking(false);
                    }
                    break;
            }
        } else if (isBlocking) {
            setBlocking(false);
        }
    }

    private void onAttack() {
        if (extraCheck() && canAutoBlock()) {
            switch (autoBlockMode.get()) {
                case "NCP":
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
                    setBlocking(true);
                    break;
                case "VanillaForce":
                    vanillaReblockAb(currentTarget);
                    break;
                case "Blink":
                    //interact();
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

    private void vanillaReblockAb(Entity e) {
        setBlocking(true);
        if (!mc.gameSettings.keyBindUseItem.isKeyDown() || !mc.thePlayer.isBlocking()) {
            setBlocking(true);
        }
    }

    private void setBlocking(boolean state) {
        switch (autoBlockMode.get()) {
            case "NCP":
                if (state) {
                    sendPacket(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 255, null, 0.0f, 0.0f, 0.0f));
                } else {
                    sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                }
                break;
            case "Blink":
                if (state) {
                    sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                } else {
                    sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                }
                break;
            default:
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), state);
                break;
        }

        isBlocking = state;
    }

    private boolean canAutoBlock() {
        return isHoldingSword() && autoBlock.get() && PlayerUtils.getDistanceToEntityBox(currentTarget) <= autoBlockRange.get();
    }

    public boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private float[] calcToEntity(EntityLivingBase entity) {
        Vec3 playerPos;

        if (predict.get() && !predictProcesses.isEmpty()) {
            PlayerUtils.PredictProcess predictedProcess = predictProcesses.get(predictProcesses.size() - 1);
            playerPos = new Vec3(predictedProcess.position.xCoord, predictedProcess.position.yCoord + mc.thePlayer.getEyeHeight(), predictedProcess.position.zCoord);
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

        AxisAlignedBB intitalBoundingBox = entity.getHitbox().offset(prediction).contract(0, yTrim.get(), 0);
        AxisAlignedBB bb = entity.getHitbox().offset(prediction).contract(0, yTrim.get(), 0);

        AxisAlignedBB finalBoundingBox = new AxisAlignedBB(bb.minX, bb.minY + entity.height * 0.45, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);

        Vec3 boxCenter = intitalBoundingBox.getCenter();
        Vec3 entityPos = new Vec3(boxCenter.xCoord, intitalBoundingBox.minY, boxCenter.zCoord);
        Vec3 vec;

        switch (aimPos.get()) {
            case "Head": {
                vec = entityPos.add(0.0, entity.getEyeHeight(), 0.0);
                break;
            }
            case "Torso": {
                vec = entityPos.add(0.0, entity.height * 0.75, 0.0);
                break;
            }
            case "Legs": {
                vec = entityPos.add(0.0, entity.height * 0.45, 0.0);
                break;
            }
            case "Nearest": {
                vec = RotationUtils.getBestHitVec(entity);
                break;
            }
            case "Straight": {
                final double ex = (finalBoundingBox.maxX + finalBoundingBox.minX) / 2;
                final double ey = MathHelper.clamp_double(playerPos.yCoord, finalBoundingBox.minY, finalBoundingBox.maxY);
                final double ez = (finalBoundingBox.maxZ + finalBoundingBox.minZ) / 2;

                vec = new Vec3(ex, ey, ez);
                break;
            }
            case "Assist": {
                final Vec3 pos = playerPos.add(mc.thePlayer.getLookVec());

                final double ex = MathHelper.clamp_double(pos.xCoord, finalBoundingBox.minX, finalBoundingBox.maxX);
                final double ey = MathHelper.clamp_double(pos.yCoord, finalBoundingBox.minY, finalBoundingBox.maxY);
                final double ez = MathHelper.clamp_double(pos.zCoord, finalBoundingBox.minZ, finalBoundingBox.maxZ);

                vec = new Vec3(ex, ey, ez);
                break;
            }
            default:
                vec = entityPos.add(0, 0, 0);
                break;
        }

        switch (offsetMode.get()) {
            case "Gaussian": {
                double minXZ = -0.4;
                double maxXZ = 0.4;
                double minY = -2;
                double maxY = 0.4;

                double meanXZ = (minXZ + maxXZ) / 2;
                double stdDevXZ = (maxXZ - minXZ) / 4;
                double meanY = (minY + maxY) / 2;
                double stdDevY = (maxY - minY) / 4;

                double yawFactor = MathUtils.randomizeDouble(minYawFactor.get(), maxYawFactor.get());
                double pitchFactor = MathUtils.randomizeDouble(minPitchFactor.get(), maxPitchFactor.get());

                double xOffset = ThreadLocalRandom.current().nextGaussian(meanXZ, stdDevXZ) * yawFactor;
                double yOffset = ThreadLocalRandom.current().nextGaussian(meanY, stdDevY) * pitchFactor;
                double zOffset = ThreadLocalRandom.current().nextGaussian(meanXZ, stdDevXZ) * yawFactor;

                if (shouldRandomize) {
                    offsetVec = MathUtils.interpolate(offsetVec, new Vec3(xOffset, yOffset, zOffset), mc.timer.partialTicks / 2);

                    lastXOffset = (float) xOffset;
                    lastYOffset = (float) yOffset;
                    lastZOffset = (float) zOffset;
                } else {
                    offsetVec = MathUtils.interpolate(offsetVec, new Vec3(lastXOffset, lastYOffset, lastZOffset), mc.timer.partialTicks / 2);
                }
            }
            break;
            case "Noise": {
                double minXZ = -0.4;
                double maxXZ = 0.4;
                double minY = -2;
                double maxY = 0.4;

                double yawFactor = MathUtils.randomizeDouble(minYawFactor.get(), maxYawFactor.get());
                double pitchFactor = MathUtils.randomizeDouble(minPitchFactor.get(), maxPitchFactor.get());

                double xOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;
                double yOffset = MathUtils.randomizeDouble(minY, maxY) * pitchFactor;
                double zOffset = MathUtils.randomizeDouble(minXZ, maxXZ) * yawFactor;

                if (shouldRandomize) {
                    offsetVec = MathUtils.interpolate(offsetVec, new Vec3(xOffset, yOffset, zOffset), mc.timer.partialTicks / 2);

                    lastXOffset = (float) xOffset;
                    lastYOffset = (float) yOffset;
                    lastZOffset = (float) zOffset;
                } else {
                    offsetVec = MathUtils.interpolate(offsetVec, new Vec3(lastXOffset, lastYOffset, lastZOffset), mc.timer.partialTicks / 2);
                }
            }
            break;
            case "Drift":
                if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                    double smoothYawFactor = MathUtils.randomizeDouble(0.4, 0.6);
                    double smoothPitchFactor = MathUtils.randomizeDouble(0.2, 0.4);

                    double smoothYawOffset = (rand.nextDouble() - 0.5) * smoothYawFactor;
                    double smoothPitchOffset = (rand.nextDouble() - 0.5) * smoothPitchFactor;

                    float smoothedX = (float) (lastXOffset * 0.75 + smoothYawOffset * 0.25);
                    float smoothedY = (float) (lastYOffset * 0.75 + smoothPitchOffset * 0.25);
                    float smoothedZ = (float) (lastZOffset * 0.75 + smoothYawOffset * 0.25);

                    currentXOffset += smoothedX;
                    currentYOffset += smoothedY;
                    currentZOffset += smoothedZ;

                    lastXOffset = smoothedX;
                    lastYOffset = smoothedY;
                    lastZOffset = smoothedZ;

                    vec.xCoord += currentXOffset;
                    vec.yCoord += currentYOffset;
                    vec.zCoord += currentZOffset;
                } else {
                    vec.xCoord += currentXOffset = MathUtils.interpolate(currentXOffset, 0, 0.75f);
                    vec.yCoord += currentYOffset = MathUtils.interpolate(currentYOffset, 0, 0.75f);
                    vec.zCoord += currentZOffset = MathUtils.interpolate(currentZOffset, 0, 0.75f);
                }
                break;
            default:
                offsetVec = new Vec3(0, 0, 0);
        }

        if ((mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY && onlyUpdateOnMiss.get()) || !onlyUpdateOnMiss.get()) {
            targetVec = new Vec3(vec.xCoord, vec.yCoord, vec.zCoord).add(offsetVec).add(xOffset.get(), yOffset.get(), xOffset.get());
        }

        if (delayed.get()) {
            positionHistory.add(targetVec);

            while (positionHistory.size() > delayedTicks.get()) {
                positionHistory.poll();
            }

            if (positionHistory.size() < delayedTicks.get()) {
                currentVec = targetVec;
            }

            if (positionHistory.size() >= delayedTicks.get()) {
                if ((delayOnHurtTime.get() && entity.hurtTime >= hurtTime.get()) || !delayOnHurtTime.get()) {
                    currentVec = positionHistory.poll();
                } else {
                    currentVec = targetVec;
                }
            }
        } else {
            positionHistory.clear();
            currentVec = targetVec;
        }

        assert currentVec != null;

        double deltaX = currentVec.xCoord - playerPos.xCoord;
        double deltaY = currentVec.yCoord - playerPos.yCoord;
        double deltaZ = currentVec.zCoord - playerPos.zCoord;

        float yaw = (float) -(Math.atan2(deltaX, deltaZ) * (180.0 / Math.PI));
        float pitch = (float) (-Math.toDegrees(Math.atan2(deltaY, Math.hypot(deltaX, deltaZ))));

        pitch = MathHelper.clamp_float(pitch, -90, 90);

        if (pauseRotation.get() && rand.nextInt(100) <= pauseChance.get() && !pause) {
            pauseTimer.reset();
            pause = true;
        }

        if (pause) {
            if (!pauseTimer.hasTimeElapsed(100)) {
                yaw = RotationUtils.previousRotation[0];
                pitch = RotationUtils.previousRotation[1];
            } else {
                pause = false;
            }
        }

        if (!((mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY && onlyRotateOnMiss.get()) || !onlyRotateOnMiss.get())) {
            yaw = RotationUtils.previousRotation[0];
            pitch = RotationUtils.previousRotation[1];
        }

        return new float[]{yaw, pitch};
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        predictProcesses.clear();

        SimulatedPlayer simulatedPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput, simulatedMotionMulti.get());

        simulatedPlayer.rotationYaw = RotationUtils.currentRotation != null ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < predictTicks.get(); i++) {
            simulatedPlayer.tick();
            predictProcesses.add(
                    new PlayerUtils.PredictProcess(
                            simulatedPlayer.getPos(),
                            simulatedPlayer.fallDistance,
                            simulatedPlayer.onGround,
                            simulatedPlayer.isCollidedHorizontally
                    )
            );
        }
    }

    public static void drawCircle(final Entity entity) {
        GL11.glPushMatrix();
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        GL11.glEnable(2832);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glHint(3154, 4354);
        GL11.glHint(3155, 4354);
        GL11.glHint(3153, 4354);
        GL11.glDepthMask(false);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        final double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - (mc.getRenderManager()).renderPosX;
        final double y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - (mc.getRenderManager()).renderPosY) + Math.sin(System.currentTimeMillis() / 2E+2) + 0.8;
        final double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - (mc.getRenderManager()).renderPosZ;

        Color c;

        for (float i = 0; i < Math.PI * 2; i += (float) (Math.PI * 2 / 64.F)) {
            final double vecX = x + 0.67 * Math.cos(i);
            final double vecZ = z + 0.67 * Math.sin(i);

            c = new Color(Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1));

            GL11.glColor4f(c.getRed() / 255.F,
                    c.getGreen() / 255.F,
                    c.getBlue() / 255.F,
                    0
            );
            GL11.glVertex3d(vecX, y - Math.cos(System.currentTimeMillis() / 2E+2) / 2.0F, vecZ);
            GL11.glColor4f(c.getRed() / 255.F,
                    c.getGreen() / 255.F,
                    c.getBlue() / 255.F,
                    0.85F
            );
            GL11.glVertex3d(vecX, y, vecZ);
        }

        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDepthMask(true);
        GL11.glEnable(2929);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableCull();
        GL11.glDisable(2848);
        GL11.glDisable(2848);
        GL11.glEnable(2832);
        GL11.glEnable(3553);
        GL11.glPopMatrix();
        GL11.glColor3f(255, 255, 255);
    }
}
