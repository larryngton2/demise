package wtf.demise.utils.render.shader.impl;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import wtf.demise.Demise;
import wtf.demise.features.modules.impl.visual.MainMenuOptions;
import wtf.demise.utils.InstanceAccess;
import wtf.demise.utils.render.RenderUtils;
import wtf.demise.utils.render.shader.ShaderUtils;

public class MainMenu implements InstanceAccess {
    private static final ShaderUtils mainmenu = new ShaderUtils("mainmenu");

    public static void draw(long initTime) {
        ScaledResolution sr = new ScaledResolution(mc);
        if (Demise.INSTANCE.getModuleManager().getModule(MainMenuOptions.class).shaderMenu.get()) {
            mainmenu.init();
            mainmenu.setUniformf("TIME", (float) (System.currentTimeMillis() - initTime) / 1000);
            mainmenu.setUniformf("RESOLUTION", (float) ((double) sr.getScaledWidth() * sr.getScaleFactor()), (float) ((double) sr.getScaledHeight() * sr.getScaleFactor()));
            ShaderUtils.drawFixedQuads();
            mainmenu.unload();
        } else {
            RenderUtils.drawImage(new ResourceLocation("demise/texture/background.png"), 0, 0, sr.getScaledWidth(), sr.getScaledHeight());
        }
    }
}
