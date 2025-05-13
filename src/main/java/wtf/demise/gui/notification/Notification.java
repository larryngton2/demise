package wtf.demise.gui.notification;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.ContinualAnimation;
import wtf.demise.utils.animations.Translate;
import wtf.demise.utils.animations.impl.EaseOutSine;
import wtf.demise.utils.math.TimerUtils;

@Getter
public class Notification implements InstanceAccess {
    private final NotificationType notificationType;
    private final String title, description;
    private final float time;
    private final TimerUtils timerUtils;
    private final Animation animation;
    private final Translate translate;

    public Notification(NotificationType type, String title, String description) {
        this(type, title, description, Demise.INSTANCE.getNotificationManager().getToggleTime());
    }

    public Notification(NotificationType type, String title, String description, float time) {
        this.title = title;
        this.description = description;
        this.time = (long) (time * 1000);
        timerUtils = new TimerUtils();
        this.notificationType = type;
        this.animation = new EaseOutSine(250, 1);
        final ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        this.translate = new Translate(sr.getScaledWidth() - this.getWidth(), sr.getScaledHeight() - getHeight());
    }

    public double getWidth() {
        return Math.max(Math.max(Fonts.interSemiBold.get(17).getStringWidth(getTitle()), Fonts.interRegular.get(17).getStringWidth(getDescription())) + 8, 120);
    }

    public double getHeight() {
        return 26;
    }
}