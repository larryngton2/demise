package wtf.demise.events.impl.render;

import lombok.AllArgsConstructor;
import wtf.demise.events.impl.Event;

@AllArgsConstructor
public class ChatGUIEvent implements Event {
    public int mouseX, mouseY;
}
