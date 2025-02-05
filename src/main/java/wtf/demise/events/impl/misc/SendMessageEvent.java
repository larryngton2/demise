package wtf.demise.events.impl.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import wtf.demise.events.impl.CancellableEvent;

@Getter
@AllArgsConstructor
public class SendMessageEvent extends CancellableEvent {
    private final String message;
}
