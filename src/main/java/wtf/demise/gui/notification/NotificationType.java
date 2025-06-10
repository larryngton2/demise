package wtf.demise.gui.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
    ENABLE,
    DISABLE,
    ERROR,
    SUCCESS,
    INFO
}