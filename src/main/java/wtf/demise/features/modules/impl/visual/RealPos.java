package wtf.demise.features.modules.impl.visual;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.packet.PacketReleaseEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.FakeLag;
import wtf.demise.utils.math.TimerUtils;

import static org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS;

@ModuleInfo(name = "RealPos")
public class RealPos extends Module {
    private double x, y, z;
    private final TimerUtils resetTimer = new TimerUtils();

    @EventTarget
    public void onPacketRelease(PacketReleaseEvent e) {
        if (e.getTimedPacket().getPacket() instanceof C03PacketPlayer c03) {
            x = c03.getPositionX();
            y = c03.getPositionY();
            z = c03.getPositionZ();
            resetTimer.reset();
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent e) {
        if (getModule(FakeLag.class).isEnabled()) {
            setEnabled(false);
        }

        if (!resetTimer.hasTimeElapsed(1000)) {
            if (mc.gameSettings.thirdPersonView == 0) return;

            double x = this.x - mc.getRenderManager().viewerPosX;
            double y = this.y - mc.getRenderManager().viewerPosY;
            double z = this.z - mc.getRenderManager().viewerPosZ;

            GlStateManager.pushMatrix();
            GL11.glPushAttrib(GL_ALL_ATTRIB_BITS);
            float lightLevel = mc.theWorld.getLight(new BlockPos(mc.thePlayer.getPositionVector()));
            GlStateManager.color(lightLevel, lightLevel, lightLevel);
            mc.getRenderManager().doRenderEntity(mc.thePlayer, x, y, z, mc.thePlayer.rotationYawHead, e.partialTicks(), true, true);
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }
    }
}