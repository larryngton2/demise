package wtf.demise.features.modules.impl.misc;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import wtf.demise.Demise;
import wtf.demise.events.annotations.EventTarget;
import wtf.demise.events.impl.misc.WorldChangeEvent;
import wtf.demise.events.impl.player.UpdateEvent;
import wtf.demise.events.impl.render.Render2DEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.features.modules.Module;
import wtf.demise.features.modules.ModuleCategory;
import wtf.demise.features.modules.ModuleInfo;
import wtf.demise.features.modules.impl.visual.Interface;
import wtf.demise.features.values.impl.BoolValue;
import wtf.demise.features.values.impl.SliderValue;
import wtf.demise.gui.font.Fonts;
import wtf.demise.gui.notification.NotificationType;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@ModuleInfo(name = "Murder Mystery", category = ModuleCategory.Misc)
public class MurderMystery extends Module {

    // murderer
    private final BoolValue murdererFinder = new BoolValue("Murderer finder", true, this);
    private final BoolValue announceMurderer = new BoolValue("Announce murderer", false, this, murdererFinder::get);
    private final BoolValue displayMurderer = new BoolValue("Display murderer name on HUD", true, this, murdererFinder::get);

    // detective
    private final BoolValue detectiveFinder = new BoolValue("Detective finder", true, this);
    private final BoolValue displayDetective = new BoolValue("Display detective name on HUD", true, this, detectiveFinder::get);
    public final BoolValue renderDroppedBow = new BoolValue("Render dropped bow", true, this);

    // hud
    private final SliderValue hudX = new SliderValue("HUD X", 78, 0, 850, 1, this);
    private final SliderValue hudY = new SliderValue("HUD Y", 98, 0, 525, 1, this);

    private EntityPlayer murderer;
    private EntityPlayer detective;

    private final List<Item> knownSwordItems = Arrays.asList(
            Items.golden_carrot,
            Items.carrot,
            Items.carrot_on_a_stick,
            Items.bone,
            Items.fish,
            Items.cooked_fish,
            Items.blaze_rod,
            Items.pumpkin_pie,
            Items.name_tag,
            Items.apple,
            Items.feather,
            Items.cookie,
            Items.shears,
            Items.stick,
            Items.quartz,
            Items.cooked_beef,
            Items.netherbrick,
            Items.cooked_chicken,
            Items.record_11,
            Items.record_13,
            Items.record_cat,
            Items.record_chirp,
            Items.record_far,
            Items.record_mall,
            Items.record_mellohi,
            Items.record_stal,
            Items.record_strad,
            Items.record_ward,
            Items.record_wait,
            Items.boat,
            Items.book,
            Items.speckled_melon,
            Items.prismarine_shard,
            Items.coal,
            Items.flint,
            Items.bread,
            Items.leather,
            Items.iron_sword
    );

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (murderer == null && murdererFinder.get()) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player.getHeldItem() != null) {
                    for (Item item : knownSwordItems) {
                        if (player.getHeldItem().getItem() == item) {
                            if (player != mc.thePlayer) {
                                Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "MurderMystery", player.getName() + " is the murderer!", 3);
                                if (announceMurderer.get()) {
                                    mc.thePlayer.sendChatMessage(player.getName() + " is the murderer");
                                }
                            } else {
                                Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "MurderMystery", "You are the murderer!", 3);
                            }

                            this.murderer = player;
                        }
                    }
                }
            }
        }

        if (detective == null && detectiveFinder.get()) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player.getHeldItem() != null) {
                    if (player.getHeldItem().getItem() instanceof ItemBow) {
                        if (player != mc.thePlayer) {
                            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "MurderMystery", player.getName() + " is the detective!", 3);
                        } else {
                            Demise.INSTANCE.getNotificationManager().post(NotificationType.INFO, "MurderMystery", "You are the detective!", 3);
                        }

                        detective = player;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (displayMurderer.get()) {
            Fonts.interRegular.get(17).drawStringWithShadow("Murderer: ", hudX.get(), hudY.get(), Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1));
            Fonts.interRegular.get(17).drawStringWithShadow((murderer != null ? murderer.getName() : "null"), hudX.get() + Fonts.interRegular.get(17).getStringWidth("Murderer: "), hudY.get(), -1);
        }

        if (displayDetective.get()) {
            Fonts.interRegular.get(17).drawStringWithShadow("Detective: ", hudX.get(), hudY.get() + Fonts.interRegular.get(17).getHeight(), Demise.INSTANCE.getModuleManager().getModule(Interface.class).color(1));
            Fonts.interRegular.get(17).drawStringWithShadow((detective != null ? detective.getName() : "null"), hudX.get() + Fonts.interRegular.get(17).getStringWidth("Detective: "), hudY.get() + Fonts.interRegular.get(17).getHeight(), -1);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!renderDroppedBow.get()) {
            return;
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityArmorStand e && e.getHeldItem().getItem() instanceof ItemBow) {
                double interpolatedX = e.lastTickPosX + (entity.posX - e.lastTickPosX) * event.getPartialTicks();
                double interpolatedY = e.lastTickPosY + (entity.posY - e.lastTickPosY) * event.getPartialTicks();
                double interpolatedZ = e.lastTickPosZ + (entity.posZ - e.lastTickPosZ) * event.getPartialTicks();
                double diffX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.getPartialTicks() - interpolatedX;
                double diffY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.getPartialTicks() - interpolatedY;
                double diffZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.getPartialTicks() - interpolatedZ;

                double dist = MathHelper.sqrt_double(diffX * diffX + diffY * diffY + diffZ * diffZ);

                GlStateManager.pushMatrix();
                drawText(e.getHeldItem().getDisplayName(), interpolatedX, interpolatedY, interpolatedZ, dist);
                GlStateManager.popMatrix();
            }
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent e) {
        murderer = null;
        detective = null;
    }

    private void drawText(String value, double posX, double posY, double posZ, double dist) {
        posX -= mc.getRenderManager().viewerPosX;
        posY -= mc.getRenderManager().viewerPosY;
        posZ -= mc.getRenderManager().viewerPosZ;
        GL11.glPushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(2.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY + 1, (float) posZ);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
        float scale = Math.min(Math.max(0.02266667f, (float) (0.001500000013038516 * dist)), 0.07f);
        GlStateManager.scale(-scale, -scale, -scale);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        Fonts.interSemiBold.get(47).drawOutlinedString(value, -((float) Fonts.interSemiBold.get(47).getStringWidth(value) / 2) + scale * 3.5f, -(123.805f * scale - 2.47494f), Color.blue.getRGB(), Color.BLACK.getRGB());
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }
}
