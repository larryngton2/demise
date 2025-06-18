package wtf.demise.features.modules.impl.player;

import net.minecraft.inventory.ContainerChest;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.math.TimerUtils;
import wtf.demise.utils.player.InventoryUtils;

@ModuleInfo(name = "Stealer", description = "Steals items from chests.", category = ModuleCategory.Player)
public class Stealer extends Module {
    private final SliderValue startDelay = new SliderValue("Start Delay", 1, 0, 10, 1, this);
    private final SliderValue closeDelay = new SliderValue("Close Delay", 1, 0, 10, 1, this);
    private final SliderValue minDelay = new SliderValue("Min Delay", 1, 0, 10, 1, this);
    private final SliderValue maxDelay = new SliderValue("Max Delay", 1, 0, 10, 1, this);
    public final BoolValue menuCheck = new BoolValue("Menu Check", true, this);

    private final TimerUtils stealTimer = new TimerUtils();
    private final TimerUtils startDelayTimer = new TimerUtils();
    private final TimerUtils closeDelayTimer = new TimerUtils();
    private long currentStealWait;
    public static float[] rotation;
    public int slot;

    private final String[] list = new String[]{"mode", "delivery", "menu", "selector", "game", "gui", "server", "inventory", "play", "teleporter",
            "shop", "melee", "armor", "block", "castle", "mini", "warp", "teleport", "user", "team", "tool", "sure", "trade", "cancel", "accept",
            "soul", "book", "recipe", "profile", "tele", "port", "map", "kit", "select", "lobby", "vault", "lock", "anticheat", "travel", "settings",
            "user", "preference", "compass", "cake", "wars", "buy", "upgrade", "ranged", "potions", "utility"};

    @EventTarget
    private void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.openContainer instanceof ContainerChest container) {
            if (menuCheck.get()) {
                String name = container.getLowerChestInventory().getDisplayName().getUnformattedText().toLowerCase();
                for (String str : list) {
                    if (name.contains(str)) return;
                }
            }

            if (startDelayTimer.hasTimeElapsed(startDelay.get() * 50L)) {
                for (int i = 0; i < container.getLowerChestInventory().getSizeInventory(); ++i) {
                    if (container.getLowerChestInventory().getStackInSlot(i) != null && (stealTimer.hasTimeElapsed(currentStealWait * 50L) || currentStealWait == 0 && InventoryUtils.isValid(container.getLowerChestInventory().getStackInSlot(i)))) {
                        slot = i;
                        mc.playerController.windowClick(container.windowId, i, 0, 1, mc.thePlayer);
                        currentStealWait = MathUtils.randomizeInt(minDelay.get(), maxDelay.get());
                        stealTimer.reset();
                    }
                }
            }

            if (InventoryUtils.isInventoryFull() || InventoryUtils.isInventoryEmpty(container.getLowerChestInventory())) {
                if (closeDelayTimer.hasTimeElapsed(closeDelay.get() * 50L)) {
                    mc.thePlayer.closeScreen();
                }
            } else {
                closeDelayTimer.reset();
            }
        } else {
            currentStealWait = MathUtils.randomizeInt(minDelay.get(), maxDelay.get());
            startDelayTimer.reset();
        }
    }
}