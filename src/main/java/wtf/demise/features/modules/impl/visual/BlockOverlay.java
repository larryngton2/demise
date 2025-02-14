package wtf.demise.features.modules.impl.visual;

import net.minecraft.block.BlockAir;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ColorValue;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

import java.awt.*;

@ModuleInfo(name = "BlockOverlay", category = ModuleCategory.Visual)
public class BlockOverlay extends Module {

    public final BoolValue outline = new BoolValue("Outline", true, this);
    public final BoolValue filled = new BoolValue("Filled", false, this);
    public final BoolValue syncColor = new BoolValue("Sync Color", false, this);
    public final ColorValue color = new ColorValue("Color", new Color(255, 255, 255), this, () -> !syncColor.get());

    @EventTarget
    public void onRender3D(Render3DEvent event) {

        if (PlayerUtils.getBlock(mc.objectMouseOver.getBlockPos()) instanceof BlockAir)
            return;

        if (syncColor.get()) {
            RenderUtils.renderBlock(mc.objectMouseOver.getBlockPos(), getModule(Interface.class).color(0), outline.get(), filled.get());
        } else {
            RenderUtils.renderBlock(mc.objectMouseOver.getBlockPos(), color.get().getRGB(), outline.get(), filled.get());
        }

    }
}
