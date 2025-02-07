package wtf.demise.features.modules.impl.combat;

import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.packet.PingSpoofComponent;
import wtf.demise.features.modules.ModuleCategory;
import de.florianmichael.viamcp.fixes.AttackOrder;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.settings.KeyBinding;
import wtf.demise.features.modules.ModuleInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.*;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.packet.PacketUtils;
import wtf.demise.features.modules.Module;
import org.lwjglx.util.vector.Vector2f;
import wtf.demise.utils.math.MathUtils;
import net.minecraft.item.ItemSword;
import net.minecraft.entity.Entity;
import wtf.demise.utils.player.*;
import net.minecraft.util.*;

import java.util.*;

import wtf.demise.Demise;

@ModuleInfo(name = "KillAura", category = ModuleCategory.Combat)
public class KillAura extends Module {

    //reach
    private final SliderValue attackRange = new SliderValue("Attack range", 3, 1, 8, 0.1f, this);
    private final SliderValue searchRange = new SliderValue("Search range", 4.0f, 1, 8, 0.1f, this);

    // attack
    private final SliderValue attackDelayMin = new SliderValue("Attack delay (min)", 25, 25, 1000, 25, this);
    private final SliderValue attackDelayMax = new SliderValue("Attack delay (max)", 25, 25, 1000, 25, this);
    private final BoolValue failSwing = new BoolValue("Fail swing", false, this);

    // autoBlock
    private final ModeValue autoBlock = new ModeValue("AutoBlock", new String[]{"None", "Fake", "Vanilla", "Release", "AAC", "VanillaForce", "Smart", "Blink", "Fast Blink"}, "Vanilla", this);

