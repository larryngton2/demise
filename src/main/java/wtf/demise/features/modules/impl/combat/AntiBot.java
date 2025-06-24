package wtf.demise.features.modules.impl.combat;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.MultiBoolValue;

import java.util.ArrayList;
import java.util.Arrays;

@ModuleInfo(name = "AntiBot", description = "Makes Combat-related modules ignore bots.", category = ModuleCategory.Combat)
public class AntiBot extends Module {
    public final MultiBoolValue options = new MultiBoolValue("Options", Arrays.asList(
            new BoolValue("Tab", false),
            new BoolValue("Hypixel", false),
            new BoolValue("Simple", false))
            , this);
    public final ArrayList<EntityPlayer> bots = new ArrayList<>();
    private static final String VALID_USERNAME_REGEX = "^[a-zA-Z0-9_]{1,16}+$";

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player != mc.thePlayer)
                if (isBot(player))
                    bots.add(player);
        }
    }

    @EventTarget
    public void onWorld(WorldChangeEvent event) {
        bots.clear();
    }

    @Override
    public void onDisable() {
        bots.clear();
    }

    public boolean isBot(EntityPlayer player) {
        if (!isEnabled()) return false;

        if (options.isEnabled("Tab")) {
            boolean found = false;
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equals(player.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) return true;
        }

        if (options.isEnabled("Hypixel")) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                return info.getGameProfile().getId().compareTo(player.getUniqueID()) != 0 || this.nameStartsWith(player, "[NPC] ") || !player.getName().matches(VALID_USERNAME_REGEX);
            }
        }

        if (options.isEnabled("Simple")) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                return info.getDisplayName().getFormattedText().contains("BOT") || info.getDisplayName().getFormattedText().contains("NPC");
            }
        }

        return false;
    }

    private boolean nameStartsWith(EntityPlayer player, String prefix) {
        return EnumChatFormatting.getTextWithoutFormattingCodes(player.getDisplayName().getUnformattedText()).startsWith(prefix);
    }
}