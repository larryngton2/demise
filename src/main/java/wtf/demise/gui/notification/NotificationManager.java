package wtf.demise.gui.notification;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.ScaledResolution;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.render.RenderUtils;

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
        }
    }

    public void publish(ScaledResolution sr, boolean shader) {
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
                float actualOffset = 0;
                Interface anInterface = INSTANCE.getModuleManager().getModule(Interface.class);

                x = (float) (sr.getScaledWidth() - (width + 5) * animation.getOutput());
                y = sr.getScaledHeight() - 5 - height - yOffset;

                if (!shader) {
                    notification.getAnimation().setDuration(150);
                    actualOffset = 10;

                    RenderUtils.drawRect(x, y, width, height, anInterface.bgColor());
                    Fonts.interSemiBold.get(17).drawStringWithShadow(notification.getTitle(), x + 3, y + 5, new Color(255, 255, 255, 255).getRGB());
                    Fonts.interRegular.get(17).drawStringWithShadow(notification.getDescription(), x + 3, y + 15, new Color(anInterface.color()).brighter().getRGB());
                    RenderUtils.drawRect(x, y + height - 1, width * Math.min((notification.getTimerUtils().getTime() / notification.getTime()), 1), 1, INSTANCE.getModuleManager().getModule(Interface.class).color());
                } else {
                    RenderUtils.drawRect(x, y, width, height, Color.black.getRGB());
                }

                yOffset += (height + actualOffset) * (float) notification.getAnimation().getOutput();
            }
        }
    }
}