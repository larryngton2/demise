package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
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

@ModuleInfo(name = "Speed", description = "Makes you go faster.")
public class Speed extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Strafe Hop", "Custom", "NCP", "Verus", "Legit", "Intave", "Vulcan", "BMC", "Collide", "AAC"}, "Strafe Hop", this);

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

    private final ModeValue ncpMode = new ModeValue("NCP mode", new String[]{"Low hop", "Fast hop", "Old hop"}, "Fast hop", this, () -> mode.is("NCP"));
    private final SliderValue speedMulti = new SliderValue("Extra speed multiplier", 0.4f, 0f, 1f, 0.01f, this, () -> ncpMode.is("Old Hop") && ncpMode.canDisplay());

    private final ModeValue verusMode = new ModeValue("Verus mode", new String[]{"Low"}, "Low", this, () -> mode.is("Verus"));

    private final BoolValue customGravity = new BoolValue("Custom gravity", false, this, () -> mode.is("Intave"));
    private final ModeValue intaveMode = new ModeValue("Intave mode", new String[]{"Safe", "Fast"}, "Safe", this, () -> mode.is("Intave"));
    private final BoolValue timer = new BoolValue("Timer", false, this, () -> mode.is("Intave") && intaveMode.is("Fast"));
    private final SliderValue iBoostMulti = new SliderValue("Boost multiplier", 1, 0f, 1, 0.1f, this, () -> mode.is("Intave") && intaveMode.is("Safe"));

    private final SliderValue collideSpeed = new SliderValue("Collide speed", 0.08f, 0.01f, 0.08f, 0.01f, this, () -> mode.is("Collide"));

    private final ModeValue aacMode = new ModeValue("AAC mode", new String[]{"3.2.0"}, "3.2.0", this, () -> mode.is("AAC"));

    private final BoolValue damageBoost = new BoolValue("Damage boost", false, this);
    private final SliderValue boostSpeed = new SliderValue("Boost speed", 0.5f, 0.01f, 1f, 0.01f, this, damageBoost::get);
    private final ModeValue yawOffsetMode = new ModeValue("Yaw offset", new String[]{"None", "Ground", "Air", "Constant"}, "Air", this);
    private final BoolValue minSpeedLimiter = new BoolValue("Min speed limiter", false, this);
    private final SliderValue minSpeed = new SliderValue("Min speed", 0.25f, 0, 1, 0.01f, this, minSpeedLimiter::get);
    private final SliderValue minMoveTicks = new SliderValue("Move ticks for limit", 15, 0, 40, 1, this, minSpeedLimiter::get);
    private final BoolValue printAirTicks = new BoolValue("Print airTicks", false, this);

    public Speed() {
        abideFriction.setDescription("True: move at a constant speed defined by the speed option, False: follow vanilla speed.");
        yawOffsetMode.setDescription("Ground: when on ground, aims at the dir you are moving at to strafe more, Air: automatically does 45deg strafes to gain more speed, Constant: always aims at the dir you are moving at.");
    }

    private int movingTicks, ticks;

    @Override
    public void onEnable() {
        ticks = 0;
    }

    @Override
    public void onDisable() {
        mc.timer.timerSpeed = 1.0f;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get().replace(" ", ""));

        if (!MoveUtil.isMoving()) {
            movingTicks = 0;
            return;
        } else {
            // doing mc.thePlayer.jump() in places where minecraft doesn't causes simulation flags on certain anticheats.
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), mc.thePlayer.onGround);

            movingTicks++;
        }

        if (printAirTicks.get()) {
            ChatUtils.sendMessageClient("Air Ticks: " + mc.thePlayer.offGroundTicks);
        }

        if (MoveUtil.isMoving() && MoveUtil.getSpeed() < minSpeed.get() && movingTicks > minMoveTicks.get() && minSpeedLimiter.get()) {
            MoveUtil.strafe(minSpeed.get());
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
            case "AAC":
                switch (aacMode.get()) {
                    case "3.2.0":
                        mc.timer.timerSpeed = 1.025f;
                        break;
                }
                break;
        }
    }

    @EventTarget
    public void onAngle(AngleEvent e) {
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
    }

    @EventTarget
    public void onGravity(GravityEvent e) {
        if (mode.is("Intave") && customGravity.get()) {
            e.setGravityDecrement(0.081);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent e) {
        if (mode.is("Collide")) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer) {
                    continue;
                }

                if (mc.thePlayer.getEntityBoundingBox().expand(1, 1, 1).intersectsWith(player.getEntityBoundingBox())) {
                    MoveUtil.moveFlying(collideSpeed.get());
                }
            }
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (e.isPre()) {
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
                case "AAC":
                    switch (aacMode.get()) {
                        case "3.2.0":
                            MoveUtil.strafe(MoveUtil.getSpeed(), mc.thePlayer.onGround ? 1f : 0.5f);

                            if (mc.thePlayer.offGroundTicks == 9) {
                                mc.thePlayer.motionY -= 0.45f;
                            }
                            break;
                    }
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
                        mc.thePlayer.motionY = -(mc.thePlayer.posY - (mc.thePlayer.posY - (mc.thePlayer.posY % 0.015625)));
                    } else {
                        if (mc.thePlayer.onGround) {
                            MoveUtil.strafe(1.01);
                        } else {
                            MoveUtil.strafe(0.41);
                        }
                    }
                }
                break;
        }
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
            case "AAC":
                if (aacMode.is("3.2.0")) {
                    e.setMotionY((float) ((double) 0.39f));
                }
                break;
        }
    }
}