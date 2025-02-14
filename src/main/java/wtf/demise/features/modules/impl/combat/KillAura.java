package wtf.demise.features.modules.impl.combat;

import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.client.entity.EntityPlayerSP;
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
import org.lwjglx.util.vector.Vector2f;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.AttackEvent;
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
import wtf.demise.utils.misc.DebugUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.utils.player.*;
import wtf.demise.utils.render.RenderUtils;

import java.util.*;

@ModuleInfo(name = "KillAura", category = ModuleCategory.Combat)
public class KillAura extends Module {

    //reach
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 4.0f, 1, 8, 0.1f, this);

    // attack
    private final ModeValue clickMode = new ModeValue("Click mode", new String[]{"Legit", "PlayerController", "Packet"}, "PlayerController", this);
    private final BoolValue smartClicking = new BoolValue("Smart clicking", false, this);
    private final BoolValue noSwing = new BoolValue("No swing", false, this, () -> Objects.equals(clickMode.get(), "Packet"));
    private final SliderValue attackDelayMin = new SliderValue("Attack delay (min)", 25, 25, 1000, 25, this);
    private final SliderValue attackDelayMax = new SliderValue("Attack delay (max)", 25, 25, 1000, 25, this);
    private final BoolValue extraClicks = new BoolValue("Extra clicks", false, this);
    private final SliderValue eChance = new SliderValue("Chance", 50, 1, 100, 1, this, extraClicks::get);
    private final SliderValue clicks = new SliderValue("Extra click count", 1, 1, 10, 1, this, extraClicks::get);
    private final BoolValue rayCast = new BoolValue("RayCast", false, this);
    private final BoolValue failSwing = new BoolValue("Fail swing", false, this, rayCast::get);

    // autoBlock
    private final ModeValue autoBlock = new ModeValue("AutoBlock", new String[]{"None", "Fake", "Vanilla", "Release", "VanillaForce", "Smart", "Blink"}, "Vanilla", this);
    private final BoolValue unBlockOnRayCastFail = new BoolValue("Unblock on rayCast fail", false, this, () -> !Objects.equals(autoBlock.get(), "None") && rayCast.get());

    // rotation
    private final ModeValue rotationMode = new ModeValue("Rotation mode", new String[]{"Silent", "Normal", "None"}, "Silent", this);
    private final ModeValue aimPos = new ModeValue("Aim position", new String[]{"Head", "Torso", "Legs", "Nearest", "Straight", "Dynamic"}, "Straight", this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue xzTrim = new SliderValue("XZ trim", 0, 0, 0.5f, 0.01f, this);
    private final SliderValue yTrim = new SliderValue("Y trim", 0, 0, 0.5f, 0.01f, this);
    private final ModeValue smoothMode = new ModeValue("Smooth mode", new String[]{"Linear", "Lerp", "LerpLimit", "Correlation"}, "Linear", this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final BoolValue smoothReset = new BoolValue("Smooth reset", false, this);
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
    private final SliderValue pauseRange = new SliderValue("Pause range", 0.5f, 0, 6, 0.1f, this, () -> !Objects.equals(rotationMode.get(), "None") && pauseRotation.get());
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
    private final ModeValue targetPriority = new ModeValue("Target Priority", new String[]{"None", "Distance", "Health"}, "Distance", this);
    private final BoolValue targetSwitch = new BoolValue("Target Switch", false, this);
    private final BoolValue pitSwitch = new BoolValue("Pit Switch", false, this, targetSwitch::get); // helps with vampire thing on the pit
    private final SliderValue targetSwitchDelay = new SliderValue("Target Switch Delay (ms)", 500, 50, 1000, 50, this, targetSwitch::get);

    // misc
    private final BoolValue targetOnPlayer = new BoolValue("Target on player", false, this);
    private final SliderValue dotScale = new SliderValue("Target scale", 0.04f, 0.01f, 1, 0.01f, this, targetOnPlayer::get);
    private final BoolValue botCheck = new BoolValue("Bot Check", false, this);

    public final List<PlayerUtils.PredictProcess> predictProcesses = new ArrayList<>();
    private final Queue<Vec3> positionHistory = new LinkedList<>();
    private final TimerUtils lastTargetTime = new TimerUtils();
    private final TimerUtils lastSwitchTime = new TimerUtils();
    public List<EntityLivingBase> targets = new ArrayList<>();
    private Vec3 positionOnPlayer, lastPositionOnPlayer;
    public static Entity currentTarget = null;
    private final Random rand = new Random();
    public static boolean isBlocking = false;
    private boolean blink = false;
    private int blockTicks = 0;
    public Vec3 currentVec;
    double lastPitchOffset;
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
        if (isBlocking) {
            blocking(false);
        }
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

        if (pitSwitch.get()) {
            if (mc.thePlayer.getHealth() > 15) {
                currentTarget = findTarget();
            } else if (lastSwitchTime.hasTimeElapsed(targetSwitchDelay.get())) {
                currentTarget = findNextTarget();
            }
        } else if (!targetSwitch.get()) {
            currentTarget = findTarget();
        } else if (lastSwitchTime.hasTimeElapsed(targetSwitchDelay.get())) {
            currentTarget = findNextTarget();
        }

        if (currentTarget != null) {
            lastSwitchTime.reset();

            if (!isWithinAttackRange() && isBlocking) {
                blocking(false);
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

        targets.removeIf(target -> PlayerUtils.getDistanceToEntityBox(currentTarget) > searchRange.get() || target.isDead);

        if (currentTarget != null && Objects.equals(rotationMode.get(), "Silent") && PlayerUtils.getDistanceToEntityBox(currentTarget) <= searchRange.get() + 0.4) {
            if (!pauseRotation.get() || PlayerUtils.getDistanceToEntityBox(currentTarget) >= pauseRange.get()) {
                setRotationToTarget(currentTarget);
            }
        }

        if (currentTarget != null) {
            sendAttack();
        } else {
            predictProcesses.clear();
            positionHistory.clear();

            if (isBlocking) {
                blocking(false);
            }
        }

        this.setTag(currentTarget != null ? currentTarget.getName() : "null");
    }

    private void setRotationToTarget(Entity target) {
        SmoothMode mode = SmoothMode.valueOf(smoothMode.get());
        MovementCorrection correction = movementFix.get() ? MovementCorrection.SILENT : MovementCorrection.OFF;

        switch (mode) {
            case Linear:
                RotationUtils.setRotation(calcToEntity((EntityLivingBase) target), correction,
                        MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()),
                        MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()),
                        smoothReset.get(), SmoothMode.Linear);
                break;
            case Lerp:
                RotationUtils.setRotation(calcToEntity((EntityLivingBase) target), correction,
                        MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()),
                        MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()),
                        smoothReset.get(), SmoothMode.Lerp);
                break;
            case LerpLimit:
                RotationUtils.setRotation(calcToEntity((EntityLivingBase) target), correction,
                        MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()),
                        MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()),
                        smoothReset.get(), SmoothMode.LerpLimit,
                        MathUtils.randomizeInt(limitYawSpeedMin.get(), limitYawSpeedMax.get()),
                        MathUtils.randomizeInt(limitPitchSpeedMin.get(), limitPitchSpeedMax.get()),
                        rangeToLimit.get() + 0.4f, target);
                break;
            case Correlation:
                RotationUtils.setRotation(calcToEntity((EntityLivingBase) target), correction,
                        MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()),
                        MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()),
                        smoothReset.get(), SmoothMode.Correlation,
                        MathUtils.randomizeInt(limitYawSpeedMin.get(), limitYawSpeedMax.get()),
                        MathUtils.randomizeInt(limitPitchSpeedMin.get(), limitPitchSpeedMax.get()),
                        rangeToLimit.get() + 0.4f, target, threshold.get());
        }
    }

    @EventTarget
    public void onAttack(AttackEvent e) {
        targets.add((EntityLivingBase) e.getTargetEntity());
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
            double distanceToEntity = mc.thePlayer.getDistanceToEntity(entity);

            if (entity instanceof EntityPlayer && entity != mc.thePlayer && !Demise.INSTANCE.getFriendManager().isFriend((EntityPlayer) entity) && distanceToEntity <= searchRange.get() + 0.4) {
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
            if (isBlocking) {
                blocking(false);
            }
            return;
        }

        if (!isAttackReady()) {
            return;
        }

        MovingObjectPosition mov = performRayCast();

        handleAttack(mov);
        lastTargetTime.reset();
    }

    private void handleAttack(MovingObjectPosition mov) {

        if (shouldFailSwing(mov)) {
            handleFailSwing(mov);
            return;
        }

        if ((rayCast.get() && mov != null && mov.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) || !rayCast.get()) {
            attack();
        }

        handleAutoBlock();
    }

    private boolean isWithinAttackRange() {
        return PlayerUtils.getDistanceToEntityBox(currentTarget) <= attackRange.get();
    }

    private boolean isAttackReady() {
        long delay = MathUtils.nextInt((int) attackDelayMin.get(), (int) attackDelayMax.get());
        return lastTargetTime.hasTimeElapsed(delay);
    }

    private MovingObjectPosition performRayCast() {
        float yaw = RotationUtils.currentRotation != null ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw;
        float pitch = RotationUtils.currentRotation != null ? RotationUtils.currentRotation[1] : mc.thePlayer.rotationPitch;
        return RayCastUtil.rayCast(new Vector2f(yaw, pitch), attackRange.get() + 0.4, -0.1f);
    }

    private boolean shouldFailSwing(MovingObjectPosition movingObjectPosition) {
        return rayCast.get() && (movingObjectPosition == null || movingObjectPosition.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY);
    }

    private void handleFailSwing(MovingObjectPosition movingObjectPosition) {
        if (failSwing.get() && failSwing.canDisplay()) {
            switch (clickMode.get()) {
                case "PlayerController":
                    AttackOrder.sendConditionalSwing(movingObjectPosition);
                    break;
                case "Packet":
                    if (!noSwing.get()) {
                        mc.thePlayer.swingItem();
                    }
                    PacketUtils.sendPacket(new C02PacketUseEntity(currentTarget, C02PacketUseEntity.Action.ATTACK));
                    break;
                case "Legit":
                    KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
                    break;
            }
        }

        if (isBlocking && unBlockOnRayCastFail.get()) {
            blocking(false);
        }

        lastTargetTime.reset();
    }

    private void handleAutoBlock() {
        switch (autoBlock.get()) {
            case "None":
                if (isBlocking) {
                    blocking(false);
                }
                break;
            case "Fake":
                isBlocking = true;
                break;
            case "Vanilla":
                blocking(true);
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
    }

    private boolean shouldClick() {
        if (mc.thePlayer.hurtTime != 0 || mc.thePlayer.motionY < 0) {
            return true;
        }

        if (currentTarget instanceof EntityLivingBase) {
            return ((EntityLivingBase) currentTarget).hurtTime <= 3;
        }

        return true;
    }

    private void attack() {
        int attackCount = 1;

        if (extraClicks.get() && rand.nextInt(100) <= eChance.get()) {
            attackCount = (int) clicks.get() + 1;
        }

        for (int i = 0; i < attackCount; i++) {
            switch (clickMode.get()) {
                case "PlayerController":
                    AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
                    break;
                case "Legit":
                    KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
                    break;
                case "Packet":
                    if (!noSwing.get()) {
                        mc.thePlayer.swingItem();
                    }
                    mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(currentTarget, C02PacketUseEntity.Action.ATTACK));
                    break;
            }
        }
    }

    @EventTarget
    public void onMouseOver(MouseOverEvent event) {
        if (Objects.equals(clickMode.get(), "Legit")) {
            event.setRange(attackRange.get());
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
        blocking(e.hurtTime >= 5 || isWithinAttackRange());
    }

    private void vanillaReblockAb(Entity e) {
        blocking(true);
        if (!mc.gameSettings.keyBindUseItem.isKeyDown() || !mc.thePlayer.isBlocking()) {
            blocking(true);
        }
    }

    private void smartAb(EntityLivingBase e) {
        blocking(((mc.thePlayer.hurtTime <= 5 && mc.thePlayer.hurtTime != 0) && mc.thePlayer.motionY >= 0) || e.hurtTime > 5);
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
                blocking(false);
                break;
            case 2:
                blocking(true);
                blink = false;
                break;
        }
    }

    private void blocking(boolean state) {
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
}