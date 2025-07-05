package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInput;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.*;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.rotation.RotationHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ModuleInfo(name = "Speed", description = "Makes you go faster.")
public class Speed extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Strafe Hop", "Custom", "NCP", "Verus", "Legit", "Intave", "Vulcan", "BMC", "Miniblox"}, "Strafe Hop", this);

    private final BoolValue abideFriction = new BoolValue("Abide friction", true, this, () -> mode.is("Custom"));
    private final SliderValue customSpeed = new SliderValue("Speed", 0.35f, 0, 1, 0.01f, this, () -> mode.is("Custom") && abideFriction.get());
    private final SliderValue randomizeSpeed = new SliderValue("Randomize speed", 0.0f, 0.0f, 10, 0.1f, this, () -> mode.is("Custom") && abideFriction.get());
    private final SliderValue motionMultiplier = new SliderValue("Motion multiplier", 1, 0, 5, 0.01f, this, () -> mode.is("Custom") && !abideFriction.get());
    private final SliderValue customJumpMotion = new SliderValue("Jump motion", 0.42f, 0, 1, 0.01f, this, () -> mode.is("Custom"));
    private final BoolValue pullDown = new BoolValue("PullDown", false, this, () -> mode.is("Custom"));
    private final SliderValue pullDownTick = new SliderValue("PullDown tick", 2, 0, 12, 1, this, () -> mode.is("Custom") && pullDown.get());
    private final SliderValue pullDownMotionMotion = new SliderValue("PullDown motion", 0.4f, 0, 1, 0.01f, this, () -> mode.is("Custom") && pullDown.get());
    private final SliderValue timerSpeed = new SliderValue("Timer speed", 1.0f, 0.1f, 10, 0.1f, this, () -> mode.is("Custom"));
    private final SliderValue timerRandom = new SliderValue("Timer random", 0.0f, 0.0f, 10, 0.1f, this, () -> mode.is("Custom"));

    private final BoolValue ground = new BoolValue("Ground strafe", true, this, () -> mode.is("Strafe Hop") || mode.is("Custom"));
    private final SliderValue groundStrength = new SliderValue("Ground strength", 1, 0, 1, 0.01f, this, () -> (mode.is("Strafe Hop") || mode.is("Custom")) && ground.get());
    private final BoolValue air = new BoolValue("Air strafe", true, this, () -> mode.is("Strafe Hop") || mode.is("Custom"));
    private final SliderValue airStrength = new SliderValue("Air strength", 1, 0, 1, 0.01f, this, () -> (mode.is("Strafe Hop") || mode.is("Custom")) && air.get());

    private final ModeValue ncpMode = new ModeValue("NCP mode", new String[]{"Low hop", "Fast hop", "Old hop", "BHop"}, "Fast hop", this, () -> mode.is("NCP"));
    private final SliderValue speedMulti = new SliderValue("Extra speed multiplier", 0.4f, 0f, 1f, 0.01f, this, () -> ncpMode.is("Old Hop") && ncpMode.canDisplay());

    private final ModeValue verusMode = new ModeValue("Verus mode", new String[]{"Low"}, "Low", this, () -> mode.is("Verus"));

    private final BoolValue customGravity = new BoolValue("Custom gravity", false, this, () -> mode.is("Intave"));
    private final ModeValue intaveMode = new ModeValue("Intave mode", new String[]{"Safe", "Fast"}, "Safe", this, () -> mode.is("Intave"));
    private final BoolValue timer = new BoolValue("Timer", false, this, () -> mode.is("Intave") && intaveMode.is("Fast"));
    private final SliderValue iBoostMulti = new SliderValue("Boost multiplier", 1, 0f, 1, 0.1f, this, () -> mode.is("Intave") && intaveMode.is("Safe"));

    private final ModeValue bmcMode = new ModeValue("BMC mode", new String[]{"Low", "Ground"}, "Low", this, () -> mode.is("BMC"));

    private final SliderValue hopTicks = new SliderValue("Hop ticks", 5, 1, 6, 1, this, () -> mode.is("Miniblox"));

    private final BoolValue damageBoost = new BoolValue("Damage boost", false, this);
    private final SliderValue boostSpeed = new SliderValue("Boost speed", 0.5f, 0.01f, 1f, 0.01f, this, damageBoost::get);
    private final ModeValue yawOffsetMode = new ModeValue("Yaw offset", new String[]{"None", "Ground", "Air", "Constant"}, "Air", this);
    private final BoolValue minSpeedLimiter = new BoolValue("Min speed limiter", false, this);
    private final SliderValue minSpeed = new SliderValue("Min speed", 0.25f, 0, 1, 0.01f, this, minSpeedLimiter::get);
    private final SliderValue minMoveTicks = new SliderValue("Move ticks for limit", 15, 0, 40, 1, this, minSpeedLimiter::get);
    private final BoolValue printAirTicks = new BoolValue("Print airTicks", false, this);

    private int movingTicks, ticks;
    private int level = 1;
    private double moveSpeed = 0.2873;
    private double lastDist;
    private int timerDelay;

    @Override
    public void onEnable() {
        ticks = 0;
        level = !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0)).isEmpty() || mc.thePlayer.isCollidedVertically ? 1 : 4;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
        moveSpeed = MoveUtil.getBaseMoveSpeed();
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get().replace(" ", ""));

        if (!MoveUtil.isMoving()) {
            movingTicks = 0;
            return;
        } else {
            // doing mc.thePlayer.jump() in places where minecraft doesn't will cause simulation flags on certain anticheats.
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), mc.thePlayer.onGround);

            movingTicks++;
        }

        if (printAirTicks.get()) {
            ChatUtils.sendMessageClient("Air Ticks: " + mc.thePlayer.offGroundTicks);
        }

        if (MoveUtil.isMoving() && MoveUtil.getSpeed() < minSpeed.get() && movingTicks > minMoveTicks.get() && minSpeedLimiter.get()) {
            MoveUtil.strafe(minSpeed.get());
        }

        switch (yawOffsetMode.get()) {
            case "Ground":
                if (mc.thePlayer.onGround) {
                    RotationHandler.setBasicRotation(new float[]{MoveUtil.getYawFromKeybind(), mc.thePlayer.rotationPitch}, true, 180, 180);
                }
                break;
            case "Air":
                if (!mc.thePlayer.onGround) {
                    RotationHandler.setBasicRotation(new float[]{mc.thePlayer.rotationYaw + 45, mc.thePlayer.rotationPitch}, true, 180, 180);
                }
                break;
            case "Constant":
                RotationHandler.setBasicRotation(new float[]{MoveUtil.getYawFromKeybind(), mc.thePlayer.rotationPitch}, true, 180, 180);
                break;
        }

        if (mc.thePlayer.hurtTime >= 9 && damageBoost.get()) {
            MoveUtil.strafe(boostSpeed.get());
        }

        switch (mode.get()) {
            case "NCP":
                switch (ncpMode.get()) {
                    case "Fast hop":
                        if (mc.thePlayer.onGround) {
                            MoveUtil.strafe();
                        }

                        if (mc.thePlayer.offGroundTicks == 5) {
                            mc.thePlayer.motionY -= 0.1523351824467155;
                        }

                        if (mc.thePlayer.hurtTime >= 5 && mc.thePlayer.motionY >= 0) {
                            mc.thePlayer.motionY -= 0.1;
                        }

                        if (MoveUtil.isMoving()) {
                            mc.thePlayer.motionX *= 1.00718;
                            mc.thePlayer.motionZ *= 1.00718;
                        }

                        mc.timer.timerSpeed = 1f;
                        break;
                    case "Old hop":
                        if (MoveUtil.isMoving() && !mc.thePlayer.isInWater()) {
                            double spd = 0.0025 * speedMulti.get();
                            double m = (float) (Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ) + spd);
                            MoveUtil.bop(m);
                        }

                        if (mc.thePlayer.offGroundTicks == 4) {
                            mc.thePlayer.motionY -= 0.09800000190734863;
                        }

                        if (MoveUtil.getSpeed() < 0.312866806998394775 && movingTicks > 15) {
                            MoveUtil.strafe(0.312866806998394775);
                        }

                        if (MoveUtil.isMoving()) {
                            float timerSpeed = (float) (1.337 - MoveUtil.getSpeed());

                            if (timerSpeed > 1.5) timerSpeed = 1.5f;
                            if (timerSpeed < 0.6) timerSpeed = 0.6f;
                            mc.timer.timerSpeed = timerSpeed;
                        }
                        break;
                    case "Low hop":
                        if (mc.thePlayer.offGroundTicks == 4 && mc.thePlayer.posY % 1.0 == 0.16610926093821377) {
                            mc.thePlayer.motionY = -0.09800000190734863;
                        }

                        if (mc.thePlayer.offGroundTicks >= 6) {
                            MoveUtil.strafe();
                        }

                        if (mc.thePlayer.onGround) {
                            MoveUtil.strafe(Math.max(0.281, MoveUtil.getSpeed()));
                        }

                        if (MoveUtil.getSpeedEffect() > 0 && mc.thePlayer.offGroundTicks == 3) {
                            mc.thePlayer.motionX *= 1.2;
                            mc.thePlayer.motionZ *= 1.2;
                        }
                        break;
                }
                break;
            case "BMC":
                switch (bmcMode.get()) {
                    case "Low":
                        if (mc.thePlayer.offGroundTicks == 4 && mc.thePlayer.posY % 1.0 == 0.16610926093821377) {
                            if (MoveUtil.getSpeedEffect() == 0 || getModule(Scaffold.class).isEnabled()) {
                                mc.thePlayer.motionX *= 0.93;
                                mc.thePlayer.motionZ *= 0.93;
                            }

                            if (!getModule(Scaffold.class).isEnabled()) {
                                mc.thePlayer.motionY = -0.09800000190734863;
                            }
                        }

                        if (!mc.thePlayer.onGround) {
                            MoveUtil.strafe();
                        }

                        if (mc.thePlayer.offGroundTicks == 6) {
                            MoveUtil.strafe(Math.max(MoveUtil.getSpeed(), 0.281));
                        }

                        switch (MoveUtil.getSpeedEffect()) {
                            case 0 -> MoveUtil.strafe(Math.max(0.23, MoveUtil.getSpeed()));
                            case 1 -> MoveUtil.strafe(Math.max(0.27, MoveUtil.getSpeed()));
                            case 2 -> MoveUtil.strafe(Math.max(0.3, MoveUtil.getSpeed()));
                        }

                        if (MoveUtil.getSpeedEffect() > 0 && mc.thePlayer.offGroundTicks == 3) {
                            switch (MoveUtil.getSpeedEffect()) {
                                case 1:
                                    mc.thePlayer.motionX *= 1.07;
                                    mc.thePlayer.motionZ *= 1.07;
                                    break;
                                case 2:
                                    mc.thePlayer.motionX *= 1.15;
                                    mc.thePlayer.motionZ *= 1.15;
                                    break;
                            }
                        }
                        break;
                    case "Ground":
                        if (mc.thePlayer.onGroundTicks % 15 == 0) {
                            MoveUtil.strafe(0.2);
                        }
                        if (mc.thePlayer.onGroundTicks % 15 == 1) {
                            mc.thePlayer.motionX *= 0.1822;
                            mc.thePlayer.motionZ *= 0.1822;
                        }
                        if (mc.thePlayer.hurtTime > 0) {
                            MoveUtil.strafe(0.5);
                        }
                        break;
                }
                break;
            case "Intave":
                if (mc.thePlayer.isSprinting()) {
                    switch (intaveMode.get()) {
                        case "Safe": {
                            if (mc.thePlayer.motionY > 0.003) {
                                mc.thePlayer.motionX *= 1f + (0.003 * iBoostMulti.get());
                                mc.thePlayer.motionX *= 1f + (0.003 * iBoostMulti.get());
                            }
                            break;
                        }
                        case "Fast": {
                            switch (mc.thePlayer.offGroundTicks) {
                                case 1 -> {
                                    mc.thePlayer.motionX *= 1.005;
                                    mc.thePlayer.motionZ *= 1.005;
                                }
                                case 2, 3, 4, 5, 6 -> {
                                    mc.thePlayer.motionX *= 1.011;
                                    mc.thePlayer.motionZ *= 1.011;
                                }
                            }

                            if (mc.thePlayer.onGroundTicks == 1) {
                                mc.thePlayer.motionX *= 1.0045;
                                mc.thePlayer.motionZ *= 1.0045;
                            }

                            if (timer.get()) {
                                mc.timer.timerSpeed = 1.0075f;
                            }
                            break;
                        }
                    }
                }
                break;
            case "Vulcan":
                if (mc.thePlayer.onGround) {
                    MoveUtil.strafe();
                }

                if (mc.thePlayer.hurtTime == 0) {
                    switch (mc.thePlayer.offGroundTicks) {
                        case 10:
                            MoveUtil.strafe();
                            mc.thePlayer.motionY -= 0.44;
                            break;
                        case 12:
                            MoveUtil.strafe();
                            mc.thePlayer.motionY = 0;
                            break;
                    }
                }

                if (MoveUtil.getSpeed() <= 0.221) {
                    MoveUtil.strafe(0.221);
                }
                break;
            case "Miniblox": {
                switch (mc.thePlayer.offGroundTicks) {
                    case 1: {
                        switch ((int) hopTicks.get()) {
                            case 1:
                                mc.thePlayer.motionY -= 0.76;
                                break;
                            case 2:
                                mc.thePlayer.motionY -= 0.52;
                                break;
                            case 3:
                                mc.thePlayer.motionY -= 0.452335182447;
                                break;
                            case 4:
                                mc.thePlayer.motionY -= 0.322335182447;
                                break;
                            case 5:
                                mc.thePlayer.motionY -= 0.232335182447;
                                break;
                            case 6:
                                mc.thePlayer.motionY -= 0.162335182447;
                                break;
                        }
                    }
                    break;

                    case 3: {
                        mc.thePlayer.motionY -= 0.1523351824467155;
                    }
                    break;
                }
            }
            break;
        }
    }

    @EventTarget
    public void onGravity(GravityEvent e) {
        if (mode.is("Intave") && customGravity.get()) {
            e.setGravityDecrement(0.081);
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        switch (mode.get()) {
            case "NCP":
                if (ncpMode.is("BHop")) {
                    double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
                    double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
                    lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
                }
                break;
            case "Miniblox": {
                if (MoveUtil.isMoving()) {
                    if (mc.thePlayer.onGround) {
                        switch ((int) hopTicks.get()) {
                            case 1:
                                MoveUtil.strafe(0.07);
                                break;
                            case 2:
                                MoveUtil.strafe(0.08);
                                break;
                            case 3:
                                MoveUtil.strafe(0.09);
                                break;
                            case 4:
                                MoveUtil.strafe(0.1);
                                break;
                            case 5:
                                MoveUtil.strafe(0.115);
                                break;
                            case 6:
                                MoveUtil.strafe(0.13);
                                break;
                        }
                    } else {
                        MoveUtil.strafe(0.35);
                    }
                }
            }
            break;
        }
    }

    @EventTarget
    public void onPreMotion(MotionEvent e) {
        if (!e.isPre() || !MoveUtil.isMoving()) {
            return;
        }

        ticks++;

        switch (mode.get()) {
            case "Strafe Hop":
                if ((mc.thePlayer.onGround && ground.get()) || (!mc.thePlayer.onGround && air.get())) {
                    MoveUtil.strafe(MoveUtil.getSpeed(), mc.thePlayer.onGround ? groundStrength.get() : airStrength.get());
                }
                break;
            case "Custom": {
                double randomConst = randomizeSpeed.get() < 0.1 ? 0.0 : MathUtils.randomizeDouble(0.0, randomizeSpeed.get() / 100);
                double randomize = (rand.nextDouble() < 0.5 ? -randomConst : randomConst);

                if ((mc.thePlayer.onGround && ground.get()) || (!mc.thePlayer.onGround && air.get())) {
                    if (abideFriction.get()) {
                        MoveUtil.strafe(customSpeed.get() + randomize, mc.thePlayer.onGround ? groundStrength.get() : airStrength.get());
                    } else {
                        MoveUtil.strafe(MoveUtil.getSpeed(), mc.thePlayer.onGround ? groundStrength.get() : airStrength.get());
                    }
                }

                if (!abideFriction.get()) {
                    mc.thePlayer.motionX *= motionMultiplier.get();
                    mc.thePlayer.motionZ *= motionMultiplier.get();
                }

                if (pullDown.get()) {
                    if (mc.thePlayer.offGroundTicks == pullDownTick.get()) {
                        mc.thePlayer.motionY = -pullDownMotionMotion.get();
                    }
                }

                float timerRandomConst = (float) (timerRandom.get() < 0.1 ? 0.0 : MathUtils.randomizeDouble(0.0, timerRandom.get() / 100));
                float timerRandomize = (rand.nextDouble() < 0.5 ? -timerRandomConst : timerRandomConst);

                boolean timerReset = timerSpeed.get() - 1.0 < 1E-3 && timerSpeed.get() - 1.0 > -1E-3;

                mc.timer.timerSpeed = timerReset ? 1.0F : timerSpeed.get();
                mc.timer.timerSpeed += timerRandomize;
                break;
            }
        }
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        switch (mode.get()) {
            case "Verus":
                if (verusMode.is("Low") && MoveUtil.isMoving()) {
                    if (ticks % 12 == 0 && mc.thePlayer.onGround) {
                        MoveUtil.strafe(0.69);
                        e.setY(0.42F);
                        mc.thePlayer.motionY = -(mc.thePlayer.posY - roundToOnGround(mc.thePlayer.posY));
                    } else {
                        if (mc.thePlayer.onGround) {
                            MoveUtil.strafe(1.01);
                        } else {
                            MoveUtil.strafe(0.41);
                        }
                    }
                }
                break;
            case "NCP":
                if (ncpMode.is("BHop")) {
                    ++timerDelay;
                    timerDelay %= 5;
                    if (timerDelay != 0) {
                        mc.timer.timerSpeed = 1F;
                    } else {
                        if (MoveUtil.isMoving()) {
                            mc.timer.timerSpeed = 1.3F;
                            mc.thePlayer.motionX *= 1.0199999809265137;
                            mc.thePlayer.motionZ *= 1.0199999809265137;
                        }
                    }

                    if (mc.thePlayer.onGround && MoveUtil.isMoving())
                        level = 2;

                    if (round(mc.thePlayer.posY - (double) ((int) mc.thePlayer.posY)) == round(0.138)) {
                        EntityPlayerSP thePlayer = mc.thePlayer;
                        thePlayer.motionY -= 0.08;
                        e.setY(e.getY() - 0.09316090325960147);
                        thePlayer.posY -= 0.09316090325960147;
                    }

                    if (level == 1 && (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f)) {
                        level = 2;
                        moveSpeed = 1.35 * MoveUtil.getBaseMoveSpeed() - 0.01;
                    } else if (level == 2) {
                        level = 3;
                        mc.thePlayer.motionY = 0.399399995803833;
                        e.setY(0.399399995803833);
                        moveSpeed *= 2.149;
                    } else if (level == 3) {
                        level = 4;
                        double difference = 0.66 * (lastDist - MoveUtil.getBaseMoveSpeed());
                        moveSpeed = lastDist - difference;
                    } else {
                        if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0)).isEmpty() || mc.thePlayer.isCollidedVertically)
                            level = 1;

                        moveSpeed = lastDist - lastDist / 159.0;
                    }

                    moveSpeed = Math.max(moveSpeed, MoveUtil.getBaseMoveSpeed());
                    final MovementInput movementInput = mc.thePlayer.movementInput;
                    float forward = movementInput.moveForward;
                    float strafe = movementInput.moveStrafe;

                    TargetStrafe targetStrafe = getModule(TargetStrafe.class);

                    float yaw = targetStrafe.isEnabled() && targetStrafe.active && targetStrafe.target != null ? targetStrafe.yaw : mc.thePlayer.rotationYaw;
                    if (forward == 0.0f && strafe == 0.0f) {
                        e.setX(0.0);
                        e.setZ(0.0);
                    } else if (forward != 0.0f) {
                        if (strafe >= 1.0f) {
                            yaw += (float) (forward > 0.0f ? -45 : 45);
                            strafe = 0.0f;
                        } else if (strafe <= -1.0f) {
                            yaw += (float) (forward > 0.0f ? 45 : -45);
                            strafe = 0.0f;
                        }
                        if (forward > 0.0f) {
                            forward = 1.0f;
                        } else if (forward < 0.0f) {
                            forward = -1.0f;
                        }
                    }

                    final double mx2 = Math.cos(Math.toRadians(yaw + 90.0f));
                    final double mz2 = Math.sin(Math.toRadians(yaw + 90.0f));
                    e.setX((double) forward * moveSpeed * mx2 + (double) strafe * moveSpeed * mz2);
                    e.setZ((double) forward * moveSpeed * mz2 - (double) strafe * moveSpeed * mx2);

                    mc.thePlayer.stepHeight = 0.6F;
                    if (forward == 0.0F && strafe == 0.0F) {
                        e.setX(0.0);
                        e.setZ(0.0);
                    }
                }
                break;
        }
    }

    private double round(double value) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    @EventTarget
    public void onJump(JumpEvent e) {
        switch (mode.get()) {
            case "Intave":
                if (intaveMode.is("Fast")) {
                    e.setJumpoff(0.209f);
                }
                break;
            case "Custom":
                // getJumpUpwardsMotion() in EntityLivingBase is a float, and it's added to the motionY, which is a double.
                // to simulate that, we need to convert the float to a double and back to a float.
                e.setMotionY((float) ((double) customJumpMotion.get()));
                break;
        }
    }

    private double roundToOnGround(double posY) {
        return posY - (posY % 0.015625);
    }
}