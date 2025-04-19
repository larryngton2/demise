package wtf.demise.features.modules.impl.visual;

import net.minecraft.util.MovementInput;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.gui.font.Fonts;
import wtf.demise.utils.player.MoveUtil;

@ModuleInfo(name = "MoveStatus", category = ModuleCategory.Visual)
public class MoveStatus extends Module {
    private final BoolValue lowercase = new BoolValue("Lowercase", false, this);

    private String status, status2, status3;

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        String stateKey = (MoveUtil.isMoving() ? "M" : "S") + (mc.thePlayer.isSprinting() ? "P" : "") + (mc.thePlayer.isSneaking() ? "N" : "");

        status = switch (stateKey) {
            case "SN", "MN", "MPN" -> "Sneak";
            case "MP" -> "Sprint";
            case "M" -> "Walk";
            default -> "";
        };

        status2 = mc.thePlayer.onGround ? "Ground" : "Air";

        MovementInput ts = mc.thePlayer.movementInput;
        StringBuilder statusBuilder = new StringBuilder();

        if (ts.moveForward > 0f) {
            statusBuilder.append("W");
        }

        if (ts.moveStrafe > 0f) {
            statusBuilder.append("A");
        }

        if (ts.moveForward < 0f) {
            statusBuilder.append("S");
        }

        if (ts.moveStrafe < 0f) {
            statusBuilder.append("D");
        }

        status3 = statusBuilder.toString();
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        Fonts.interSemiBold.get(15).drawCenteredStringWithShadow((lowercase.get() ? status.toLowerCase() : status) + " " + (lowercase.get() ? status2.toLowerCase() : status2) + " " + status3, (float) e.scaledResolution().getScaledWidth() / 2, (float) e.scaledResolution().getScaledHeight() / 2 + 25, Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1));
    }
}