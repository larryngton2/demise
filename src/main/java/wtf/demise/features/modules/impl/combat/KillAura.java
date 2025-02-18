package wtf.demise.features.modules.impl.combat;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.utils.player.*;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

@ModuleInfo(name = "KillAura", category = ModuleCategory.Combat)
public class KillAura extends Module {

    //reach
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 4.0f, 1, 8, 0.1f, this);

    // attack
    private final ModeValue clickMode = new ModeValue("Click mode", new String[]{"Legit", "Packet", "PlayerController"}, "Packet", this);
    //private final BoolValue smartClicking = new BoolValue("Smart clicking", false, this);
    private final BoolValue noSwing = new BoolValue("No swing", false, this, () -> Objects.equals(clickMode.get(), "Packet"));
    private final SliderValue attackDelayMin = new SliderValue("Attack delay (min)", 25, 25, 1000, 25, this);
    private final SliderValue attackDelayMax = new SliderValue("Attack delay (max)", 25, 25, 1000, 25, this);
    private final BoolValue extraClicks = new BoolValue("Extra clicks", false, this);
    private final SliderValue eChance = new SliderValue("Chance", 50, 1, 100, 1, this, extraClicks::get);
    private final SliderValue clicks = new SliderValue("Extra click count", 1, 1, 10, 1, this, extraClicks::get);
    private final BoolValue rayCast = new BoolValue("RayCast", false, this);
    private final BoolValue failSwing = new BoolValue("Fail swing", false, this, rayCast::get);

    // autoBlock
    private final BoolValue autoBlock = new BoolValue("AutoBlock", true, this);
    private final SliderValue autoBlockRange = new SliderValue("AutoBlock range", 3.5f, 1, 8, 0.1f, this, autoBlock::get);
    private final ModeValue autoBlockMode = new ModeValue("AutoBlock mode", new String[]{"Fake", "Vanilla", "Release", "VanillaForce", "Smart", "Blink"}, "Vanilla", this, autoBlock::get);
    private final BoolValue unBlockOnRayCastFail = new BoolValue("Unblock on rayCast fail", false, this, () -> autoBlock.get() && rayCast.get());

    // rotation
    private final ModeValue rotationMode = new ModeValue("Rotation mode", new String[]{"Silent", "None"}, "Silent", this);
    private final ModeValue aimPos = new ModeValue("Aim position", new String[]{"Head", "Torso", "Legs", "Nearest", "Straight", "Dynamic"}, "Straight", this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue xzTrim = new SliderValue("XZ trim", 0, 0, 0.5f, 0.01f, this);
    private final SliderValue yTrim = new SliderValue("Y trim", 0, 0, 0.5f, 0.01f, this);
    private final ModeValue smoothMode = new ModeValue("Smooth mode", new String[]{"Linear", "Lerp", "LerpLimit", "Correlation"}, "Linear", this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue yawRotationSpeedMin = new SliderValue("Yaw rotation speed (min)", 180, 0.01f, 180, 0.01f, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue yawRotationSpeedMax = new SliderValue("Yaw rotation speed (max)", 180, 0.01f, 180, 0.01f, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue pitchRotationSpeedMin = new SliderValue("Pitch rotation speed (min)", 180, 0.01f, 180, 0.01f, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue pitchRotationSpeedMax = new SliderValue("Pitch rotation speed (max)", 180, 0.01f, 180, 0.01f, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue threshold = new SliderValue("Threshold", 0.5f, 0.01f, 1, 0.01f, this, () -> Objects.equals(smoothMode.get(), "Correlation"));
    private final SliderValue rangeToLimit = new SliderValue("Range to limit (<)", 1.5f, 0, 8, 0.1f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final SliderValue limitYawSpeedMin = new SliderValue("Yaw limit speed (min)", 180, 0.01f, 180, 0.01f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final SliderValue limitYawSpeedMax = new SliderValue("Yaw limit speed (max)", 180, 0.01f, 180, 0.01f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final SliderValue limitPitchSpeedMin = new SliderValue("Pitch limit speed (min)", 180, 0.01f, 180, 0.01f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final SliderValue limitPitchSpeedMax = new SliderValue("Pitch limit speed (max)", 180, 0.01f, 180, 0.01f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final BoolValue movementFix = new BoolValue("Movement fix", false, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final BoolValue pauseRotation = new BoolValue("Pause rotation", false, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue pauseChance = new SliderValue("Pause chance", 5, 1, 25, 1, this, () -> !Objects.equals(rotationMode.get(), "None") && pauseRotation.get());
    private final BoolValue delayed = new BoolValue("Delayed target pos", false, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue delayedTicks = new SliderValue("Delay ticks", 2, 1, 20, 1, this, () -> delayed.get() && delayed.canDisplay());
    private final BoolValue predict = new BoolValue("Rotation prediction", false, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue predictTicks = new SliderValue("Predict ticks", 2, 0, 3, 1, this, () -> predict.get() && predict.canDisplay());
    private final BoolValue renderPredictPos = new BoolValue("Render predicted pos", false, this, () -> predict.get() && predict.canDisplay() && predictTicks.get() != 0);

    // offset
    private final ModeValue offsetMode = new ModeValue("Offset mode", new String[]{"None", "Gaussian", "SinCos", "Intave"}, "None", this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue oChance = new SliderValue("Offset chance", 75, 1, 100, 1, this, () -> rotationMode.canDisplay() && Objects.equals(offsetMode.get(), "Gaussian"));
    private final SliderValue minPitchFactor = new SliderValue("Min Pitch Factor", 0.25f, 0, 1, 0.01f, this, () -> rotationMode.canDisplay() && Objects.equals(offsetMode.get(), "Gaussian"));
    private final SliderValue maxPitchFactor = new SliderValue("Max Pitch Factor", 0.25f, 0, 1, 0.01f, this, () -> rotationMode.canDisplay() && Objects.equals(offsetMode.get(), "Gaussian"));
    private final SliderValue minYawFactor = new SliderValue("Min Yaw Factor", 0.25f, 0, 1, 0.01f, this, () -> rotationMode.canDisplay() && Objects.equals(offsetMode.get(), "Gaussian"));
    private final SliderValue maxYawFactor = new SliderValue("Max Yaw Factor", 0.25f, 0, 1, 0.01f, this, () -> rotationMode.canDisplay() && Objects.equals(offsetMode.get(), "Gaussian"));
    private final SliderValue frequency = new SliderValue("SpeedSinCos", 1.5f, 0f, 5.0f, 0.01f, this, () -> Objects.equals(offsetMode.get(), "SinCos"));
    private final SliderValue yStrengthAimPattern = new SliderValue("YStrengthAmplitudeSinCos", 3.5f, 0f, 15.0f, 0.01f, this, () -> Objects.equals(offsetMode.get(), "SinCos"));
    private final SliderValue xStrengthAimPattern = new SliderValue("XStrengthAmplitudeSinCos", 3.5f, 0f, 15.0f, 0.01f, this, () -> Objects.equals(offsetMode.get(), "SinCos"));
    private final SliderValue yawStrengthAddon = new SliderValue("Yaw Strength Randomize", 5f, 1, 35f, this, () -> Objects.equals(offsetMode.get(), "SinCos"));
    private final SliderValue pitchStrengthAddon = new SliderValue("Pitch Strength Randomize", 5f, 1, 35f, this, () -> Objects.equals(offsetMode.get(), "SinCos"));

    //target
    private final ModeValue targetMode = new ModeValue("Target selection mode", new String[]{"Single", "Switch", "Multi"}, "Single", this);
    private final ModeValue targetPriority = new ModeValue("Target Priority", new String[]{"None", "Distance", "Health"}, "Distance", this, () -> targetMode.is("Single"));
    private final SliderValue targetSwitchDelay = new SliderValue("Target Switch Delay (ms)", 500, 50, 1000, 50, this, () -> targetMode.is("Switch"));
    private final BoolValue botCheck = new BoolValue("Bot Check", false, this);

    // visual
    private final BoolValue targetESP = new BoolValue("Target ESP", false, this);
    private final BoolValue targetOnPlayer = new BoolValue("Target on player", false, this);
    private final SliderValue dotScale = new SliderValue("Target scale", 0.04f, 0.01f, 1, 0.01f, this, targetOnPlayer::get);

    public final List<PlayerUtils.PredictProcess> predictProcesses = new ArrayList<>();
    private final Queue<Vec3> positionHistory = new LinkedList<>();
    private final TimerUtils lastTargetTime = new TimerUtils();
    private final TimerUtils lastSwitchTime = new TimerUtils();
    public List<EntityLivingBase> targets = new ArrayList<>();
    private final TimerUtils pauseTimer = new TimerUtils();
    private Vec3 positionOnPlayer, lastPositionOnPlayer;
    public static Entity currentTarget = null;
    private final Random rand = new Random();
    public static boolean isBlocking = false;
    private boolean blink = false;
    private int blockTicks = 0;
    public Vec3 currentVec;
    double lastPitchOffset;
    private boolean pause;
    public Vec3 targetVec;
    double lastYawOffset;
    public Vec3 prevVec;

    @Override
    public void onEnable() {
        lastTargetTime.reset();
        lastSwitchTime.reset();
    }

    @Override
    public void onDisable() {
        resetBlocking();
        blink = false;
        currentTarget = null;
        targets.clear();
        predictProcesses.clear();
        positionHistory.clear();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        this.setTag(targetMode.get());

        switch (targetMode.get()) {
            case "Single":
                currentTarget = findTarget();
                break;
            case "Switch":
                if (lastSwitchTime.hasTimeElapsed(targetSwitchDelay.get())) {
                    currentTarget = findNextTarget();
                }
                break;
            case "Multi":
                currentTarget = findNextTarget();

                targets.clear();
                for (Entity target : mc.theWorld.loadedEntityList) {
                    if (target instanceof EntityLivingBase target2 && PlayerUtils.getDistanceToEntityBox(target) <= searchRange.get() && target != mc.thePlayer) {
                        targets.add(target2);
                    }
                }
                break;
        }

        if (currentTarget != null) {
            lastSwitchTime.reset();

            if (nullTargetCheck()) {
                resetBlocking();
                currentTarget = null;
            }

            if (!isWithinAttackRange()) {
                resetBlocking();
            }

            sendAttack();
            handleAutoBlock();

            if (Objects.equals(rotationMode.get(), "Silent") && PlayerUtils.getDistanceToEntityBox(currentTarget) <= searchRange.get()) {
                setRotationToTarget(currentTarget);
            }
        } else {
            predictProcesses.clear();
            positionHistory.clear();

            resetBlocking();
        }

        if (Objects.equals(clickMode.get(), "Packet")) {
            targets.clear();
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityLivingBase && PlayerUtils.getDistanceToEntityBox(entity) <= searchRange.get()) {
                    targets.add((EntityLivingBase) entity);
                }
            }
        }

        targets.removeIf(target -> nullTargetCheck());
    }

    private void setRotationToTarget(Entity target) {
        SmoothMode mode = SmoothMode.valueOf(smoothMode.get());
        MovementCorrection correction = movementFix.get() ? MovementCorrection.SILENT : MovementCorrection.OFF;

        switch (mode) {
            case Linear:
                RotationUtils.setRotation(calcToEntity((EntityLivingBase) target), correction,
                        MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()),
                        MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()),
                        SmoothMode.Linear);
                break;
            case Lerp:
                RotationUtils.setRotation(calcToEntity((EntityLivingBase) target), correction,
                        MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()),
                        MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()),
                        SmoothMode.Lerp);
                break;
            case LerpLimit:
                RotationUtils.setRotation(calcToEntity((EntityLivingBase) target), correction,
                        MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()),
                        MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()),
                        SmoothMode.LerpLimit,
                        MathUtils.randomizeInt(limitYawSpeedMin.get(), limitYawSpeedMax.get()),
                        MathUtils.randomizeInt(limitPitchSpeedMin.get(), limitPitchSpeedMax.get()),
                        rangeToLimit.get() + 0.4f, target);
                break;
            case Correlation:
                RotationUtils.setRotation(calcToEntity((EntityLivingBase) target), correction,
                        MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()),
                        MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()),
                        SmoothMode.Correlation,
                        MathUtils.randomizeInt(limitYawSpeedMin.get(), limitYawSpeedMax.get()),
                        MathUtils.randomizeInt(limitPitchSpeedMin.get(), limitPitchSpeedMax.get()),
                        rangeToLimit.get() + 0.4f, target, threshold.get());
        }
    }

    private EntityLivingBase findNextTarget() {
        List<Entity> targets = new ArrayList<>();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                if (botCheck.get() && getModule(AntiBot.class).isBot((EntityPlayer) entity)) {
                    continue;
                }

                if (PlayerUtils.isInTeam(entity)) {
                    continue;
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

        for (Entity entity : mc.theWorld.loadedEntityList) {
            double distanceToEntity = PlayerUtils.getDistanceToEntityBox(entity);

            if (entity instanceof EntityPlayer && entity != mc.thePlayer && !Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity) && distanceToEntity <= searchRange.get()) {
                if (botCheck.get() && getModule(AntiBot.class).isBot((EntityPlayer) entity)) {
                    continue;
                }

                if (PlayerUtils.isInTeam(entity)) {
                    continue;
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
                }
            }
        }

        return target;
    }

    @EventTarget
    public void onRenderTick(Render3DEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (currentTarget != null) {
            if (positionOnPlayer != null && lastPositionOnPlayer != null && targetOnPlayer.get()) {
                Vec3 interpolatedPosition = new Vec3(
                        (positionOnPlayer.xCoord - lastPositionOnPlayer.xCoord) * mc.timer.renderPartialTicks + lastPositionOnPlayer.xCoord,
                        (positionOnPlayer.yCoord - lastPositionOnPlayer.yCoord) * mc.timer.renderPartialTicks + lastPositionOnPlayer.yCoord,
                        (positionOnPlayer.zCoord - lastPositionOnPlayer.zCoord) * mc.timer.renderPartialTicks + lastPositionOnPlayer.zCoord);
                RenderUtils.renderBreadCrumb(interpolatedPosition, dotScale.get());
            }

            if (targetESP.get()) {
                if (targetMode.is("Multi")) {
                    for (EntityLivingBase target : targets) {
                        drawCircle(target);
                    }
                } else {
                    drawCircle(currentTarget);
                }
            }
        }

        if (predict.get() && mc.gameSettings.thirdPersonView != 0 && renderPredictPos.get()) {
            double x = predictProcesses.get(predictProcesses.size() - 1).position.xCoord - mc.getRenderManager().viewerPosX;
            double y = predictProcesses.get(predictProcesses.size() - 1).position.yCoord - mc.getRenderManager().viewerPosY;
            double z = predictProcesses.get(predictProcesses.size() - 1).position.zCoord - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(box.minX - mc.thePlayer.posX + x, box.minY - mc.thePlayer.posY + y, box.minZ - mc.thePlayer.posZ + z, box.maxX - mc.thePlayer.posX + x, box.maxY - mc.thePlayer.posY + y, box.maxZ - mc.thePlayer.posZ + z);
            RenderUtils.drawAxisAlignedBB(axis, false, true, Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1));
        }
    }

    private void sendAttack() {
        if (!isWithinAttackRange()) {
            resetBlocking();
            return;
        }

        if (!isAttackReady()) {
            return;
        }

        if (rayCast.get() && (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY)) {
            handleFailSwing(mc.objectMouseOver);
        } else {
            attack();
        }

        lastTargetTime.reset();
    }

    private boolean isWithinAttackRange() {
        return PlayerUtils.getDistanceToEntityBox(currentTarget) <= attackRange.get();
    }

    private boolean isAttackReady() {
        long delay = MathUtils.nextInt((int) attackDelayMin.get(), (int) attackDelayMax.get());
        return lastTargetTime.hasTimeElapsed(delay);
    }

    private void handleFailSwing(MovingObjectPosition movingObjectPosition) {
        if (failSwing.get() && failSwing.canDisplay()) {
            switch (clickMode.get()) {
                case "Packet":
                    if (!noSwing.get()) {
                        AttackOrder.sendConditionalSwing(movingObjectPosition);
                    }
                    break;
                case "Legit":
                    KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
                    break;
                case "PlayerController":
                    AttackOrder.sendConditionalSwing(movingObjectPosition);
                    break;
            }
        }

        if (unBlockOnRayCastFail.get()) {
            resetBlocking();
        }

        lastTargetTime.reset();
    }

    private boolean nullTargetCheck() {
        return currentTarget.isDead || PlayerUtils.getDistanceToEntityBox(currentTarget) > searchRange.get() || currentTarget.getEntityWorld() != mc.thePlayer.getEntityWorld();
    }

    private void handleAutoBlock() {
        if (canAutoBlock()) {
            switch (autoBlockMode.get()) {
                case "None":
                    resetBlocking();
                    break;
                case "Fake":
                    isBlocking = true;
                    break;
                case "Vanilla":
                    setBlocking(true);
                    break;
                case "Release":
                    releaseAb((EntityLivingBase) currentTarget);
                    break;
                case "VanillaForce":
                    vanillaReblockAb(currentTarget);
                    break;
                case "Smart":
                    smartAb((EntityLivingBase) currentTarget);
                    break;
                case "Blink":
                    blinkAb(currentTarget);
                    break;
            }
        } else {
            resetBlocking();
        }
    }

    private void resetBlocking() {
        if (isBlocking && isHoldingSword()) {
            setBlocking(false);
            mc.thePlayer.stopUsingItem();
        }
    }

    private boolean shouldClick() {
        if (mc.thePlayer.hurtTime != 0 || mc.thePlayer.motionY < 0) {
            return true;
        }

        if (currentTarget instanceof EntityLivingBase) {
            return ((EntityLivingBase) currentTarget).hurtTime <= 3;
        }

        return false;
    }

    private boolean canAutoBlock() {
        return isHoldingSword() && autoBlock.get() && PlayerUtils.getDistanceToEntityBox(currentTarget) <= autoBlockRange.get();
    }

    private void attack() {
        int attackCount = 1;

        if (extraClicks.get() && rand.nextInt(100) <= eChance.get()) {
            attackCount = (int) clicks.get() + 1;
        }

        if (targetMode.is("Multi")) {
            for (EntityLivingBase target : targets) {
                for (int i = 0; i < attackCount; i++) {
                    switch (clickMode.get()) {
                        case "Packet":
                            if (clickMode.get().equals("Packet")) {
                                if (ViaLoadingBase.getInstance().getTargetVersion().isOlderThanOrEqualTo(ProtocolVersion.v1_8)) {
                                    if (!noSwing.get()) {
                                        mc.thePlayer.swingItem();
                                    }
                                    PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                                } else {
                                    PacketUtils.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                                    if (!noSwing.get()) {
                                        mc.thePlayer.swingItem();
                                    }
                                }
                            }
                            break;
                        case "PlayerController":
                            AttackOrder.sendFixedAttack(mc.thePlayer, target);
                            break;
                    }
                }
            }
        } else {
            for (int i = 0; i < attackCount; i++) {
                switch (clickMode.get()) {
                    case "Legit":
                        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
                        break;
                    case "Packet":
                        if (ViaLoadingBase.getInstance().getTargetVersion().isOlderThanOrEqualTo(ProtocolVersion.v1_8)) {
                            if (!noSwing.get()) {
                                mc.thePlayer.swingItem();
                            }
                            PacketUtils.sendPacket(new C02PacketUseEntity(currentTarget, C02PacketUseEntity.Action.ATTACK));
                        } else {
                            PacketUtils.sendPacket(new C02PacketUseEntity(currentTarget, C02PacketUseEntity.Action.ATTACK));
                            if (!noSwing.get()) {
                                mc.thePlayer.swingItem();
                            }
                        }
                        break;
                    case "PlayerController":
                        AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
                        break;
                }
            }
        }
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent e) {
        if (Objects.equals(clickMode.get(), "Legit")) {
            e.setRange(attackRange.get());
        }

        if (!getModule(HitBox.class).isEnabled()) {
            e.setExpand(-0.1f);
        }
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (e.isPre()) {
            MovingObjectPosition movingObjectPosition = PlayerUtils.getMouseOver(RotationUtils.serverRotation[0], RotationUtils.serverRotation[1], (float) (attackRange.get() + 0.4));

            if (movingObjectPosition == null) {
                return;
            }

            final Vec3 rayCast = Objects.requireNonNull(movingObjectPosition).hitVec;
            if (rayCast == null) return;
            lastPositionOnPlayer = positionOnPlayer;
            positionOnPlayer = rayCast;
        }
    }

    @EventTarget
    public void onSendPacket(PacketEvent e) {
        if (blink) {
            PingSpoofComponent.blink();
        } else {
            PingSpoofComponent.dispatch();
        }
    }

    public boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private void releaseAb(EntityLivingBase e) {
        setBlocking(e.hurtTime >= 5 || isWithinAttackRange());
    }

    private void vanillaReblockAb(Entity e) {
        setBlocking(true);
        if (!mc.gameSettings.keyBindUseItem.isKeyDown() || !mc.thePlayer.isBlocking()) {
            setBlocking(true);
        }
    }

    private void smartAb(EntityLivingBase e) {
        setBlocking(((mc.thePlayer.hurtTime <= 5 && mc.thePlayer.hurtTime != 0) && mc.thePlayer.motionY >= 0) || e.hurtTime > 5);
    }

    private void blinkAb(Entity e) {
        if (blockTicks >= 3) {
            blockTicks = 0;
        } else {
            blockTicks++;
        }

        switch (blockTicks) {
            case 1:
                blink = true;
                setBlocking(false);
                break;
            case 2:
                setBlocking(true);
                blink = false;
                break;
        }
    }

    private void setBlocking(boolean state) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), state);
        isBlocking = state;
    }

    public float[] calcToEntity(EntityLivingBase entity) {
        prevVec = currentVec;
        float yaw;
        float pitch;

        Vec3 playerPos;
        if (predict.get() && !predictProcesses.isEmpty()) {
            PlayerUtils.PredictProcess predictedProcess = predictProcesses.get(predictProcesses.size() - 1);
            playerPos = new Vec3(predictedProcess.position.xCoord, predictedProcess.position.yCoord + mc.thePlayer.getEyeHeight(), predictedProcess.position.zCoord);
        } else {
            playerPos = mc.thePlayer.getPositionEyes(1);
        }

        Vec3 entityPos = entity.getPositionVector();
        AxisAlignedBB entityBoundingBox = entity.getEntityBoundingBox().contract(xzTrim.get(), yTrim.get(), xzTrim.get());

        switch (aimPos.get()) {
            case "Head":
                targetVec = entityPos.add(0.0, entity.getEyeHeight(), 0.0);
                break;
            case "Torso":
                targetVec = entityPos.add(0.0, entity.height * 0.75, 0.0);
                break;
            case "Legs":
                targetVec = entityPos.add(0.0, entity.height * 0.45, 0.0);
                break;
            case "Nearest":
                targetVec = RotationUtils.getBestHitVec(entity);
                break;
            case "Straight":
                final double ex = (entityBoundingBox.maxX + entityBoundingBox.minX) / 2;
                final double ey = MathHelper.clamp_double(playerPos.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY);
                final double ez = (entityBoundingBox.maxZ + entityBoundingBox.minZ) / 2;

                targetVec = new Vec3(ex, ey, ez);
                break;
            case "Dynamic":
                double targetY = entity.posY + entity.getEyeHeight();

                if (mc.thePlayer.posY < entity.posY) {
                    final double exx = (entityBoundingBox.maxX + entityBoundingBox.minX) / 2;
                    final double eyy = MathHelper.clamp_double(playerPos.yCoord, entityBoundingBox.minY, entityBoundingBox.maxY);
                    final double ezz = (entityBoundingBox.maxZ + entityBoundingBox.minZ) / 2;

                    targetVec = new Vec3(exx, eyy, ezz);
                    break;
                } else if (mc.thePlayer.posY == entity.posY) {
                    targetY = entity.posY + entity.height * 0.85;
                }

                targetVec = entityPos.add(0.0, targetY - entity.posY, 0.0);
                break;
        }

        if (delayed.get()) {
            positionHistory.add(targetVec);

            while (positionHistory.size() > delayedTicks.get()) {
                positionHistory.poll();
            }

            if (positionHistory.size() >= delayedTicks.get()) {
                currentVec = positionHistory.poll();
            } else {
                currentVec = targetVec;
            }
        } else {
            positionHistory.clear();
            currentVec = targetVec;
        }

        // positionHistory can't be 0 at this point, therefore currentVec can't be null. fuck you intellij.
        double deltaX = currentVec.xCoord - playerPos.xCoord;
        double deltaY = currentVec.yCoord - playerPos.yCoord;
        double deltaZ = currentVec.zCoord - playerPos.zCoord;

        yaw = (float) -(Math.atan2(deltaX, deltaZ) * (180.0 / Math.PI));
        pitch = (float) (-Math.toDegrees(Math.atan2(deltaY, Math.hypot(deltaX, deltaZ))));

        switch (offsetMode.get()) {
            case "Gaussian":
                double yawFactor = MathUtils.randomizeDouble(minYawFactor.get(), maxYawFactor.get()) * 20;
                double pitchFactor = MathUtils.randomizeDouble(minPitchFactor.get(), maxPitchFactor.get()) * 20;

                double yawOffset = rand.nextGaussian(0.00942273861037109, 0.23319837528201348) * yawFactor;
                double pitchOffset = rand.nextGaussian(0.30075078007595923, 0.3492437109081718) * pitchFactor;

                if (rand.nextInt(100) <= oChance.get()) {
                    yaw += (float) yawOffset;
                    pitch += (float) pitchOffset;

                    lastYawOffset = yawOffset;
                    lastPitchOffset = pitchOffset;
                } else {
                    yaw += (float) lastYawOffset;
                    pitch += (float) lastPitchOffset;
                }
                break;
            case "SinCos":
                double time = System.currentTimeMillis() / 1000.0D;
                double frequency = this.frequency.get();
                double yawAmplitude = this.xStrengthAimPattern.get();
                double pitchAmplitude = this.yStrengthAimPattern.get();

                yaw += (float) (Math.sin(time * frequency) * yawAmplitude);
                pitch += (float) (Math.cos(time * frequency) * pitchAmplitude);

                yaw += (float) MathUtils.randomizeDouble(-this.yawStrengthAddon.get(), this.yawStrengthAddon.get());
                pitch += (float) MathUtils.randomizeDouble(-this.pitchStrengthAddon.get(), this.pitchStrengthAddon.get());
                break;
            case "Intave":
                boolean dynamicCheck = entity.hurtTime >= 7;

                double initialYawFactor = MathUtils.randomizeDouble(0.7, 0.8) * 30;
                double initialPitchFactor = MathUtils.randomizeDouble(0.25, 0.5) * 30;

                double iyawFactor = dynamicCheck ? initialYawFactor + MovementUtils.getSpeed() * 6.5 : initialYawFactor;
                double ipitchFactor = dynamicCheck ? initialPitchFactor + MovementUtils.getSpeed() : initialPitchFactor;

                double iyawOffset = rand.nextGaussian(0.00942273861037109, 0.23319837528201348) * iyawFactor;
                double ipitchOffset = rand.nextGaussian(0.30075078007595923, 0.3492437109081718) * ipitchFactor;

                float targetYaw = yaw;
                float targetPitch = pitch;

                if (dynamicCheck ? rand.nextInt(100) <= 75 : rand.nextInt(100) <= 25) {
                    targetYaw += (float) iyawOffset;
                    targetPitch += (float) ipitchOffset;

                    lastYawOffset = iyawOffset;
                    lastPitchOffset = ipitchOffset;
                } else {
                    targetYaw += (float) lastYawOffset;
                    targetPitch += (float) lastPitchOffset;
                }

                float yawLerp = dynamicCheck ? 1.0f : (float) MathUtils.randomizeDouble(0.5, 0.7);
                float pitchLerp = dynamicCheck ? 1.0f : (float) MathUtils.randomizeDouble(0.5, 0.7);

                yaw = MathUtils.interpolate(yaw, targetYaw, yawLerp);
                pitch = MathUtils.interpolate(pitch, targetPitch, pitchLerp);
                break;
        }

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

        return new float[]{yaw, pitch};
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        predictProcesses.clear();

        SimulatedPlayer simulatedPlayer = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);

        simulatedPlayer.rotationYaw = RotationUtils.currentRotation != null ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw;

        for (int i = 0; i < (predict.get() ? predictTicks.get() : 1); i++) {
            simulatedPlayer.tick();
            predictProcesses.add(new PlayerUtils.PredictProcess(
                    simulatedPlayer.getPos(),
                    simulatedPlayer.fallDistance,
                    simulatedPlayer.onGround,
                    simulatedPlayer.isCollidedHorizontally
            ));
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