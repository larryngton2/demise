package wtf.demise.gui.notification;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.animations.Animation;
import wtf.demise.utils.animations.Direction;
import wtf.demise.utils.animations.Translate;
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

            if (!INSTANCE.getModuleManager().getModule(Interface.class).notificationMode.is("Exhi") && notification.getAnimation().finished(Direction.BACKWARDS)) {
                getNotifications().remove(notification);
            }

            if (!animation.finished(Direction.BACKWARDS)) {
                float x;
                float y;
                float actualOffset = 0;
                switch (INSTANCE.getModuleManager().getModule(Interface.class).notificationMode.get()) {
                    case "Default":
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
                        break;
                    case "Exhi":
                        Translate translate = notification.getTranslate();
                        boolean middlePos = INSTANCE.getModuleManager().getModule(Interface.class).centerNotif.get() && mc.thePlayer != null && (mc.currentScreen instanceof GuiChat || mc.currentScreen == null);
                        int scaledHeight = sr.getScaledHeight();
                        int scaledWidth = sr.getScaledWidth();
                        y = middlePos ? (int) (scaledHeight / 2.0f + 43.0f) : scaledHeight - (mc.currentScreen instanceof GuiChat ? 45 : 31);
                        if (!notification.getTimerUtils().hasTimeElapsed(notification.getTime())) {
                            translate.translate(middlePos ? scaledWidth / 2.0f - (width / 2.0f) : (scaledWidth - width), y + yOffset);
                            if (middlePos) {
                                yOffset += height;
                            }
                        } else {
                            translate.translate(scaledWidth, y + yOffset);
                            if (!middlePos) {
                                yOffset += height;
                            }
                        }
                        if (!shader) {
                            RenderUtils.drawRect((float) translate.getX(), (float) translate.getY(), width, height, new Color(0, 0, 0, 185).getRGB());
                            float percentage = Math.min((notification.getTimerUtils().getTime() / notification.getTime()), 1);
                            RenderUtils.drawRect((float) (translate.getX() + (width * percentage)), (float) (translate.getY() + height - 1), width - (width * percentage), 1, notification.getNotificationType().getColor().getRGB());
                            RenderUtils.drawImage(new ResourceLocation("demise/texture/noti/" + notification.getNotificationType().getName() + ".png"), (float) translate.getX() + 2f, (float) translate.getY() + 4.5f, 18, 18);

                            Fonts.interRegular.get(18).drawStringNoFormat(notification.getTitle(), translate.getX() + 21.5f, translate.getY() + 4.5, -1);
                            Fonts.interRegular.get(14).drawStringNoFormat(notification.getDescription(), translate.getX() + 21.5f, translate.getY() + 15.5, -1);
                        }
                        if (!middlePos) {
                            yOffset -= height;
                        }
                        break;
                }
            }
        }
    }
}