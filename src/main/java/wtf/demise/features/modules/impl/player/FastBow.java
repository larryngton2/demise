package wtf.demise.features.modules.impl.player;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBow;
import net.minecraft.network.play.client.C03PacketPlayer;
import org.lwjglx.input.Mouse;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.packet.PacketUtils;

@ModuleInfo(name = "FastBow", description = "Makes you shoot bows faster.", category = ModuleCategory.Player)
public class FastBow extends Module {
    private final SliderValue speed = new SliderValue("Speed", 32, 1, 100, 1, this);
    private final BoolValue trigger = new BoolValue("Trigger (hood bypass)", true, this);

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (!Mouse.isButtonDown(0) || !(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBow)) {
            return;
        }

        for (int i = 0; i < speed.get(); i++) {
            PacketUtils.sendPacket(new C03PacketPlayer(mc.thePlayer.onGround));
        }

        if (trigger.get()) {
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
        }
    }
}