    // rotation
    private final ModeValue rotationMode = new ModeValue("Rotation mode", new String[]{"Silent", "Normal", "None"}, "Silent", this);
    private final ModeValue aimMode = new ModeValue("Aim position", new String[]{"Head", "Torso", "Legs", "Nearest", "Straight"}, "Nearest", this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final ModeValue smoothMode = new ModeValue("Smooth mode", new String[]{"Linear", "Lerp", "LerpLimit"}, "Linear", this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final BoolValue smoothReset = new BoolValue("Smooth reset", false, this);
    private final SliderValue yawRotationSpeedMin = new SliderValue("Yaw rotation speed (min)", 180, 0.01f, 180, 0.01f, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue yawRotationSpeedMax = new SliderValue("Yaw rotation speed (max)", 180, 0.01f, 180, 0.01f, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue pitchRotationSpeedMin = new SliderValue("Pitch rotation speed (min)", 180, 0.01f, 180, 0.01f, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue pitchRotationSpeedMax = new SliderValue("Pitch rotation speed (max)", 180, 0.01f, 180, 0.01f, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue rangeToLimit = new SliderValue("Range to limit (<)", 1.5f, 0, 8, 0.1f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final SliderValue limitYawSpeedMin = new SliderValue("Yaw limit speed (min)", 180, 0.01f, 180, 0.01f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final SliderValue limitYawSpeedMax = new SliderValue("Yaw limit speed (max)", 180, 0.01f, 180, 0.01f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final SliderValue limitPitchSpeedMin = new SliderValue("Pitch limit speed (min)", 180, 0.01f, 180, 0.01f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final SliderValue limitPitchSpeedMax = new SliderValue("Pitch limit speed (max)", 180, 0.01f, 180, 0.01f, this, () -> Objects.equals(smoothMode.get(), "LerpLimit"));
    private final BoolValue movementFix = new BoolValue("Movement fix", false, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final BoolValue pauseRotation = new BoolValue("Pause rotation", false, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue pauseRange = new SliderValue("Pause range", 0.5f, 0, 6, 0.1f, this, () -> !Objects.equals(rotationMode.get(), "None") && pauseRotation.get());
    private final BoolValue delayed = new BoolValue("Delayed", false, this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue delayedTicks = new SliderValue("Ticks", 2, 0, 20, 1, this, () -> delayed.get() && delayed.canDisplay());
    // offset
    private final ModeValue offsetMode = new ModeValue("Offset mode", new String[]{"None", "Gaussian", "SinCos"}, "None", this, () -> !Objects.equals(rotationMode.get(), "None"));
    private final SliderValue chance = new SliderValue("Offset chance", 75, 1, 100, 1, this, () -> rotationMode.canDisplay() && Objects.equals(offsetMode.get(), "Gaussian"));
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

    private final Queue<Vec3> positionHistory = new LinkedList<>();
    public List<EntityLivingBase> targets = new ArrayList<>();
    private Vec3 positionOnPlayer, lastPositionOnPlayer;
    public static Entity currentTarget = null;
    public static boolean isBlocking = false;
    private static long lastTargetTime = 0;
    private long lastSwitchTime = 0;
    private boolean blink = false;
    private int blockTicks = 0;
    public Vec3 currentVec;
    public Vec3 targetVec;
    public Vec3 prevVec;

    @Override
    public void onEnable() {
        lastTargetTime = lastSwitchTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        if (isBlocking) {
            blocking(false);
        }
        blink = false;
        currentTarget = null;
        targets.clear();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (pitSwitch.get()) {
            if (mc.thePlayer.getHealth() > 15) {
                currentTarget = findTarget();
                lastSwitchTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastSwitchTime >= targetSwitchDelay.get()) {
                currentTarget = findNextTarget();
                lastSwitchTime = System.currentTimeMillis();
            }
        } else if (!targetSwitch.get()) {
            currentTarget = findTarget();
            lastSwitchTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastSwitchTime >= targetSwitchDelay.get()) {
            currentTarget = findNextTarget();
            lastSwitchTime = System.currentTimeMillis();
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                if (mc.thePlayer.getDistanceToEntity(entity) <= searchRange.get() + 0.4) {
                    targets.add((EntityLivingBase) entity);
                }
            }
        }

        targets.removeIf(target -> mc.thePlayer.getDistanceToEntity(target) > searchRange.get() + 0.4);

        if (currentTarget != null) {
            if (Objects.equals(rotationMode.get(), "Silent") && mc.thePlayer.getDistanceToEntity(currentTarget) <= searchRange.get() + 0.4) {
                if ((pauseRotation.get() && mc.thePlayer.getDistanceToEntity(currentTarget) >= pauseRange.get()) || !pauseRotation.get()) {
                    switch (smoothMode.get()) {
                        case "Linear":
                            RotationUtils.setRotation(calcToEntity((EntityLivingBase) currentTarget), movementFix.get() ? MovementCorrection.SILENT : MovementCorrection.OFF, MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()), MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()), smoothReset.get(), SmoothMode.Linear);
                            break;
                        case "Lerp":
                            RotationUtils.setRotation(calcToEntity((EntityLivingBase) currentTarget), movementFix.get() ? MovementCorrection.SILENT : MovementCorrection.OFF, MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()), MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()), smoothReset.get(), SmoothMode.Lerp);
                            break;
                        case "LerpLimit":
                            RotationUtils.setRotation(calcToEntity((EntityLivingBase) currentTarget), movementFix.get() ? MovementCorrection.SILENT : MovementCorrection.OFF, MathUtils.randomizeInt(yawRotationSpeedMin.get(), yawRotationSpeedMax.get()), MathUtils.randomizeInt(pitchRotationSpeedMin.get(), pitchRotationSpeedMax.get()), smoothReset.get(), SmoothMode.LerpLimit, MathUtils.randomizeInt(limitYawSpeedMin.get(), limitYawSpeedMax.get()), MathUtils.randomizeInt(limitPitchSpeedMin.get(), limitPitchSpeedMax.get()), rangeToLimit.get() + 0.4f, currentTarget);
                            break;
                    }
                }
            }

            if (mc.thePlayer.getDistanceToEntity(currentTarget) <= attackRange.get() + 0.4) {
                if (System.currentTimeMillis() - lastTargetTime >= MathUtils.randomizeInt((int) attackDelayMin.get(), (int) attackDelayMax.get())) {
                    lastTargetTime = System.currentTimeMillis();

                    MovingObjectPosition movingObjectPosition = RayCastUtil.rayCast(new Vector2f(RotationUtils.currentRotation[0], RotationUtils.currentRotation[1]), attackRange.get() + 0.85 /* tested on grim, works fine */, -0.1f);
                    if (movingObjectPosition == null || movingObjectPosition.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
                        if (failSwing.get()) {
                            AttackOrder.sendConditionalSwing(movingObjectPosition);
                        }

                        if (isBlocking) {
                            blocking(false);
                        }
                    } else {
                        switch (autoBlock.get()) {
                            case "None":
                                if (isBlocking) {
                                    blocking(false);
                                }
                                AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
                                break;
                            case "Fake":
                                isBlocking = true;
                                AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
                                break;
                            case "Vanilla":
                                blocking(true);
                                AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
                                break;
                            case "Release":
                                releaseAb((EntityLivingBase) currentTarget);
                                break;
                            case "AAC":
                                AACAb(currentTarget);
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
                            case "Fast Blink":
                                //handled on render tick
                                break;
                        }
                    }
                }
            } else if (isBlocking) {
                blocking(false);
            }
        } else if (isBlocking) {
            blocking(false);
        }

        this.setTag(this.isEnabled() ? currentTarget != null ? currentTarget.getName() : "null" : "");
    }

    private EntityLivingBase findNextTarget() {
        List<Entity> targets = new ArrayList<>();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                if (botCheck.get() && getModule(AntiBot.class).isBot((EntityPlayer) entity)) {
                    continue;
                }

                double distanceToEntity = mc.thePlayer.getDistanceToEntity(entity);
                if (distanceToEntity <= searchRange.get() + 0.4) {
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

                switch (targetPriority.get()) {
                    case "Distance":
                        if (distanceToEntity < closestDistance) {
                            target = (EntityLivingBase) entity;
                            closestDistance = distanceToEntity;
                        }
                        break;
                    case "Health":
                        EntityLivingBase potentialTarget = (EntityLivingBase) entity;
                        float potentialHealth = potentialTarget.getHealth();
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
            if (mc.thePlayer.getDistanceToEntity(currentTarget) <= attackRange.get() + 0.4 && Objects.equals(autoBlock.get(), "Fast Blink")) {
                blinkAb(currentTarget);
            }

            if (positionOnPlayer != null && lastPositionOnPlayer != null && targetOnPlayer.get()) {
                Vec3 interpolatedPosition = new Vec3(
                        (positionOnPlayer.xCoord - lastPositionOnPlayer.xCoord) * mc.timer.renderPartialTicks + lastPositionOnPlayer.xCoord,
                        (positionOnPlayer.yCoord - lastPositionOnPlayer.yCoord) * mc.timer.renderPartialTicks + lastPositionOnPlayer.yCoord,
                        (positionOnPlayer.zCoord - lastPositionOnPlayer.zCoord) * mc.timer.renderPartialTicks + lastPositionOnPlayer.zCoord);
                RenderUtils.renderBreadCrumb(interpolatedPosition, dotScale.get());
            }
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
        AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
        blocking(e.hurtTime >= 5 || mc.thePlayer.getDistanceToEntity(e) > attackRange.get() + 0.4);
    }

    private void AACAb(Entity e) {
        if (System.currentTimeMillis() - lastTargetTime >= MathUtils.nextInt((int) attackDelayMin.get(), (int) attackDelayMax.get())) {
            blocking(false);
            AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
            lastTargetTime = System.currentTimeMillis();
        }
        mc.addScheduledTask(() -> blocking(true));

        if (mc.thePlayer.ticksExisted % 2 == 0) {
            mc.playerController.interactWithEntitySendPacket(mc.thePlayer, e);
            PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }
    }

    private void vanillaReblockAb(Entity e) {
        blocking(true);
        if (!mc.gameSettings.keyBindUseItem.isKeyDown() || !mc.thePlayer.isBlocking()) {
            blocking(true);
        }
        AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
    }

    private void smartAb(EntityLivingBase e) {
        AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
        blocking(((mc.thePlayer.hurtTime <= 5 && mc.thePlayer.hurtTime != 0) && mc.thePlayer.motionY >= 0) || e.hurtTime >= 5);
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
                AttackOrder.sendFixedAttack(mc.thePlayer, currentTarget);
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

        Vec3 playerPos = mc.thePlayer.getPositionEyes(1);
        Vec3 entityPos = entity.getPositionVector();
        final AxisAlignedBB entityBoundingBox = entity.getEntityBoundingBox();

        switch (aimMode.get()) {
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
                Random rand = new Random();

                if (rand.nextInt(100) <= chance.get()) {
                    double yawFactor = MathUtils.randomizeDouble(minYawFactor.get(), maxYawFactor.get()) * 20;
                    double pitchFactor = MathUtils.randomizeDouble(minPitchFactor.get(), maxPitchFactor.get()) * 20;

                    yaw += (float) (rand.nextGaussian(0.00942273861037109, 0.23319837528201348) * yawFactor);
                    pitch += (float) (rand.nextGaussian(0.30075078007595923, 0.3492437109081718) * pitchFactor);
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
        }

        pitch = MathHelper.clamp_float(pitch, -90, 90);

        return new float[]{yaw, pitch};
    }
}