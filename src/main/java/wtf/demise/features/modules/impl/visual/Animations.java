package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.render.RenderItemEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.ModeValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.misc.SpoofSlotUtils;

@ModuleInfo(name = "Animations", description = "Modifies blocking animations.")
public class Animations extends Module {
    public final BoolValue blockOnSwing = new BoolValue("Block on swing", true, this);
    private final ModeValue blockAnimation = new ModeValue("Block animation", new String[]{"Default", "Exhibition", "Spin"}, "Default", this);

    private final SliderValue translatedX = new SliderValue("Translated X", 0, -0.2f, 0.2f, 0.01f, this);
    private final SliderValue translatedY = new SliderValue("Translated Y", 0, -0.2f, 0.2f, 0.01f, this);
    private final SliderValue translatedZ = new SliderValue("Translated Z", 0, -0.2f, 0.2f, 0.01f, this);

    @EventTarget
    public void onRenderItem(RenderItemEvent e) {
        if (e.getItemToRender().getItem() instanceof ItemMap) {
            return;
        }

        if (blockOnSwing.get() && mc.thePlayer.isSwingInProgress && SpoofSlotUtils.getSpoofedStack() != null && SpoofSlotUtils.getSpoofedStack().getItem() instanceof ItemSword) {
            e.setUseItem(true);
        }

        if (e.isUseItem() && e.getEnumAction() == EnumAction.BLOCK) {
            GlStateManager.translate(translatedX.get(), translatedY.get(), translatedZ.get());

            if (blockAnimation.is("Default")) {
                return;
            }

            switch (blockAnimation.get()) {
                case "Exhibition": {
                    float funny = MathHelper.sin(MathHelper.sqrt_float(e.getSwingProgress()) * (float) Math.PI);
                    ItemRenderer.transformFirstPersonItem(e.getProgress() / 2.0F, 0.0F);
                    GlStateManager.translate(0.0F, 0.3F, -0.0F);
                    GlStateManager.rotate(-funny * 31.0F, 1, 0, 2.0F);
                    GlStateManager.rotate(-funny * 33.0F, 1.5F, (funny / 1.1F), 0F);
                    ItemRenderer.doBlockTransformations();
                    break;
                }
                case "Spin": {
                    GlStateManager.translate(0.1f, -0.125f, -0.1f);
                    ItemRenderer.transformFirstPersonItem(e.getProgress() / 2f, 0.0f);
                    GlStateManager.rotate(45f, -0.0f, 0.0f, -0.1f);
                    GlStateManager.rotate(System.currentTimeMillis() % 360, -0.1f, 0.1f, -0.1f);
                    ItemRenderer.doBlockTransformations();
                    break;
                }
            }

            e.setCancelled(true);
        }
    }
}