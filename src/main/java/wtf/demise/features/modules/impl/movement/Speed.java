package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInput;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.player.MoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.ChatUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.MovementCorrection;
import wtf.demise.utils.player.RotationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static wtf.demise.utils.player.MoveUtil.getBaseMoveSpeed;

@ModuleInfo(name = "Speed", description = "Makes you go faster.", category = ModuleCategory.Movement)
public class Speed extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Strafe Hop", "NCP", "Verus", "Legit", "Intave", "Vulcan", "BMC"}, "Strafe Hop", this);
    private final BoolValue smooth = new BoolValue("Smooth", false, this, () -> mode.is("Strafe Hop"));
    private final BoolValue ground = new BoolValue("Ground", true, this, () -> mode.is("Strafe Hop"));
    private final BoolValue air = new BoolValue("Air", true, this, () -> mode.is("Strafe Hop"));
    private final ModeValue ncpMode = new ModeValue("NCP mode", new String[]{"On tick 4", "On tick 5", "Old Hop", "BHop"}, "On tick 5", this, () -> mode.is("NCP"));
    private final ModeValue verusMode = new ModeValue("Verus mode", new String[]{"Low"}, "Low", this, () -> mode.is("Verus"));
    private final SliderValue speedMulti = new SliderValue("Extra speed multiplier", 0.4f, 0f, 1f, 0.01f, this, () -> ncpMode.is("Old Hop") && ncpMode.canDisplay());
    private final ModeValue intaveMode = new ModeValue("Intave mode", new String[]{"Safe", "Fast"}, "Safe", this, () -> mode.is("Intave"));
    private final BoolValue timer = new BoolValue("Timer", false, this, () -> mode.is("Intave") && intaveMode.is("Fast"));
    private final SliderValue iBoostMulti = new SliderValue("Boost multiplier", 1, 0f, 1, 0.1f, this, () -> mode.is("Intave") && intaveMode.is("Safe"));
    private final ModeValue yawOffsetMode = new ModeValue("Yaw offset", new String[]{"None", "Ground", "Air", "Constant"}, "Air", this);
    private final BoolValue minSpeedLimiter = new BoolValue("Min speed limiter", false, this);
    private final SliderValue minSpeed = new SliderValue("Min speed", 0.25f, 0, 1, 0.01f, this, minSpeedLimiter::get);
    private final SliderValue minMoveTicks = new SliderValue("Move ticks for limit", 15, 0, 40, 1, this, minSpeedLimiter::get);
    private final BoolValue printAirTicks = new BoolValue("Print airTicks", false);

    private int movingTicks, stoppedTicks, ticks;

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
        moveSpeed = getBaseMoveSpeed();
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        this.setTag(mode.get().replace(" ", ""));

        if (!MoveUtil.isMoving()) {
            movingTicks = 0;
            stoppedTicks++;
            return;
        } else {
            if (mc.thePlayer.onGround && !mode.is("Legit")) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            }

            movingTicks++;
            stoppedTicks = 0;
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
                    RotationUtils.setRotation(new float[]{MoveUtil.getYawFromKeybind(), mc.thePlayer.rotationPitch}, MovementCorrection.Silent, 180, 180);
                }
                break;
            case "Air":
                if (!mc.thePlayer.onGround) {
                    RotationUtils.setRotation(new float[]{mc.thePlayer.rotationYaw + 45, mc.thePlayer.rotationPitch}, MovementCorrection.Silent, 180, 180);
                }
                break;
            case "Constant":
                RotationUtils.setRotation(new float[]{MoveUtil.getYawFromKeybind(), mc.thePlayer.rotationPitch}, MovementCorrection.Silent, 180, 180);
                break;
        }

        switch (mode.get()) {
            case "NCP":
                if (ncpMode.is("On tick 4")) {
                    if (mc.thePlayer.offGroundTicks == 4 && mc.thePlayer.posY % 1.0 == 0.16610926093821377) {
                        mc.thePlayer.motionY = -0.09800000190734863;
                    }

                    if (mc.thePlayer.offGroundTicks >= 6) {
                        MoveUtil.strafe();
                    }

                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();

                        MoveUtil.strafe(Math.max(0.281, MoveUtil.getSpeed()));
                    }

                    if (MoveUtil.getSpeedEffect() > 0 && mc.thePlayer.offGroundTicks == 3) {
                        mc.thePlayer.motionX *= 1.2;
                        mc.thePlayer.motionZ *= 1.2;
                    }
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
                } else {
                    mc.thePlayer.jump();
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
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                }

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
                                case 1:
                                    mc.thePlayer.motionX *= 1.04;
                                    mc.thePlayer.motionZ *= 1.04;
                                    break;
                                case 2, 3, 4:
                                    mc.thePlayer.motionX *= 1.02;
                                    mc.thePlayer.motionZ *= 1.02;
                                    break;
                            }

                            if (timer.get()) {
                                mc.timer.timerSpeed = 1.002f;
                            }
                            break;
                        }
                    }
                }
                break;
            case "Vulcan":
                if (mc.thePlayer.onGround) {
                    MoveUtil.strafe();
                    mc.thePlayer.jump();
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
        }
    }

    @EventTarget
    public void onMotion(MotionEvent e) {
        if (mode.is("NCP") && ncpMode.is("BHop")) {
            double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
            double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
            lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
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
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                }

                if ((mc.thePlayer.onGround && ground.get()) || (!mc.thePlayer.onGround && air.get())) {
                    if (smooth.get()) {
                        MoveUtil.smoothStrafe(e);
                    } else {
                        MoveUtil.strafe();
                    }
                }
                break;

            case "NCP":
                switch (ncpMode.get()) {
                    case "On tick 5":
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
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
                    case "Old Hop":
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                        }

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
                }
                break;
            case "Legit":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), mc.thePlayer.onGround && MoveUtil.isMoving());
                break;
        }
    }

    @EventTarget
    public void onMove(MoveEvent e) {
        if (!MoveUtil.isMoving() && !mode.is("NCP") && !ncpMode.is("BHop")) {
            return;
        }

        switch (mode.get()) {
            case "Verus":
                if (verusMode.get().equals("Low")) {
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
                ++timerDelay;
                timerDelay %= 5;
                if (timerDelay != 0) {
                    mc.timer.timerSpeed = 1F;
                } else {
                    if (MoveUtil.isMoving())
                        mc.timer.timerSpeed = 32767F;

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
                    moveSpeed = 1.35 * getBaseMoveSpeed() - 0.01;
                } else if (level == 2) {
                    level = 3;
                    mc.thePlayer.motionY = 0.399399995803833;
                    e.setY(0.399399995803833);
                    moveSpeed *= 2.149;
                } else if (level == 3) {
                    level = 4;
                    double difference = 0.66 * (lastDist - getBaseMoveSpeed());
                    moveSpeed = lastDist - difference;
                } else {
                    if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0)).isEmpty() || mc.thePlayer.isCollidedVertically)
                        level = 1;

                    moveSpeed = lastDist - lastDist / 159.0;
                }

                moveSpeed = Math.max(moveSpeed, getBaseMoveSpeed());
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
        if (mode.is("Intave")) {
            e.setMotionY(0.42f - 1.7E-14f);
        }
    }

    public static double roundToOnGround(final double posY) {
        return posY - (posY % 0.015625);
    }
}