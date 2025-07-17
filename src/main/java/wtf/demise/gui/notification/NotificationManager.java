package wtf.demise.gui.notification;

import lombok.Getter;
import lombok.Setter;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.misc.SoundUtil;
import wtf.demise.utils.render.RoundedUtils;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class NotificationManager implements InstanceAccess {
    private final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    @Setter
    private float toggleTime = 2;

    public void post(NotificationType type, String title, String description) {
        post(new Notification(type, title, description));
    }

    public void post(NotificationType type, String title, String description, float time) {
        post(new Notification(type, title, description, time));
    }

    public void post(NotificationType type, String title) {
        post(new Notification(type, title, title));
    }

    private void post(Notification notification) {
        if (INSTANCE.getModuleManager().getModule(Interface.class).elements.isEnabled("Notification")) {
            notifications.add(notification);

            if (Demise.INSTANCE.getStartTimeLong() > 1000) {
                SoundUtil.notifSound(notification.getNotificationType());
            }
        }
    }

    public void publish(boolean shader, boolean isGlow) {
        float yOffset = 0;

        for (Notification notification : getNotifications()) {
            float width = (float) notification.getWidth();
            float height = (float) notification.getHeight();

            Animation animation = notification.getAnimation();
            animation.setDirection(notification.getTimerUtils().hasTimeElapsed((long) notification.getTime()) ? Direction.BACKWARDS : Direction.FORWARDS);

            if (notification.getAnimation().finished(Direction.BACKWARDS)) {
                getNotifications().remove(notification);
            }

            if (!animation.finished(Direction.BACKWARDS)) {
                float x;
                float y;
                float actualOffset;
                Interface anInterface = INSTANCE.getModuleManager().getModule(Interface.class);

                x = (float) (10 * animation.getOutput());

                float extraY = switch (anInterface.watermarkMode.get()) {
                    case "Text" -> Fonts.urbanist.get(38).getHeight() + 5;
                    case "Blue archive" -> 60;
                    case "Bivir" -> 80;
                    case "Old" -> mc.fontRendererObj.FONT_HEIGHT + 3;
                    default -> 0;
                };

                y = 10 + (anInterface.elements.isEnabled("Watermark") ? (extraY) : 0) - yOffset;

                notification.getAnimation().setDuration(150);
                actualOffset = 7;

                if (!shader) {
                    RoundedUtils.drawRound(x, y, width, height, 7, new Color(anInterface.bgColor(), true));
                    Fonts.interSemiBold.get(17).drawString(notification.getTitle(), x + 4, y + 5, new Color(255, 255, 255, 255).getRGB());
                    Fonts.interRegular.get(17).drawString(notification.getDescription(), x + 4, y + 15, new Color(170, 170, 170).getRGB());
                } else {
                    if (!isGlow) {
                        RoundedUtils.drawShaderRound(x, y, width, height, 7, Color.black);
                    } else {
                        RoundedUtils.drawGradientPreset(x, y, width, height, 7);
                    }
                }

                yOffset -= (height + actualOffset) * (float) (notification.getAnimation().getOutput());
            }
        }
    }
}