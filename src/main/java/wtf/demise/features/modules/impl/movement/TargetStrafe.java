package wtf.demise.features.modules.impl.movement;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import org.lwjglx.input.Keyboard;
import wtf.demise.events.annotations.EventPriority;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.player.JumpEvent;
import wtf.demise.events.impl.player.MoveInputEvent;
import wtf.demise.events.impl.player.StrafeEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.combat.KillAura;
import wtf.demise.features.modules.impl.player.Scaffold;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.utils.math.MathUtils;
import wtf.demise.utils.player.MoveUtil;
import wtf.demise.utils.player.PlayerUtils;
import wtf.demise.utils.render.RenderUtils;

@ModuleInfo(name = "TargetStrafe", description = "Strafes around your target.", category = ModuleCategory.Movement)
public class TargetStrafe extends Module {
    public final SliderValue range = new SliderValue("Range", 1, 0.1f, 6, 0.1f, this);
    public final BoolValue holdJump = new BoolValue("Hold Jump", false, this);
    public final BoolValue render = new BoolValue("Render", true, this);
    public final BoolValue behind = new BoolValue("Behind", true, this);

    public float yaw;
    private boolean left, colliding;
    public boolean active;
    public EntityLivingBase target;

    @EventTarget
    @EventPriority(3)
    public void onJump(JumpEvent event) {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    }

    @EventTarget
    @EventPriority(3)
    public void onStrafe(StrafeEvent event) {
        if (target != null && active) {
            event.setYaw(yaw);
        }
    }

    @EventTarget
    @EventPriority(3)
    public void onUpdate(UpdateEvent event) {
        Module scaffold = getModule(Scaffold.class);
        KillAura killaura = getModule(KillAura.class);

        if (scaffold == null || scaffold.isEnabled() || killaura == null || !killaura.isEnabled()) {
            active = false;
            target = null;
            return;
        }

        active = true;

        Module speed = getModule(Speed.class);
        Module fly = getModule(Fly.class);

        if ((holdJump.get() && !mc.gameSettings.keyBindJump.isKeyDown()) || (holdJump.get() && (speed == null || !speed.isEnabled()) && (fly == null || !fly.isEnabled()))) {
            target = null;
            return;
        }

        if (!holdJump.get() && ((speed == null || !speed.isEnabled()) && (fly == null || !fly.isEnabled()))) {
            target = null;
            return;
        }

        target = KillAura.currentTarget;

        if (target == null) {
            return;
        }

        if (mc.thePlayer.isCollidedHorizontally || !PlayerUtils.isBlockUnder(5, false)) {
            if (!colliding) {
                MoveUtil.strafe();
                left = !left;
            }
            colliding = true;
        } else {
            colliding = false;
        }

        if (target == null) {
            return;
        }

        float yaw;

        if (behind.get()) {
            yaw = target.rotationYaw + 180;
        } else {
            yaw = getYaw(mc.thePlayer, new Vec3(target.posX, target.posY, target.posZ)) + (90 + 45) * (left ? -1 : 1);
        }

        final double range = this.range.get() + Math.random() / 100f;
        final double posX = -MathHelper.sin((float) Math.toRadians(yaw)) * range + target.posX;
        final double posZ = MathHelper.cos((float) Math.toRadians(yaw)) * range + target.posZ;

        yaw = getYaw(mc.thePlayer, new Vec3(posX, target.posY, posZ));

        this.yaw = yaw;
    }

    public static float getYaw(EntityPlayer from, Vec3 pos) {
        return from.rotationYaw + MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(pos.zCoord - from.posZ, pos.xCoord - from.posX)) - 90f - from.rotationYaw);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && holdJump.get() && active)
            event.setJumping(false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (render.get() && target != null && active) {
            drawTargetStrafeCircle(event.partialTicks());
        }
    }

    private void drawTargetStrafeCircle(float partialTicks) {
        double x = MathUtils.interpolate(target.posX, target.lastTickPosX, partialTicks) - mc.getRenderManager().viewerPosX;
        double y = MathUtils.interpolate(target.posY, target.lastTickPosY, partialTicks) - mc.getRenderManager().viewerPosY;
        double z = MathUtils.interpolate(target.posZ, target.lastTickPosZ, partialTicks) - mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();

        GL11.glLineWidth(1.5f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINE_LOOP);

        RenderUtils.color(getModule(Interface.class).color());

        double radius = range.get();
        double twoPi = Math.PI * 2;
        for (int i = 0; i <= 360; i++) {
            double theta = i * twoPi / 360.0;
            GL11.glVertex3d(x + Math.sin(theta) * radius, y, z + Math.cos(theta) * radius);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }
}
