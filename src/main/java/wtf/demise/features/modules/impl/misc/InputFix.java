package wtf.demise.features.modules.impl.misc;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;

@ModuleInfo(name = "InputFix", description = "Applies the input fix from newer Minecraft versions.", category = ModuleCategory.Misc)
public class InputFix extends Module {
    private GuiScreen prevGuiScreen;
    private final KeyBinding[] keyBindings = new KeyBinding[]{
            mc.gameSettings.keyBindPlayerList,
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindUseItem,
            mc.gameSettings.keyBindSprint,
            mc.gameSettings.keyBindRight,
            mc.gameSettings.keyBindSneak,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindJump,
            mc.gameSettings.keyBindDrop
    };

    @EventTarget
    private void onUpdate(UpdateEvent e) {
        if (mc.currentScreen == null && prevGuiScreen != null) {
            for (KeyBinding keyBinding : this.keyBindings) {
                KeyBinding.setKeyBindState(keyBinding.getKeyCode(), GameSettings.isKeyDown(keyBinding));
            }
        }

        prevGuiScreen = mc.currentScreen;
    }
}