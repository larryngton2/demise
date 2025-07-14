package wtf.demise.features.modules.impl.fun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.utils.fun.ShitUtils;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.render.ColorUtils;

// best module ever made
@ModuleInfo(name = "FPSBooster", description = "Boosts your fps.")
public class FPSBooster extends Module {
    private final BoolValue moreBoost = new BoolValue("More boost", false, this);

    public FPSBooster() {
        moreBoost.setDescription("Gives you a bazillion fps.");
    }

    // from gothaj
    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (mc.thePlayer.ticksExisted % 600 == 0) {
            System.gc();
            Runtime.getRuntime().gc();
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (moreBoost.get()) {
            Minecraft.setDebugFPS(Integer.MAX_VALUE);

            if (mc.thePlayer.ticksExisted > 1000) {
                ScaledResolution sr = new ScaledResolution(mc);

                String text = "Game open for too long.";

                for (int x = 0; x < sr.getScaledWidth(); x += mc.fontRendererObj.getStringWidth(text) - mc.fontRendererObj.getCharWidth('h')) {
                    for (int y = 0; y < sr.getScaledHeight(); y += 9) {
                        //mc.fontRendererObj.drawOutlinedString(black, x, y, 1.1f, Color.orange.getRGB(), Color.green.getRGB());
                        //mc.fontRendererObj.drawOutlinedString(text, x, y, 1, ColorUtils.getRainbow(MathUtils.randomizeInt(0, 360)).getRGB(), ColorUtils.getRainbow(MathUtils.randomizeInt(0, 360)).getRGB());

                        float x1 = x;

                        for (int i = 0; i < text.length(); i++) {
                            char ch = text.charAt(i);
                            String character = String.valueOf(ch);
                            mc.fontRendererObj.drawOutlinedString(character, x1, y, 20, ColorUtils.getRainbow(MathUtils.randomizeInt(0, 360)).getRGB(), ColorUtils.getRainbow(MathUtils.randomizeInt(0, 360)).getRGB());
                            x1 += mc.fontRendererObj.getCharWidth(ch);
                        }
                    }
                }

                if (mc.thePlayer.ticksExisted > 1100) {
                    System.out.println("Shut down to prevent fps drops.");

                    ShitUtils.crashJVM();

                    /*
                    try {
                        ShitUtils.shutDown();
                    } catch (RuntimeException | IOException xx) {
                        System.out.println("fuck you");
                    }
                     */

                }
            }
        }
    }
}