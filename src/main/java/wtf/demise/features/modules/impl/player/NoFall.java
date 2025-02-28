package wtf.demise.features.modules.impl.player;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.C03PacketPlayer;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.MotionEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.packet.BlinkComponent;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.PlayerUtils;

@ModuleInfo(name = "NoFall", category = ModuleCategory.Player)
public class NoFall extends Module {

    public final ModeValue mode = new ModeValue("Mode", new String[]{"NoGround", "Extra", "Blink", "Watchdog"}, "NoGround", this);
    public final SliderValue distance = new SliderValue("Fall distance", 3, 0, 8, 1, this, () -> !mode.is("NoGround"));

    private boolean blinked = false;
    private boolean prevOnGround = false;
    private double fallDistance = 0;
    private boolean timed = false;

    @Override
    public void onEnable() {
        if (PlayerUtils.nullCheck())
            fallDistance = mc.thePlayer.fallDistance;
    }

    @Override
    public void onDisable() {
        if (blinked) {
            BlinkComponent.dispatch();
            blinked = false;
        }
        mc.timer.timerSpeed = 1f;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        setTag(mode.get());
        if (!PlayerUtils.nullCheck())
            return;

        if (event.isPost())
            return;

        if (mc.thePlayer.onGround)
            fallDistance = 0;
        else {
            fallDistance += (float) Math.max(mc.thePlayer.lastTickPosY - event.getY(), 0);

            fallDistance -= MoveUtil.predictedMotionY(mc.thePlayer.motionY, 1);
        }

        if (mc.thePlayer.capabilities.allowFlying) return;
        if (isVoid()) {
            if (blinked) {
                BlinkComponent.dispatch();
                blinked = false;
            }
            return;
        }

        switch (mode.get()) {
            case "NoGround":
                event.setOnGround(false);
                break;

            case "Extra":
                float extra$fallDistance = mc.thePlayer.fallDistance;
                mc.timer.timerSpeed = 1f;
                if (extra$fallDistance - mc.thePlayer.motionY > distance.get()) {
                    mc.timer.timerSpeed = 0.5f;
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                    mc.thePlayer.fallDistance = 0;
                }
                break;

            case "Watchdog":
                if (fallDistance >= distance.get() && !isEnabled(Scaffold.class)) {
                    mc.timer.timerSpeed = (float) 0.5;
                    timed = true;
                    mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
                    fallDistance = 0;
                } else if (timed) {
                    mc.timer.timerSpeed = 1;
                    timed = false;
                }
                break;
            case "Blink":
                if (mc.thePlayer.onGround) {
                    if (blinked) {
                        BlinkComponent.dispatch();
                        blinked = false;
                    }

                    prevOnGround = true;
                } else if (prevOnGround) {
                    if (shouldBlink()) {
                        if (!BlinkComponent.blinking)
                            BlinkComponent.blinking = true;
                        blinked = true;
                    }

                    prevOnGround = false;
                } else if (PlayerUtils.isBlockUnder() && BlinkComponent.blinking && (fallDistance - mc.thePlayer.motionY) >= distance.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(true));
                    fallDistance = 0.0F;
                }
                break;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        ScaledResolution sr = new ScaledResolution(mc);
        if (mode.is("Blink")) {
            if (blinked)
                mc.fontRendererObj.drawStringWithShadow("Blinking: " + BlinkComponent.packets.size(), (float) sr.getScaledWidth() / 2.0F - (float) mc.fontRendererObj.getStringWidth("Blinking: " + BlinkComponent.packets.size()) / 2.0F, (float) sr.getScaledHeight() / 2.0F + 13.0F, -1);
        }
    }

    private boolean isVoid() {
        return PlayerUtils.overVoid(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    private boolean shouldBlink() {
        return !mc.thePlayer.onGround && !PlayerUtils.isBlockUnder((int) Math.floor(distance.get())) && PlayerUtils.isBlockUnder() && !getModule(Scaffold.class).isEnabled();
    }
}
