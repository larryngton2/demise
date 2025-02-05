package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.Minecraft;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;

@ModuleInfo(name = "Fly", category = ModuleCategory.Movement)
public class Fly extends Module {

    public final ModeValue mode = new ModeValue("Mode", new String[]{"Vanilla"}, "Vanilla", this);
    private final SliderValue speed = new SliderValue("Speed", 2f, 1f, 5f, 0.1f, this, () -> mode.is("Vanilla"));

    public void onDisable() {
        if (mode.get().equals("Vanilla")) {
            if (Minecraft.getMinecraft().thePlayer == null)
                return;

            if (Minecraft.getMinecraft().thePlayer.capabilities.isFlying) {
                Minecraft.getMinecraft().thePlayer.capabilities.isFlying = false;
            }

            Minecraft.getMinecraft().thePlayer.capabilities.setFlySpeed(0.05F);
        }

    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mode.get().equals("Vanilla")) {
            mc.thePlayer.motionY = 0.0D;
            mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806D * speed.get()));
            mc.thePlayer.capabilities.isFlying = true;
        }
    }
}