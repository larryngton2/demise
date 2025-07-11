package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.gui.ScaledResolution;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.MouseMoveEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@ModuleInfo(name = "AFKGui")
public class AFKGui extends Module {
    private final SliderValue afkThreshold = new SliderValue("Afk threshold", 10, 5, 300, 1, this);

    // long instead of an integer because someone might afk for more than 2,147,483,647 ticks
    private long afkTicks;
    private final TimerUtils transitionTimer = new TimerUtils();
    private int bgAlpha;
    private final TimerUtils textTimer = new TimerUtils();
    private final TimerUtils textAlphaTimer = new TimerUtils();
    private int textAlpha;
    private long cachedAFKTicks;

    @EventTarget
    // need high event priority so the overlay gets rendered above (almost) everything
    @EventPriority(100)
    public void onRender2D(Render2DEvent e) {
        if (afkTicks > afkThreshold.get() * 20) {
            if (transitionTimer.hasTimeElapsed(10)) {
                bgAlpha = Math.min(bgAlpha + 1, 255);
                transitionTimer.reset();
            }

            if (textTimer.hasTimeElapsed(4000)) {
                if (textAlphaTimer.hasTimeElapsed(10)) {
                    textAlpha = Math.min(textAlpha + 1, 150);
                    textAlphaTimer.reset();
                }
            }
        } else {
            bgAlpha = Math.max(bgAlpha - 1, 0);
            textAlpha = Math.max(textAlpha - 1, 0);
            textTimer.reset();
        }

        if (afkTicks / 20 > 0) {
            cachedAFKTicks = afkTicks;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        if (bgAlpha > 0) {
            RenderUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, bgAlpha).getRGB());
        }

        if (textAlpha > 0) {
            Fonts.urbanist.get(87).drawCenteredString(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), sr.getScaledWidth() / 2f, 80, new Color(255, 255, 255, textAlpha).getRGB());
            Fonts.interRegular.get(18).drawCenteredString("AFK for " + (cachedAFKTicks / 20) + " seconds", sr.getScaledWidth() / 2f, 132, new Color(200, 200, 200, textAlpha).getRGB());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        afkTicks++;

        if (MoveUtil.isMoving()) {
            afkTicks = 0;
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        afkTicks = 0;
    }

    @EventTarget
    public void onMouseMove(MouseMoveEvent e) {
        if (e.getDeltaX() > 0 || e.getDeltaY() > 0) {
            afkTicks = 0;
        }
    }
}
