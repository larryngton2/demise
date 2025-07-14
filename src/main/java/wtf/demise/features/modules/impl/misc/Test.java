package wtf.demise.features.modules.impl.misc;

import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Test", description = "test 12345")
public class Test extends Module {
    private final SliderValue h = new SliderValue("h", 0, 0, 0, 0, this);

    public Test() {
        h.setDescription("This should be a really long description because an Integer's max value is 2,147,483,647, and oh yeah it was actually 271,307 instead of 6 mil. Also, idk demise best clietn lolloolo. Lore em imsup sit dollar emmit.");
    }
}