package net.minecraft.client.renderer;

import com.google.common.base.Predicates;
import com.google.gson.JsonSyntaxException;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.src.Config;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.CustomColors;
import net.optifine.GlErrors;
import net.optifine.Lagometer;
import net.optifine.RandomEntities;
import net.optifine.gui.GuiChatOF;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.reflect.ReflectorResolver;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.MemoryMonitor;
import net.optifine.util.TextureUtils;
import net.optifine.util.TimedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.GLContext;
import org.lwjglx.util.glu.Project;
import wtf.demise.Demise;
import wtf.demise.events.impl.misc.MouseOverEvent;
import wtf.demise.events.impl.player.MouseMoveEvent;
import wtf.demise.events.impl.render.Render3DEvent;
import wtf.demise.events.impl.render.ViewBobbingEvent;
import wtf.demise.features.modules.impl.visual.Atmosphere;
import wtf.demise.features.modules.impl.visual.NoHurtCam;
import wtf.demise.features.modules.impl.visual.ThirdPersonDistance;
import wtf.demise.gui.mainmenu.GuiMainMenu;
import wtf.demise.utils.math.MathUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class EntityRenderer implements IResourceManagerReloadListener {
    private static final Logger logger = LogManager.getLogger();
    private static final ResourceLocation locationRainPng = new ResourceLocation("textures/environment/rain.png");
    private static final ResourceLocation locationSnowPng = new ResourceLocation("textures/environment/snow.png");
    public static boolean anaglyphEnable;
    public static int anaglyphField;
    private static Minecraft mc;
    private final IResourceManager resourceManager;
    private final Random random = new Random();
    private static float farPlaneDistance;
    public static ItemRenderer itemRenderer;
    private final MapItemRenderer theMapItemRenderer;
    private int rendererUpdateCount;
    private Entity pointedEntity;
    private static MouseFilter mouseFilterXAxis = new MouseFilter();
    private static MouseFilter mouseFilterYAxis = new MouseFilter();
    private float smoothCamYaw;
    private float smoothCamPitch;
    private float smoothCamFilterX;
    private float smoothCamFilterY;
    private float smoothCamPartialTicks;
    private static float fovModifierHand;
    private static float fovModifierHandPrev;
    private float bossColorModifier;
    private float bossColorModifierPrev;
    private boolean cloudFog;
    private final boolean renderHand = true;
    private final boolean drawBlockOutline = true;
    private long prevFrameTime = Minecraft.getSystemTime();
    private long renderEndNanoTime;
    private final DynamicTexture lightmapTexture;
    private final int[] lightmapColors;
    private static ResourceLocation locationLightMap;
    private boolean lightmapUpdateNeeded;
    private float torchFlickerX;
    private float torchFlickerDX;
    private int rainSoundCounter;
    private final float[] rainXCoords = new float[1024];
    private final float[] rainYCoords = new float[1024];
    private final FloatBuffer fogColorBuffer = GLAllocation.createDirectFloatBuffer(16);
    public float fogColorRed;
    public float fogColorGreen;
    public float fogColorBlue;
    private float fogColor2;
    private float fogColor1;
    private static final boolean debugView = false;
    private ShaderGroup theShaderGroup;
    private static final ResourceLocation[] shaderResourceLocations = new ResourceLocation[]{new ResourceLocation("shaders/post/notch.json"), new ResourceLocation("shaders/post/fxaa.json"), new ResourceLocation("shaders/post/art.json"), new ResourceLocation("shaders/post/bumpy.json"), new ResourceLocation("shaders/post/blobs2.json"), new ResourceLocation("shaders/post/pencil.json"), new ResourceLocation("shaders/post/color_convolve.json"), new ResourceLocation("shaders/post/deconverge.json"), new ResourceLocation("shaders/post/flip.json"), new ResourceLocation("shaders/post/invert.json"), new ResourceLocation("shaders/post/ntsc.json"), new ResourceLocation("shaders/post/outline.json"), new ResourceLocation("shaders/post/phosphor.json"), new ResourceLocation("shaders/post/scan_pincushion.json"), new ResourceLocation("shaders/post/sobel.json"), new ResourceLocation("shaders/post/bits.json"), new ResourceLocation("shaders/post/desaturate.json"), new ResourceLocation("shaders/post/green.json"), new ResourceLocation("shaders/post/blur.json"), new ResourceLocation("shaders/post/wobble.json"), new ResourceLocation("shaders/post/blobs.json"), new ResourceLocation("shaders/post/antialias.json"), new ResourceLocation("shaders/post/creeper.json"), new ResourceLocation("shaders/post/spider.json")};
    public static final int shaderCount = shaderResourceLocations.length;
    private int shaderIndex;
    private boolean useShader;
    public int frameCount;
    private boolean initialized = false;
    private World updatedWorld = null;
    public boolean fogStandard = false;
    private float clipDistance = 128.0F;
    private long lastServerTime = 0L;
    private int lastServerTicks = 0;
    private int serverWaitTime = 0;
    private final ShaderGroup[] fxaaShaders = new ShaderGroup[10];
    private boolean loadVisibleChunks = false;
    private static float interpolatedZoom;

    public EntityRenderer(Minecraft mcIn, IResourceManager resourceManagerIn) {
        shaderIndex = shaderCount;
        useShader = false;
        frameCount = 0;
        mc = mcIn;
        resourceManager = resourceManagerIn;
        itemRenderer = mcIn.getItemRenderer();
        theMapItemRenderer = new MapItemRenderer(mcIn.getTextureManager());
        lightmapTexture = new DynamicTexture(16, 16);
        locationLightMap = mcIn.getTextureManager().getDynamicTextureLocation("lightMap", lightmapTexture);
        lightmapColors = lightmapTexture.getTextureData();
        theShaderGroup = null;

        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 32; ++j) {
                float f = (float) (j - 16);
                float f1 = (float) (i - 16);
                float f2 = MathHelper.sqrt_float(f * f + f1 * f1);
                rainXCoords[i << 5 | j] = -f1 / f2;
                rainYCoords[i << 5 | j] = f / f2;
            }
        }
    }

    public boolean isShaderActive() {
        return OpenGlHelper.shadersSupported && theShaderGroup != null;
    }

    public void stopUseShader() {
        if (theShaderGroup != null) {
            theShaderGroup.deleteShaderGroup();
        }

        theShaderGroup = null;
        shaderIndex = shaderCount;
    }

    public void switchUseShader() {
        useShader = !useShader;
    }

    public void loadEntityShader(Entity entityIn) {
        if (OpenGlHelper.shadersSupported) {
            if (theShaderGroup != null) {
                theShaderGroup.deleteShaderGroup();
            }

            theShaderGroup = null;

            if (entityIn instanceof EntityCreeper) {
                loadShader(new ResourceLocation("shaders/post/creeper.json"));
            } else if (entityIn instanceof EntitySpider) {
                loadShader(new ResourceLocation("shaders/post/spider.json"));
            } else if (entityIn instanceof EntityEnderman) {
                loadShader(new ResourceLocation("shaders/post/invert.json"));
            } else if (Reflector.ForgeHooksClient_loadEntityShader.exists()) {
                Reflector.call(Reflector.ForgeHooksClient_loadEntityShader, entityIn, this);
            }
        }
    }

    public void activateNextShader() {
        if (OpenGlHelper.shadersSupported && mc.getRenderViewEntity() instanceof EntityPlayer) {
            if (theShaderGroup != null) {
                theShaderGroup.deleteShaderGroup();
            }

            shaderIndex = (shaderIndex + 1) % (shaderResourceLocations.length + 1);

            if (shaderIndex != shaderCount) {
                loadShader(shaderResourceLocations[shaderIndex]);
            } else {
                theShaderGroup = null;
            }
        }
    }

    public void loadShader(ResourceLocation resourceLocationIn) {
        if (OpenGlHelper.isFramebufferEnabled()) {
            try {
                theShaderGroup = new ShaderGroup(mc.getTextureManager(), resourceManager, mc.getFramebuffer(), resourceLocationIn);
                theShaderGroup.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
                useShader = true;
            } catch (IOException | JsonSyntaxException ioexception) {
                logger.warn("Failed to load shader: {}", resourceLocationIn, ioexception);
                shaderIndex = shaderCount;
                useShader = false;
            }
        }
    }

    public void onResourceManagerReload(IResourceManager resourceManager) {
        if (theShaderGroup != null) {
            theShaderGroup.deleteShaderGroup();
        }

        theShaderGroup = null;

        if (shaderIndex != shaderCount) {
            loadShader(shaderResourceLocations[shaderIndex]);
        } else {
            loadEntityShader(mc.getRenderViewEntity());
        }
    }

    public void updateRenderer() {
        if (OpenGlHelper.shadersSupported && ShaderLinkHelper.getStaticShaderLinkHelper() == null) {
            ShaderLinkHelper.setNewStaticShaderLinkHelper();
        }

        updateFovModifierHand();
        updateTorchFlicker();
        fogColor2 = fogColor1;

        if (mc.gameSettings.smoothCamera) {
            float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;
            smoothCamFilterX = mouseFilterXAxis.smooth(smoothCamYaw, 0.05F * f1);
            smoothCamFilterY = mouseFilterYAxis.smooth(smoothCamPitch, 0.05F * f1);
            smoothCamPartialTicks = 0.0F;
            smoothCamYaw = 0.0F;
            smoothCamPitch = 0.0F;
        } else {
            smoothCamFilterX = 0.0F;
            smoothCamFilterY = 0.0F;
            mouseFilterXAxis.reset();
            mouseFilterYAxis.reset();
        }

        if (mc.getRenderViewEntity() == null) {
            mc.setRenderViewEntity(mc.thePlayer);
        }

        Entity entity = mc.getRenderViewEntity();
        double d2 = entity.posX;
        double d0 = entity.posY + (double) entity.getEyeHeight();
        double d1 = entity.posZ;
        float f2 = mc.theWorld.getLightBrightness(new BlockPos(d2, d0, d1));
        float f3 = (float) mc.gameSettings.renderDistanceChunks / 16.0F;
        f3 = MathHelper.clamp_float(f3, 0.0F, 1.0F);
        float f4 = f2 * (1.0F - f3) + f3;
        fogColor1 += (f4 - fogColor1) * 0.1F;
        ++rendererUpdateCount;
        itemRenderer.updateEquippedItem();
        addRainParticles();
        bossColorModifierPrev = bossColorModifier;

        if (BossStatus.hasColorModifier) {
            bossColorModifier += 0.05F;

            if (bossColorModifier > 1.0F) {
                bossColorModifier = 1.0F;
            }

            BossStatus.hasColorModifier = false;
        } else if (bossColorModifier > 0.0F) {
            bossColorModifier -= 0.0125F;
        }
    }

    public ShaderGroup getShaderGroup() {
        return theShaderGroup;
    }

    public void updateShaderGroupSize(int width, int height) {
        if (OpenGlHelper.shadersSupported) {
            if (theShaderGroup != null) {
                theShaderGroup.createBindFramebuffers(width, height);
            }

            mc.renderGlobal.createBindEntityOutlineFbs(width, height);
        }
    }

    public void getMouseOver(float partialTicks) {
        Entity entity = mc.getRenderViewEntity();

        if (entity != null && mc.theWorld != null) {
            mc.mcProfiler.startSection("pick");
            mc.pointedEntity = null;
            double blockReachDistance = mc.playerController.getBlockReachDistance();

            double reach = 3.0D;
            float expand = 0;

            MouseOverEvent mouseOverEvent = new MouseOverEvent(reach, expand);
            Demise.INSTANCE.getEventManager().call(mouseOverEvent);

            reach = Math.max(reach, mouseOverEvent.getRange());
            blockReachDistance = Math.max(blockReachDistance, mouseOverEvent.getRange() + 1.5);

            mc.objectMouseOver = entity.rayTrace(blockReachDistance, partialTicks);
            double distance = blockReachDistance;
            final Vec3 vec3 = entity.getPositionEyes(partialTicks);
            boolean flag = false;

            expand = mouseOverEvent.getExpand();

            if (mouseOverEvent.getMovingObjectPosition() != null) {
                mc.objectMouseOver = mouseOverEvent.getMovingObjectPosition();
                return;
            }

            if (mc.playerController.extendedReach()) {
                blockReachDistance = 6.0D;
                distance = 6.0D;
            } else if (blockReachDistance > 3.0D) {
                flag = true;
            }

            if (mc.objectMouseOver != null) {
                distance = mc.objectMouseOver.hitVec.distanceTo(vec3);
            }

            final Vec3 vec31 = entity.getLook(partialTicks);
            final Vec3 vec32 = vec3.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);
            pointedEntity = null;
            Vec3 vec33 = null;
            final float f = 1.0F;
            final List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
            double d2 = distance;

            for (final Entity entity1 : list) {
                final float f1 = entity1.getCollisionBorderSize() + ((entity instanceof EntityPlayer && !entity.isInvisible()) ? expand : 0);
                final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        boolean flag1 = false;

                        if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                            flag1 = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract);
                        }

                        if (!flag1 && entity1 == entity.ridingEntity) {
                            if (d2 == 0.0D) {
                                pointedEntity = entity1;
                                vec33 = movingobjectposition.hitVec;
                            }
                        } else {
                            pointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec33 != null && vec3.distanceTo(vec33) > (ViaLoadingBase.getInstance().getTargetVersion().getVersion() <= 47 ? reach : reach - 0.1f)) {
                pointedEntity = null;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null, new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < distance || mc.objectMouseOver == null)) {
                mc.objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);

                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
                    mc.pointedEntity = pointedEntity;
                }
            }

            mc.mcProfiler.endSection();
        }
    }

    private void updateFovModifierHand() {
        float f = 1.0F;

        if (mc.getRenderViewEntity() instanceof AbstractClientPlayer abstractclientplayer) {
            f = abstractclientplayer.getFovModifier();
        }

        fovModifierHandPrev = fovModifierHand;
        fovModifierHand += (f - fovModifierHand) * 0.5F;

        if (fovModifierHand > 1.5F) {
            fovModifierHand = 1.5F;
        }

        if (fovModifierHand < 0.1F) {
            fovModifierHand = 0.1F;
        }
    }

    private static float getFOVModifier(float partialTicks, boolean useFOVSetting) {
        if (debugView) {
            return 90.0F;
        } else {
            Entity entity = mc.getRenderViewEntity();
            float f = 70.0F;
            float f2;

            if (useFOVSetting) {
                f = mc.gameSettings.fovSetting;

                if (Config.isDynamicFov()) {
                    f *= fovModifierHandPrev + (fovModifierHand - fovModifierHandPrev) * partialTicks;
                }
            }

            f2 = useFOVSetting ? f : 70;

            boolean flag = false;

            if (mc.currentScreen == null) {
                flag = GameSettings.isKeyDown(mc.gameSettings.ofKeyBindZoom);
            }

            if (flag) {
                if (!Config.zoomMode) {
                    Config.zoomMode = true;
                    Config.zoomSmoothCamera = mc.gameSettings.smoothCamera;
                    mc.gameSettings.smoothCamera = true;
                    mc.renderGlobal.displayListEntitiesDirty = true;
                } else {
                    interpolatedZoom = MathUtils.interpolateNoUpdateCheck(interpolatedZoom, mc.gameSettings.fovSetting / 4f, 0.1f);
                    f = interpolatedZoom;
                }
            } else if (Config.zoomMode) {
                Config.zoomMode = false;
                mc.gameSettings.smoothCamera = Config.zoomSmoothCamera;
                mouseFilterXAxis = new MouseFilter();
                mouseFilterYAxis = new MouseFilter();
                mc.renderGlobal.displayListEntitiesDirty = true;
            }

            if (!flag) {
                if (useFOVSetting) {
                    interpolatedZoom = MathUtils.interpolateNoUpdateCheck(interpolatedZoom, f2, 0.1f);
                    f = interpolatedZoom;
                } else {
                    interpolatedZoom = MathUtils.interpolateNoUpdateCheck(interpolatedZoom, 70, 0.1f);
                }
            }

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0.0F) {
                float f1 = (float) ((EntityLivingBase) entity).deathTime + partialTicks;
                f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
            }

            Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entity, partialTicks);

            if (block.getMaterial() == Material.water) {
                f = f * 60.0F / 70.0F;
            }

            return Reflector.ForgeHooksClient_getFOVModifier.exists() ? Reflector.callFloat(Reflector.ForgeHooksClient_getFOVModifier, EntityRenderer.class, entity, block, partialTicks, f) : f;
        }
    }

    private static void hurtCameraEffect(float partialTicks) {
        if (mc.getRenderViewEntity() instanceof EntityLivingBase entitylivingbase) {
            if (Demise.INSTANCE.getModuleManager().getModule(NoHurtCam.class).isEnabled())
                return;

            float f = (float) entitylivingbase.hurtTime - partialTicks;

            if (entitylivingbase.getHealth() <= 0.0F) {
                float f1 = (float) entitylivingbase.deathTime + partialTicks;
                GlStateManager.rotate(40.0F - 8000.0F / (f1 + 200.0F), 0.0F, 0.0F, 1.0F);
            }

            if (f < 0.0F) {
                return;
            }

            f = f / (float) entitylivingbase.maxHurtTime;
            f = MathHelper.sin(f * f * f * f * (float) Math.PI);
            float f2 = entitylivingbase.attackedAtYaw;
            GlStateManager.rotate(-f2, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-f * 14.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f2, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void setupViewBobbing(float partialTicks) {
        if (mc.getRenderViewEntity() instanceof EntityPlayer entityplayer) {
            float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
            float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
            float f2 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
            float f3 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
            GlStateManager.translate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
            GlStateManager.rotate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);
        }
    }

    private void orientCamera(float partialTicks) {
        Entity entity = mc.getRenderViewEntity();
        float f = entity.getEyeHeight();
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;

        if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
            f = (float) ((double) f + 1.0D);
            GlStateManager.translate(0.0F, 0.3F, 0.0F);

            if (!mc.gameSettings.debugCamEnable) {
                BlockPos blockpos = new BlockPos(entity);
                IBlockState iblockstate = mc.theWorld.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if (Reflector.ForgeHooksClient_orientBedCamera.exists()) {
                    Reflector.callVoid(Reflector.ForgeHooksClient_orientBedCamera, mc.theWorld, blockpos, iblockstate, entity);
                } else if (block == Blocks.bed) {
                    int j = iblockstate.getValue(BlockBed.FACING).getHorizontalIndex();
                    GlStateManager.rotate((float) (j * 90), 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
            }
        } else if (mc.gameSettings.thirdPersonView > 0) {
            ThirdPersonDistance thirdPersonDistance = Demise.INSTANCE.getModuleManager().getModule(ThirdPersonDistance.class);

            double d3 = thirdPersonDistance.isEnabled() ? thirdPersonDistance.cameraDistance.get() : 4.0;

            if (mc.gameSettings.debugCamEnable) {
                GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
            } else {
                float f1 = entity.rotationYaw;
                float f2 = entity.rotationPitch;

                if (mc.gameSettings.thirdPersonView == 2) {
                    f2 += 180.0F;
                }

                double d4 = (double) (-MathHelper.sin(f1 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI)) * d3;
                double d5 = (double) (MathHelper.cos(f1 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI)) * d3;
                double d6 = (double) (-MathHelper.sin(f2 / 180.0F * (float) Math.PI)) * d3;

                for (int i = 0; i < 8; ++i) {
                    float f3 = (float) ((i & 1) * 2 - 1);
                    float f4 = (float) ((i >> 1 & 1) * 2 - 1);
                    float f5 = (float) ((i >> 2 & 1) * 2 - 1);
                    f3 = f3 * 0.1F;
                    f4 = f4 * 0.1F;
                    f5 = f5 * 0.1F;
                    MovingObjectPosition movingobjectposition = mc.theWorld.rayTraceBlocks(new Vec3(d0 + (double) f3, d1 + (double) f4, d2 + (double) f5), new Vec3(d0 - d4 + (double) f3 + (double) f5, d1 - d6 + (double) f4, d2 - d5 + (double) f5));

                    if (movingobjectposition != null) {
                        double d7 = movingobjectposition.hitVec.distanceTo(new Vec3(d0, d1, d2));

                        if (d7 < d3) {
                            d3 = d7;
                        }
                    }
                }

                if (mc.gameSettings.thirdPersonView == 2) {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.0F, (float) (-d3));
                GlStateManager.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-entity.rotationYaw, 0.0F, 1.0F, 0.0F);
            }
        } else {
            GlStateManager.translate(0.0F, 0.0F, -0.1F);
        }

        if (Reflector.EntityViewRenderEvent_CameraSetup_Constructor.exists()) {
            if (!mc.gameSettings.debugCamEnable) {
                float f6 = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F;
                float f7 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                float f8 = 0.0F;

                if (entity instanceof EntityAnimal entityanimal1) {
                    f6 = entityanimal1.prevRotationYawHead + (entityanimal1.rotationYawHead - entityanimal1.prevRotationYawHead) * partialTicks + 180.0F;
                }

                Block block1 = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entity, partialTicks);
                Object object = Reflector.newInstance(Reflector.EntityViewRenderEvent_CameraSetup_Constructor, this, entity, block1, partialTicks, f6, f7, f8);
                Reflector.postForgeBusEvent(object);
                f8 = Reflector.getFieldValueFloat(object, Reflector.EntityViewRenderEvent_CameraSetup_roll, f8);
                f7 = Reflector.getFieldValueFloat(object, Reflector.EntityViewRenderEvent_CameraSetup_pitch, f7);
                f6 = Reflector.getFieldValueFloat(object, Reflector.EntityViewRenderEvent_CameraSetup_yaw, f6);
                GlStateManager.rotate(f8, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(f7, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(f6, 0.0F, 1.0F, 0.0F);
            }
        } else if (!mc.gameSettings.debugCamEnable) {
            GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);

            if (entity instanceof EntityAnimal entityanimal1) {
                GlStateManager.rotate(entityanimal1.prevRotationYawHead + (entityanimal1.rotationYawHead - entityanimal1.prevRotationYawHead) * partialTicks + 180.0F, 0.0F, 1.0F, 0.0F);
            } else {
                GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, 1.0F, 0.0F);
            }
        }

        GlStateManager.translate(0.0F, -f, 0.0F);
        d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
        d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
        d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;
        cloudFog = mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
    }

    public void setupCameraTransform(float partialTicks, int pass) {
        farPlaneDistance = (float) (mc.gameSettings.renderDistanceChunks * 16);

        if (Config.isFogFancy()) {
            farPlaneDistance *= 0.95F;
        }

        if (Config.isFogFast()) {
            farPlaneDistance *= 0.83F;
        }

        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        float f = 0.07F;

        if (mc.gameSettings.anaglyph) {
            GlStateManager.translate((float) (-(pass * 2 - 1)) * f, 0.0F, 0.0F);
        }

        clipDistance = farPlaneDistance * 2.0F;

        if (clipDistance < 173.0F) {
            clipDistance = 173.0F;
        }

        float aspect = (float) mc.displayWidth / (float) mc.displayHeight;
        Project.gluPerspective(getFOVModifier(partialTicks, true), aspect, 0.05F, clipDistance);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();

        if (mc.gameSettings.anaglyph) {
            GlStateManager.translate((float) (pass * 2 - 1) * 0.1F, 0.0F, 0.0F);
        }

        hurtCameraEffect(partialTicks);

        ViewBobbingEvent viewBobbingEvent = new ViewBobbingEvent(ViewBobbingEvent.State.CameraTransform);
        if (mc.gameSettings.viewBobbing && !viewBobbingEvent.isCancelled()) {
            setupViewBobbing(partialTicks);
        }

        float f1 = mc.thePlayer.prevTimeInPortal + (mc.thePlayer.timeInPortal - mc.thePlayer.prevTimeInPortal) * partialTicks;

        if (f1 > 0.0F) {
            int i = 20;

            if (mc.thePlayer.isPotionActive(Potion.confusion)) {
                i = 7;
            }

            float f2 = 5.0F / (f1 * f1 + 5.0F) - f1 * 0.04F;
            f2 = f2 * f2;
            GlStateManager.rotate(((float) rendererUpdateCount + partialTicks) * (float) i, 0.0F, 1.0F, 1.0F);
            GlStateManager.scale(1.0F / f2, 1.0F, 1.0F);
            GlStateManager.rotate(-((float) rendererUpdateCount + partialTicks) * (float) i, 0.0F, 1.0F, 1.0F);
        }

        orientCamera(partialTicks);

        if (debugView) {
            GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        }
    }

    public static void renderHand(float partialTicks, int xOffset) {
        renderHand(partialTicks, xOffset, true, true, false);
    }

    public static void renderHand(float p_renderHand_1_, int p_renderHand_2_, boolean p_renderHand_3_, boolean p_renderHand_4_, boolean p_renderHand_5_) {
        if (!debugView) {
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            float f = 0.07F;

            if (mc.gameSettings.anaglyph) {
                GlStateManager.translate((float) (-(p_renderHand_2_ * 2 - 1)) * f, 0.0F, 0.0F);
            }

            if (Config.isShaders()) {
                Shaders.applyHandDepth();
            }

            float aspect = (float) mc.displayWidth / (float) mc.displayHeight;
            Project.gluPerspective(getFOVModifier(p_renderHand_1_, false), aspect, 0.05F, farPlaneDistance * 2.0F);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();

            if (mc.gameSettings.anaglyph) {
                GlStateManager.translate((float) (p_renderHand_2_ * 2 - 1) * 0.1F, 0.0F, 0.0F);
            }

            boolean flag = false;

            if (p_renderHand_3_) {
                GlStateManager.pushMatrix();
                hurtCameraEffect(p_renderHand_1_);

                ViewBobbingEvent viewBobbingEvent = new ViewBobbingEvent(ViewBobbingEvent.State.Hand1);
                if (mc.gameSettings.viewBobbing && !viewBobbingEvent.isCancelled()) {
                    setupViewBobbing(p_renderHand_1_);
                }

                flag = mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase) mc.getRenderViewEntity()).isPlayerSleeping();
                boolean flag1 = !ReflectorForge.renderFirstPersonHand(mc.renderGlobal, p_renderHand_1_, p_renderHand_2_);

                if (flag1 && mc.gameSettings.thirdPersonView == 0 && !flag && !mc.gameSettings.hideGUI && !mc.playerController.isSpectator()) {
                    enableLightmap();

                    if (Config.isShaders()) {
                        ShadersRender.renderItemFP(itemRenderer, p_renderHand_1_, p_renderHand_5_);
                    } else {
                        ItemRenderer.renderItemInFirstPerson(p_renderHand_1_);
                    }

                    disableLightmap();
                }

                GlStateManager.popMatrix();
            }

            if (!p_renderHand_4_) {
                return;
            }

            disableLightmap();

            if (mc.gameSettings.thirdPersonView == 0 && !flag) {
                itemRenderer.renderOverlays(p_renderHand_1_);
                hurtCameraEffect(p_renderHand_1_);
            }

            final ViewBobbingEvent viewBobbingEvent = new ViewBobbingEvent(ViewBobbingEvent.State.Hand2);
            if (mc.gameSettings.viewBobbing && !viewBobbingEvent.isCancelled()) {
                setupViewBobbing(p_renderHand_1_);
            }
        }
    }

    public static void disableLightmap() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        if (Config.isShaders()) {
            Shaders.disableLightmap();
        }
    }

    public static void enableLightmap() {
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        float f = 0.00390625F;
        GlStateManager.scale(f, f, f);
        GlStateManager.translate(8.0F, 8.0F, 8.0F);
        GlStateManager.matrixMode(5888);
        mc.getTextureManager().bindTexture(locationLightMap);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        if (Config.isShaders()) {
            Shaders.enableLightmap();
        }
    }

    private void updateTorchFlicker() {
        torchFlickerDX = (float) ((double) torchFlickerDX + (Math.random() - Math.random()) * Math.random() * Math.random());
        torchFlickerDX = (float) ((double) torchFlickerDX * 0.9D);
        torchFlickerX += (torchFlickerDX - torchFlickerX);
        lightmapUpdateNeeded = true;
    }

    private void updateLightmap(float partialTicks) {
        if (lightmapUpdateNeeded) {
            mc.mcProfiler.startSection("lightTex");
            World world = mc.theWorld;

            if (world != null) {
                if (Config.isCustomColors() && CustomColors.updateLightmap(world, torchFlickerX, lightmapColors, mc.thePlayer.isPotionActive(Potion.nightVision), partialTicks)) {
                    lightmapTexture.updateDynamicTexture();
                    lightmapUpdateNeeded = false;
                    mc.mcProfiler.endSection();
                    return;
                }

                float f = world.getSunBrightness(1.0F);
                float f1 = f * 0.95F + 0.05F;

                for (int i = 0; i < 256; ++i) {
                    float f2 = world.provider.getLightBrightnessTable()[i / 16] * f1;
                    float f3 = world.provider.getLightBrightnessTable()[i % 16] * (torchFlickerX * 0.1F + 1.5F);

                    if (world.getLastLightningBolt() > 0) {
                        f2 = world.provider.getLightBrightnessTable()[i / 16];
                    }

                    float f4 = f2 * (f * 0.65F + 0.35F);
                    float f5 = f2 * (f * 0.65F + 0.35F);
                    float f6 = f3 * ((f3 * 0.6F + 0.4F) * 0.6F + 0.4F);
                    float f7 = f3 * (f3 * f3 * 0.6F + 0.4F);
                    float f8 = f4 + f3;
                    float f9 = f5 + f6;
                    float f10 = f2 + f7;
                    f8 = f8 * 0.96F + 0.03F;
                    f9 = f9 * 0.96F + 0.03F;
                    f10 = f10 * 0.96F + 0.03F;

                    if (bossColorModifier > 0.0F) {
                        float f11 = bossColorModifierPrev + (bossColorModifier - bossColorModifierPrev) * partialTicks;
                        f8 = f8 * (1.0F - f11) + f8 * 0.7F * f11;
                        f9 = f9 * (1.0F - f11) + f9 * 0.6F * f11;
                        f10 = f10 * (1.0F - f11) + f10 * 0.6F * f11;
                    }

                    if (world.provider.getDimensionId() == 1) {
                        f8 = 0.22F + f3 * 0.75F;
                        f9 = 0.28F + f6 * 0.75F;
                        f10 = 0.25F + f7 * 0.75F;
                    }

                    if (mc.thePlayer.isPotionActive(Potion.nightVision)) {
                        float f15 = getNightVisionBrightness(mc.thePlayer, partialTicks);
                        float f12 = 1.0F / f8;

                        if (f12 > 1.0F / f9) {
                            f12 = 1.0F / f9;
                        }

                        if (f12 > 1.0F / f10) {
                            f12 = 1.0F / f10;
                        }

                        f8 = f8 * (1.0F - f15) + f8 * f12 * f15;
                        f9 = f9 * (1.0F - f15) + f9 * f12 * f15;
                        f10 = f10 * (1.0F - f15) + f10 * f12 * f15;
                    }

                    if (f8 > 1.0F) {
                        f8 = 1.0F;
                    }

                    if (f9 > 1.0F) {
                        f9 = 1.0F;
                    }

                    if (f10 > 1.0F) {
                        f10 = 1.0F;
                    }

                    float f16 = mc.gameSettings.gammaSetting;
                    float f17 = 1.0F - f8;
                    float f13 = 1.0F - f9;
                    float f14 = 1.0F - f10;
                    f17 = 1.0F - f17 * f17 * f17 * f17;
                    f13 = 1.0F - f13 * f13 * f13 * f13;
                    f14 = 1.0F - f14 * f14 * f14 * f14;
                    f8 = f8 * (1.0F - f16) + f17 * f16;
                    f9 = f9 * (1.0F - f16) + f13 * f16;
                    f10 = f10 * (1.0F - f16) + f14 * f16;
                    f8 = f8 * 0.96F + 0.03F;
                    f9 = f9 * 0.96F + 0.03F;
                    f10 = f10 * 0.96F + 0.03F;

                    if (f8 > 1.0F) {
                        f8 = 1.0F;
                    }

                    if (f9 > 1.0F) {
                        f9 = 1.0F;
                    }

                    if (f10 > 1.0F) {
                        f10 = 1.0F;
                    }

                    if (f8 < 0.0F) {
                        f8 = 0.0F;
                    }

                    if (f9 < 0.0F) {
                        f9 = 0.0F;
                    }

                    if (f10 < 0.0F) {
                        f10 = 0.0F;
                    }

                    int j = 255;
                    int k = (int) (f8 * 255.0F);
                    int l = (int) (f9 * 255.0F);
                    int i1 = (int) (f10 * 255.0F);
                    lightmapColors[i] = j << 24 | k << 16 | l << 8 | i1;
                }

                lightmapTexture.updateDynamicTexture();
                lightmapUpdateNeeded = false;
                mc.mcProfiler.endSection();
            }
        }
    }

    public float getNightVisionBrightness(EntityLivingBase entitylivingbaseIn, float partialTicks) {
        int i = entitylivingbaseIn.getActivePotionEffect(Potion.nightVision).getDuration();
        return i > 200 ? 1.0F : 0.7F + MathHelper.sin(((float) i - partialTicks) * (float) Math.PI * 0.2F) * 0.3F;
    }

    public void updateCameraAndRender(float partialTicks, long nanoTime) {
        Config.renderPartialTicks = partialTicks;
        frameInit();
        boolean flag = Display.isActive();

        if (!flag && mc.gameSettings.pauseOnLostFocus && (!mc.gameSettings.touchscreen || !Mouse.isButtonDown(1))) {
            if (Minecraft.getSystemTime() - prevFrameTime > 500L) {
                mc.displayInGameMenu();
            }
        } else {
            prevFrameTime = Minecraft.getSystemTime();
        }

        mc.mcProfiler.startSection("mouse");

        if (flag && Minecraft.isRunningOnMac && mc.inGameHasFocus && !Mouse.isInsideWindow()) {
            Mouse.setGrabbed(false);
            Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
            Mouse.setGrabbed(true);
        }

        Demise.INSTANCE.getEventManager().call(new MouseMoveEvent(mc.mouseHelper.deltaX, mc.mouseHelper.deltaY, MouseMoveEvent.State.PRE));

        if (mc.inGameHasFocus && flag) {
            mc.mouseHelper.mouseXYChange();
            float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;

            MouseMoveEvent mouseMoveEvent = new MouseMoveEvent(mc.mouseHelper.deltaX, mc.mouseHelper.deltaY, MouseMoveEvent.State.UPDATE);
            Demise.INSTANCE.getEventManager().call(mouseMoveEvent);

            float f2 = (float) mouseMoveEvent.getDeltaX() * f1;
            float f3 = (float) mouseMoveEvent.getDeltaY() * f1;
            int i = 1;

            if (mc.gameSettings.invertMouse) {
                i = -1;
            }

            if (mc.gameSettings.smoothCamera) {
                smoothCamYaw += f2;
                smoothCamPitch += f3;
                float f4 = partialTicks - smoothCamPartialTicks;
                smoothCamPartialTicks = partialTicks;
                f2 = smoothCamFilterX * f4;
                f3 = smoothCamFilterY * f4;
                mc.thePlayer.setAngles(f2, f3 * (float) i);
            } else {
                smoothCamYaw = 0.0F;
                smoothCamPitch = 0.0F;
                mc.thePlayer.setAngles(f2, f3 * (float) i);
            }
        }

        mc.mcProfiler.endSection();

        if (!mc.skipRenderWorld) {
            anaglyphEnable = mc.gameSettings.anaglyph;
            final ScaledResolution scaledresolution = new ScaledResolution(mc);
            int i1 = scaledresolution.getScaledWidth();
            int j1 = scaledresolution.getScaledHeight();
            final int k1 = Mouse.getX() * i1 / mc.displayWidth;
            final int l1 = j1 - Mouse.getY() * j1 / mc.displayHeight - 1;
            int i2 = mc.gameSettings.limitFramerate;

            if (mc.theWorld != null) {
                mc.mcProfiler.startSection("level");
                int j = Math.min(Minecraft.getDebugFPS(), i2);
                j = Math.max(j, 60);
                long k = System.nanoTime() - nanoTime;
                long l = Math.max((long) (1000000000 / j / 4) - k, 0L);
                renderWorld(partialTicks, System.nanoTime() + l);

                if (OpenGlHelper.shadersSupported) {
                    mc.renderGlobal.renderEntityOutlineFramebuffer();

                    if (theShaderGroup != null && useShader) {
                        GlStateManager.matrixMode(5890);
                        GlStateManager.pushMatrix();
                        GlStateManager.loadIdentity();
                        theShaderGroup.loadShaderGroup(partialTicks);
                        GlStateManager.popMatrix();
                    }

                    mc.getFramebuffer().bindFramebuffer(true);
                }

                renderEndNanoTime = System.nanoTime();
                mc.mcProfiler.endStartSection("gui");

                if (!mc.gameSettings.hideGUI || mc.currentScreen != null) {
                    GlStateManager.alphaFunc(516, 0.1F);
                    mc.ingameGUI.renderGameOverlay(partialTicks);

                    if (mc.gameSettings.ofShowFps && !mc.gameSettings.showDebugInfo) {
                        Config.drawFps();
                    }

                    if (mc.gameSettings.showDebugInfo) {
                        Lagometer.showLagometer(scaledresolution);
                    }
                }

                mc.mcProfiler.endSection();
            } else {
                GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                GlStateManager.matrixMode(5888);
                GlStateManager.loadIdentity();
                setupOverlayRendering();
                renderEndNanoTime = System.nanoTime();
                TileEntityRendererDispatcher.instance.renderEngine = mc.getTextureManager();
                TileEntityRendererDispatcher.instance.fontRenderer = mc.fontRendererObj;
            }

            if (mc.currentScreen != null) {
                GlStateManager.clear(256);

                try {
                    if (Reflector.ForgeHooksClient_drawScreen.exists()) {
                        Reflector.callVoid(Reflector.ForgeHooksClient_drawScreen, mc.currentScreen, k1, l1, partialTicks);
                    } else {
                        mc.currentScreen.drawScreen(k1, l1, partialTicks);
                    }
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Rendering screen");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Screen render details");
                    crashreportcategory.addCrashSectionCallable("Screen name", () -> EntityRenderer.mc.currentScreen.getClass().getCanonicalName());
                    crashreportcategory.addCrashSectionCallable("Mouse location", () -> String.format("Scaled: (%d, %d). Absolute: (%d, %d)", k1, l1, Mouse.getX(), Mouse.getY()));
                    crashreportcategory.addCrashSectionCallable("Screen size", () -> String.format("Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d", scaledresolution.getScaledWidth(), scaledresolution.getScaledHeight(), EntityRenderer.mc.displayWidth, EntityRenderer.mc.displayHeight, scaledresolution.getScaleFactor()));
                    throw new ReportedException(crashreport);
                }
            }
        }

        frameFinish();
        waitForServerThread();
        MemoryMonitor.update();
        Lagometer.updateLagometer();

        if (mc.gameSettings.ofProfiler) {
            mc.gameSettings.showDebugProfilerChart = true;
        }
    }

    private boolean isDrawBlockOutline() {
        if (!drawBlockOutline) {
            return false;
        } else {
            Entity entity = mc.getRenderViewEntity();
            boolean flag = entity instanceof EntityPlayer && !mc.gameSettings.hideGUI;

            if (flag && !((EntityPlayer) entity).capabilities.allowEdit) {
                ItemStack itemstack = ((EntityPlayer) entity).getCurrentEquippedItem();

                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    BlockPos blockpos = mc.objectMouseOver.getBlockPos();
                    IBlockState iblockstate = mc.theWorld.getBlockState(blockpos);
                    Block block = iblockstate.getBlock();

                    if (mc.playerController.getCurrentGameType() == WorldSettings.GameType.SPECTATOR) {
                        flag = ReflectorForge.blockHasTileEntity(iblockstate) && mc.theWorld.getTileEntity(blockpos) instanceof IInventory;
                    } else {
                        flag = itemstack != null && (itemstack.canDestroy(block) || itemstack.canPlaceOn(block));
                    }
                }
            }

            return flag;
        }
    }

    private void renderWorldDirections(float partialTicks) {
        if (mc.gameSettings.showDebugInfo && !mc.gameSettings.hideGUI && !mc.thePlayer.hasReducedDebug() && !mc.gameSettings.reducedDebugInfo) {
            Entity entity = mc.getRenderViewEntity();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GL11.glLineWidth(1.0F);
            GlStateManager.disableTexture2D();
            GlStateManager.depthMask(false);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            orientCamera(partialTicks);
            GlStateManager.translate(0.0F, entity.getEyeHeight(), 0.0F);
            RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.005D, 1.0E-4D, 1.0E-4D), 255, 0, 0, 255);
            RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0E-4D, 1.0E-4D, 0.005D), 0, 0, 255, 255);
            RenderGlobal.drawOutlinedBoundingBox(new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0E-4D, 0.0033D, 1.0E-4D), 0, 255, 0, 255);
            GlStateManager.popMatrix();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
        updateLightmap(partialTicks);

        if (mc.getRenderViewEntity() == null) {
            mc.setRenderViewEntity(mc.thePlayer);
        }

        getMouseOver(partialTicks);

        if (Config.isShaders()) {
            Shaders.beginRender(mc, partialTicks, finishTimeNano);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        mc.mcProfiler.startSection("center");

        if (mc.gameSettings.anaglyph) {
            anaglyphField = 0;
            GlStateManager.colorMask(false, true, true, false);
            renderWorldPass(0, partialTicks, finishTimeNano);
            anaglyphField = 1;
            GlStateManager.colorMask(true, false, false, false);
            renderWorldPass(1, partialTicks, finishTimeNano);
            GlStateManager.colorMask(true, true, true, false);
        } else {
            /*
            handFramebuffer = RenderUtils.createFrameBuffer(handFramebuffer, true);
            handFramebuffer.framebufferClear();
            handFramebuffer.bindFramebuffer(true);
            RenderUtils.resetColor();

            renderWorldPass(2, partialTicks, finishTimeNano);

            handFramebuffer.unbindFramebuffer();
            RenderUtils.resetColor();
            Shadow.renderBloom(handFramebuffer.framebufferTexture, 50, 1);
             */

            renderWorldPass(2, partialTicks, finishTimeNano);
        }
        mc.mcProfiler.endSection();
    }

    private void renderWorldPass(int pass, float partialTicks, long finishTimeNano) {
        boolean flag = Config.isShaders();

        if (flag) {
            Shaders.beginRenderPass(pass, partialTicks, finishTimeNano);
        }

        RenderGlobal renderglobal = mc.renderGlobal;
        EffectRenderer effectrenderer = mc.effectRenderer;
        boolean flag1 = isDrawBlockOutline();
        GlStateManager.enableCull();
        mc.mcProfiler.endStartSection("clear");

        if (flag) {
            Shaders.setViewport(0, 0, mc.displayWidth, mc.displayHeight);
        } else {
            GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
        }

        updateFogColor(partialTicks);
        GlStateManager.clear(16640);

        if (flag) {
            Shaders.clearRenderBuffer();
        }

        mc.mcProfiler.endStartSection("camera");
        setupCameraTransform(partialTicks, pass);

        if (flag) {
            Shaders.setCamera(partialTicks);
        }

        ActiveRenderInfo.updateRenderInfo(mc.thePlayer, mc.gameSettings.thirdPersonView == 2);
        mc.mcProfiler.endStartSection("frustum");
        ClippingHelper clippinghelper = ClippingHelperImpl.getInstance();
        mc.mcProfiler.endStartSection("culling");
        clippinghelper.disabled = Config.isShaders() && !Shaders.isFrustumCulling();
        ICamera icamera = new Frustum(clippinghelper);
        Entity entity = mc.getRenderViewEntity();
        double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
        double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
        double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;

        if (flag) {
            ShadersRender.setFrustrumPosition(icamera, d0, d1, d2);
        } else {
            icamera.setPosition(d0, d1, d2);
        }

        if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled() || Config.isCustomSky()) && !Shaders.isShadowPass) {
            setupFog(-1, partialTicks);

            mc.mcProfiler.endStartSection("sky");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            float aspect = (float) mc.displayWidth / (float) mc.displayHeight;
            Project.gluPerspective(getFOVModifier(partialTicks, true), aspect, 0.05F, clipDistance);
            GlStateManager.matrixMode(5888);

            if (flag) {
                Shaders.beginSky();
            }

            renderglobal.renderSky(partialTicks, pass);

            if (flag) {
                Shaders.endSky();
            }

            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(getFOVModifier(partialTicks, true), aspect, 0.05F, clipDistance);
            GlStateManager.matrixMode(5888);
        } else {
            GlStateManager.disableBlend();
        }

        setupFog(0, partialTicks);
        GlStateManager.shadeModel(7425);

        if (entity.posY + (double) entity.getEyeHeight() < 128.0D + (double) (mc.gameSettings.ofCloudsHeight * 128.0F)) {
            renderCloudsCheck(renderglobal, partialTicks, pass);
        }

        mc.mcProfiler.endStartSection("prepareterrain");
        setupFog(0, partialTicks);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        RenderHelper.disableStandardItemLighting();
        mc.mcProfiler.endStartSection("terrain_setup");
        checkLoadVisibleChunks(entity, partialTicks, icamera, mc.thePlayer.isSpectator());

        if (flag) {
            ShadersRender.setupTerrain(renderglobal, entity, partialTicks, icamera, frameCount++, mc.thePlayer.isSpectator());
        } else {
            renderglobal.setupTerrain(entity, partialTicks, icamera, frameCount++, mc.thePlayer.isSpectator());
        }

        if (pass == 0 || pass == 2) {
            mc.mcProfiler.endStartSection("updatechunks");
            Lagometer.timerChunkUpload.start();
            mc.renderGlobal.updateChunks(finishTimeNano);
            Lagometer.timerChunkUpload.end();
        }

        mc.mcProfiler.endStartSection("terrain");
        Lagometer.timerTerrain.start();

        if (mc.gameSettings.ofSmoothFps && pass > 0) {
            mc.mcProfiler.endStartSection("finish");
            GL11.glFinish();
            mc.mcProfiler.endStartSection("terrain");
        }

        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlpha();

        if (flag) {
            ShadersRender.beginTerrainSolid();
        }

        renderglobal.renderBlockLayer(EnumWorldBlockLayer.SOLID, partialTicks, pass, entity);
        GlStateManager.enableAlpha();

        if (flag) {
            ShadersRender.beginTerrainCutoutMipped();
        }

        mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, mc.gameSettings.mipmapLevels > 0);
        renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT_MIPPED, partialTicks, pass, entity);
        mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
        mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);

        if (flag) {
            ShadersRender.beginTerrainCutout();
        }

        renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT, partialTicks, pass, entity);
        mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();

        if (flag) {
            ShadersRender.endTerrain();
        }

        Lagometer.timerTerrain.end();
        GlStateManager.shadeModel(7424);
        GlStateManager.alphaFunc(516, 0.1F);

        if (!debugView) {
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            RenderHelper.enableStandardItemLighting();
            mc.mcProfiler.endStartSection("entities");

            if (Reflector.ForgeHooksClient_setRenderPass.exists()) {
                Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, 0);
            }

            renderglobal.renderEntities(entity, icamera, partialTicks);

            if (Reflector.ForgeHooksClient_setRenderPass.exists()) {
                Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, -1);
            }

            RenderHelper.disableStandardItemLighting();
            disableLightmap();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();

            if (mc.objectMouseOver != null && entity.isInsideOfMaterial(Material.water) && flag1) {
                EntityPlayer entityplayer = (EntityPlayer) entity;
                GlStateManager.disableAlpha();
                mc.mcProfiler.endStartSection("outline");
                renderglobal.drawSelectionBox(entityplayer, mc.objectMouseOver, 0, partialTicks);
                GlStateManager.enableAlpha();
            }
        }

        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();

        if (flag1 && mc.objectMouseOver != null && !entity.isInsideOfMaterial(Material.water)) {
            EntityPlayer entityplayer1 = (EntityPlayer) entity;
            GlStateManager.disableAlpha();
            mc.mcProfiler.endStartSection("outline");

            if ((!Reflector.ForgeHooksClient_onDrawBlockHighlight.exists() || !Reflector.callBoolean(Reflector.ForgeHooksClient_onDrawBlockHighlight, renderglobal, entityplayer1, mc.objectMouseOver, 0, entityplayer1.getHeldItem(), partialTicks)) && !mc.gameSettings.hideGUI) {
                renderglobal.drawSelectionBox(entityplayer1, mc.objectMouseOver, 0, partialTicks);
            }
            GlStateManager.enableAlpha();
        }

        if (!renderglobal.damagedBlocks.isEmpty()) {
            mc.mcProfiler.endStartSection("destroyProgress");
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
            mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
            renderglobal.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getWorldRenderer(), entity, partialTicks);
            mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
            GlStateManager.disableBlend();
        }

        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableBlend();

        if (!debugView) {
            enableLightmap();
            mc.mcProfiler.endStartSection("litParticles");

            if (flag) {
                Shaders.beginLitParticles();
            }

            effectrenderer.renderLitParticles(entity, partialTicks);
            RenderHelper.disableStandardItemLighting();
            setupFog(0, partialTicks);
            mc.mcProfiler.endStartSection("particles");

            if (flag) {
                Shaders.beginParticles();
            }

            effectrenderer.renderParticles(entity, partialTicks);

            if (flag) {
                Shaders.endParticles();
            }

            disableLightmap();
        }

        GlStateManager.depthMask(false);

        if (Config.isShaders()) {
            GlStateManager.depthMask(Shaders.isRainDepth());
        }

        GlStateManager.enableCull();
        mc.mcProfiler.endStartSection("weather");

        if (flag) {
            Shaders.beginWeather();
        }

        renderRainSnow(partialTicks);

        if (flag) {
            Shaders.endWeather();
        }

        GlStateManager.depthMask(true);
        renderglobal.renderWorldBorder(entity, partialTicks);

        if (flag) {
            ShadersRender.renderHand0(this, partialTicks, pass);
            Shaders.preWater();
        }

        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.alphaFunc(516, 0.1F);
        setupFog(0, partialTicks);
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GlStateManager.shadeModel(7425);
        mc.mcProfiler.endStartSection("translucent");

        if (flag) {
            Shaders.beginWater();
        }

        renderglobal.renderBlockLayer(EnumWorldBlockLayer.TRANSLUCENT, partialTicks, pass, entity);

        if (flag) {
            Shaders.endWater();
        }

        if (Reflector.ForgeHooksClient_setRenderPass.exists() && !debugView) {
            RenderHelper.enableStandardItemLighting();
            mc.mcProfiler.endStartSection("entities");
            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, 1);
            mc.renderGlobal.renderEntities(entity, icamera, partialTicks);
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            Reflector.callVoid(Reflector.ForgeHooksClient_setRenderPass, -1);
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();

        if (entity.posY + (double) entity.getEyeHeight() >= 128.0D + (double) (mc.gameSettings.ofCloudsHeight * 128.0F)) {
            mc.mcProfiler.endStartSection("aboveClouds");
            renderCloudsCheck(renderglobal, partialTicks, pass);
        }

        if (Reflector.ForgeHooksClient_dispatchRenderLast.exists()) {
            mc.mcProfiler.endStartSection("forge_render_last");
            Reflector.callVoid(Reflector.ForgeHooksClient_dispatchRenderLast, renderglobal, partialTicks);
        }

        mc.mcProfiler.endStartSection("hand");

        Demise.INSTANCE.getEventManager().call(new Render3DEvent(partialTicks, new ScaledResolution(mc)));

        if (renderHand && !Shaders.isShadowPass) {
            if (flag) {
                ShadersRender.renderHand1(this, partialTicks, pass);
                Shaders.renderCompositeFinal();
            }

            GlStateManager.clear(256);

            if (flag) {
                ShadersRender.renderFPOverlay(this, partialTicks, pass);
            } else {
                renderHand(partialTicks, pass);
            }

            renderWorldDirections(partialTicks);
        }

        if (flag) {
            Shaders.endRender();
        }
    }

    private void renderCloudsCheck(RenderGlobal renderGlobalIn, float partialTicks, int pass) {
        if (mc.gameSettings.renderDistanceChunks >= 4 && !Config.isCloudsOff() && Shaders.shouldRenderClouds(mc.gameSettings)) {

            float aspect = (float) mc.displayWidth / (float) mc.displayHeight;
            mc.mcProfiler.endStartSection("clouds");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(getFOVModifier(partialTicks, true), aspect, 0.05F, clipDistance * 4.0F);
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            setupFog(0, partialTicks);
            renderGlobalIn.renderClouds(partialTicks, pass);
            GlStateManager.disableFog();
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(getFOVModifier(partialTicks, true), aspect, 0.05F, clipDistance);
            GlStateManager.matrixMode(5888);
        }
    }

    private void addRainParticles() {
        float f = mc.theWorld.getRainStrength(1.0F);

        if (!Config.isRainFancy()) {
            f /= 2.0F;
        }

        if (f != 0.0F && Config.isRainSplash()) {
            random.setSeed((long) rendererUpdateCount * 312987231L);
            Entity entity = mc.getRenderViewEntity();
            World world = mc.theWorld;
            BlockPos blockpos = new BlockPos(entity);
            int i = 10;
            double d0 = 0.0D;
            double d1 = 0.0D;
            double d2 = 0.0D;
            int j = 0;
            int k = (int) (100.0F * f * f);

            if (mc.gameSettings.particleSetting == 1) {
                k >>= 1;
            } else if (mc.gameSettings.particleSetting == 2) {
                k = 0;
            }

            for (int l = 0; l < k; ++l) {
                BlockPos blockpos1 = world.getPrecipitationHeight(blockpos.add(random.nextInt(i) - random.nextInt(i), 0, random.nextInt(i) - random.nextInt(i)));
                BiomeGenBase biomegenbase = world.getBiomeGenForCoords(blockpos1);
                BlockPos blockpos2 = blockpos1.down();
                Block block = world.getBlockState(blockpos2).getBlock();

                if (blockpos1.getY() <= blockpos.getY() + i && blockpos1.getY() >= blockpos.getY() - i && biomegenbase.canRain() && biomegenbase.getFloatTemperature(blockpos1) >= 0.15F) {
                    double d3 = random.nextDouble();
                    double d4 = random.nextDouble();

                    if (block.getMaterial() == Material.lava) {
                        mc.theWorld.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (double) blockpos1.getX() + d3, (double) ((float) blockpos1.getY() + 0.1F) - block.getBlockBoundsMinY(), (double) blockpos1.getZ() + d4, 0.0D, 0.0D, 0.0D);
                    } else if (block.getMaterial() != Material.air) {
                        block.setBlockBoundsBasedOnState(world, blockpos2);
                        ++j;

                        if (random.nextInt(j) == 0) {
                            d0 = (double) blockpos2.getX() + d3;
                            d1 = (double) ((float) blockpos2.getY() + 0.1F) + block.getBlockBoundsMaxY() - 1.0D;
                            d2 = (double) blockpos2.getZ() + d4;
                        }

                        mc.theWorld.spawnParticle(EnumParticleTypes.WATER_DROP, (double) blockpos2.getX() + d3, (double) ((float) blockpos2.getY() + 0.1F) + block.getBlockBoundsMaxY(), (double) blockpos2.getZ() + d4, 0.0D, 0.0D, 0.0D);
                    }
                }
            }

            if (j > 0 && random.nextInt(3) < rainSoundCounter++) {
                rainSoundCounter = 0;

                if (d1 > (double) (blockpos.getY() + 1) && world.getPrecipitationHeight(blockpos).getY() > MathHelper.floor_float((float) blockpos.getY())) {
                    mc.theWorld.playSound(d0, d1, d2, "ambient.weather.rain", 0.1F, 0.5F, false);
                } else {
                    mc.theWorld.playSound(d0, d1, d2, "ambient.weather.rain", 0.2F, 1.0F, false);
                }
            }
        }
    }

    protected void renderRainSnow(float partialTicks) {
        if (Reflector.ForgeWorldProvider_getWeatherRenderer.exists()) {
            WorldProvider worldprovider = mc.theWorld.provider;
            Object object = Reflector.call(worldprovider, Reflector.ForgeWorldProvider_getWeatherRenderer);

            if (object != null) {
                Reflector.callVoid(object, Reflector.IRenderHandler_render, partialTicks, mc.theWorld, mc);
                return;
            }
        }

        float f5 = mc.theWorld.getRainStrength(partialTicks);

        if (f5 > 0.0F) {
            if (Config.isRainOff()) {
                return;
            }

            enableLightmap();
            Entity entity = mc.getRenderViewEntity();
            World world = mc.theWorld;
            int i = MathHelper.floor_double(entity.posX);
            int j = MathHelper.floor_double(entity.posY);
            int k = MathHelper.floor_double(entity.posZ);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            GlStateManager.disableCull();
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.alphaFunc(516, 0.1F);
            double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
            double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
            double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
            int l = MathHelper.floor_double(d1);
            int i1 = 5;

            if (Config.isRainFancy()) {
                i1 = 10;
            }

            int j1 = -1;
            float f = (float) rendererUpdateCount + partialTicks;
            worldrenderer.setTranslation(-d0, -d1, -d2);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int k1 = k - i1; k1 <= k + i1; ++k1) {
                for (int l1 = i - i1; l1 <= i + i1; ++l1) {
                    int i2 = (k1 - k + 16) * 32 + l1 - i + 16;
                    double d3 = (double) rainXCoords[i2] * 0.5D;
                    double d4 = (double) rainYCoords[i2] * 0.5D;
                    blockpos$mutableblockpos.set(l1, 0, k1);
                    BiomeGenBase biomegenbase = world.getBiomeGenForCoords(blockpos$mutableblockpos);

                    if (biomegenbase.canRain() || biomegenbase.getEnableSnow()) {
                        int j2 = world.getPrecipitationHeight(blockpos$mutableblockpos).getY();
                        int k2 = j - i1;
                        int l2 = j + i1;

                        if (k2 < j2) {
                            k2 = j2;
                        }

                        if (l2 < j2) {
                            l2 = j2;
                        }

                        int i3 = Math.max(j2, l);

                        if (k2 != l2) {
                            random.setSeed((long) l1 * l1 * 3121 + l1 * 45238971L ^ (long) k1 * k1 * 418711 + k1 * 13761L);
                            blockpos$mutableblockpos.set(l1, k2, k1);
                            float f1 = biomegenbase.getFloatTemperature(blockpos$mutableblockpos);

                            if (world.getWorldChunkManager().getTemperatureAtHeight(f1, j2) >= 0.15F) {
                                if (j1 != 0) {
                                    if (j1 >= 0) {
                                        tessellator.draw();
                                    }

                                    j1 = 0;
                                    mc.getTextureManager().bindTexture(locationRainPng);
                                    worldrenderer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                                }

                                double d5 = ((double) (rendererUpdateCount + l1 * l1 * 3121 + l1 * 45238971 + k1 * k1 * 418711 + k1 * 13761 & 31) + (double) partialTicks) / 32.0D * (3.0D + random.nextDouble());
                                double d6 = (double) ((float) l1 + 0.5F) - entity.posX;
                                double d7 = (double) ((float) k1 + 0.5F) - entity.posZ;
                                float f2 = MathHelper.sqrt_double(d6 * d6 + d7 * d7) / (float) i1;
                                float f3 = ((1.0F - f2 * f2) * 0.5F + 0.5F) * f5;
                                blockpos$mutableblockpos.set(l1, i3, k1);
                                int j3 = world.getCombinedLight(blockpos$mutableblockpos, 0);
                                int k3 = j3 >> 16 & 65535;
                                int l3 = j3 & 65535;
                                worldrenderer.pos((double) l1 - d3 + 0.5D, k2, (double) k1 - d4 + 0.5D).tex(0.0D, (double) k2 * 0.25D + d5).color(1.0F, 1.0F, 1.0F, f3).lightmap(k3, l3).endVertex();
                                worldrenderer.pos((double) l1 + d3 + 0.5D, k2, (double) k1 + d4 + 0.5D).tex(1.0D, (double) k2 * 0.25D + d5).color(1.0F, 1.0F, 1.0F, f3).lightmap(k3, l3).endVertex();
                                worldrenderer.pos((double) l1 + d3 + 0.5D, l2, (double) k1 + d4 + 0.5D).tex(1.0D, (double) l2 * 0.25D + d5).color(1.0F, 1.0F, 1.0F, f3).lightmap(k3, l3).endVertex();
                                worldrenderer.pos((double) l1 - d3 + 0.5D, l2, (double) k1 - d4 + 0.5D).tex(0.0D, (double) l2 * 0.25D + d5).color(1.0F, 1.0F, 1.0F, f3).lightmap(k3, l3).endVertex();
                            } else {
                                if (j1 != 1) {
                                    if (j1 >= 0) {
                                        tessellator.draw();
                                    }

                                    j1 = 1;
                                    mc.getTextureManager().bindTexture(locationSnowPng);
                                    worldrenderer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                                }

                                double d8 = ((float) (rendererUpdateCount & 511) + partialTicks) / 512.0F;
                                double d9 = random.nextDouble() + (double) f * 0.01D * (double) ((float) random.nextGaussian());
                                double d10 = random.nextDouble() + (double) (f * (float) random.nextGaussian()) * 0.001D;
                                double d11 = (double) ((float) l1 + 0.5F) - entity.posX;
                                double d12 = (double) ((float) k1 + 0.5F) - entity.posZ;
                                float f6 = MathHelper.sqrt_double(d11 * d11 + d12 * d12) / (float) i1;
                                float f4 = ((1.0F - f6 * f6) * 0.3F + 0.5F) * f5;
                                blockpos$mutableblockpos.set(l1, i3, k1);
                                int i4 = (world.getCombinedLight(blockpos$mutableblockpos, 0) * 3 + 15728880) / 4;
                                int j4 = i4 >> 16 & 65535;
                                int k4 = i4 & 65535;
                                worldrenderer.pos((double) l1 - d3 + 0.5D, k2, (double) k1 - d4 + 0.5D).tex(0.0D + d9, (double) k2 * 0.25D + d8 + d10).color(1.0F, 1.0F, 1.0F, f4).lightmap(j4, k4).endVertex();
                                worldrenderer.pos((double) l1 + d3 + 0.5D, k2, (double) k1 + d4 + 0.5D).tex(1.0D + d9, (double) k2 * 0.25D + d8 + d10).color(1.0F, 1.0F, 1.0F, f4).lightmap(j4, k4).endVertex();
                                worldrenderer.pos((double) l1 + d3 + 0.5D, l2, (double) k1 + d4 + 0.5D).tex(1.0D + d9, (double) l2 * 0.25D + d8 + d10).color(1.0F, 1.0F, 1.0F, f4).lightmap(j4, k4).endVertex();
                                worldrenderer.pos((double) l1 - d3 + 0.5D, l2, (double) k1 - d4 + 0.5D).tex(0.0D + d9, (double) l2 * 0.25D + d8 + d10).color(1.0F, 1.0F, 1.0F, f4).lightmap(j4, k4).endVertex();
                            }
                        }
                    }
                }
            }

            if (j1 >= 0) {
                tessellator.draw();
            }

            worldrenderer.setTranslation(0.0D, 0.0D, 0.0D);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(516, 0.1F);
            disableLightmap();
        }
    }

    public void setupOverlayRendering() {
        ScaledResolution scaledresolution = new ScaledResolution(mc);
        GlStateManager.clear(256);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);
    }

    private void updateFogColor(float partialTicks) {
        World world = mc.theWorld;
        Entity entity = mc.getRenderViewEntity();
        float f = 0.25F + 0.75F * (float) mc.gameSettings.renderDistanceChunks / 32.0F;
        f = 1.0F - (float) Math.pow(f, 0.25D);
        Vec3 vec3 = world.getSkyColor(mc.getRenderViewEntity(), partialTicks);
        vec3 = CustomColors.getWorldSkyColor(vec3, world, mc.getRenderViewEntity(), partialTicks);
        float f1 = (float) vec3.xCoord;
        float f2 = (float) vec3.yCoord;
        float f3 = (float) vec3.zCoord;
        Vec3 vec31 = world.getFogColor(partialTicks);
        vec31 = CustomColors.getWorldFogColor(vec31, world, mc.getRenderViewEntity(), partialTicks);
        fogColorRed = (float) vec31.xCoord;
        fogColorGreen = (float) vec31.yCoord;
        fogColorBlue = (float) vec31.zCoord;

        if (mc.gameSettings.renderDistanceChunks >= 4) {
            double d0 = -1.0D;
            Vec3 vec32 = MathHelper.sin(world.getCelestialAngleRadians(partialTicks)) > 0.0F ? new Vec3(d0, 0.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
            float f5 = (float) entity.getLook(partialTicks).dotProduct(vec32);

            if (f5 < 0.0F) {
                f5 = 0.0F;
            }

            if (f5 > 0.0F) {
                float[] afloat = world.provider.calcSunriseSunsetColors(world.getCelestialAngle(partialTicks), partialTicks);

                if (afloat != null) {
                    f5 = f5 * afloat[3];
                    fogColorRed = fogColorRed * (1.0F - f5) + afloat[0] * f5;
                    fogColorGreen = fogColorGreen * (1.0F - f5) + afloat[1] * f5;
                    fogColorBlue = fogColorBlue * (1.0F - f5) + afloat[2] * f5;
                }
            }
        }

        fogColorRed += (f1 - fogColorRed) * f;
        fogColorGreen += (f2 - fogColorGreen) * f;
        fogColorBlue += (f3 - fogColorBlue) * f;
        float f8 = world.getRainStrength(partialTicks);

        if (f8 > 0.0F) {
            float f4 = 1.0F - f8 * 0.5F;
            float f10 = 1.0F - f8 * 0.4F;
            fogColorRed *= f4;
            fogColorGreen *= f4;
            fogColorBlue *= f10;
        }

        float f9 = world.getThunderStrength(partialTicks);

        if (f9 > 0.0F) {
            float f11 = 1.0F - f9 * 0.5F;
            fogColorRed *= f11;
            fogColorGreen *= f11;
            fogColorBlue *= f11;
        }

        Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entity, partialTicks);

        if (cloudFog) {
            Vec3 vec33 = world.getCloudColour(partialTicks);
            fogColorRed = (float) vec33.xCoord;
            fogColorGreen = (float) vec33.yCoord;
            fogColorBlue = (float) vec33.zCoord;
        } else if (block.getMaterial() == Material.water) {
            float f12 = (float) EnchantmentHelper.getRespiration(entity) * 0.2F;
            f12 = Config.limit(f12, 0.0F, 0.6F);

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.waterBreathing)) {
                f12 = f12 * 0.3F + 0.6F;
            }

            fogColorRed = 0.02F + f12;
            fogColorGreen = 0.02F + f12;
            fogColorBlue = 0.2F + f12;
            Vec3 vec35 = CustomColors.getUnderwaterColor(mc.theWorld, mc.getRenderViewEntity().posX, mc.getRenderViewEntity().posY + 1.0D, mc.getRenderViewEntity().posZ);

            if (vec35 != null) {
                fogColorRed = (float) vec35.xCoord;
                fogColorGreen = (float) vec35.yCoord;
                fogColorBlue = (float) vec35.zCoord;
            }
        } else if (block.getMaterial() == Material.lava) {
            fogColorRed = 0.6F;
            fogColorGreen = 0.1F;
            fogColorBlue = 0.0F;
            Vec3 vec34 = CustomColors.getUnderlavaColor(mc.theWorld, mc.getRenderViewEntity().posX, mc.getRenderViewEntity().posY + 1.0D, mc.getRenderViewEntity().posZ);

            if (vec34 != null) {
                fogColorRed = (float) vec34.xCoord;
                fogColorGreen = (float) vec34.yCoord;
                fogColorBlue = (float) vec34.zCoord;
            }
        }

        float f13 = fogColor2 + (fogColor1 - fogColor2) * partialTicks;
        fogColorRed *= f13;
        fogColorGreen *= f13;
        fogColorBlue *= f13;
        double d1 = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks) * world.provider.getVoidFogYFactor();

        if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.blindness)) {
            int i = ((EntityLivingBase) entity).getActivePotionEffect(Potion.blindness).getDuration();

            if (i < 20) {
                d1 *= 1.0F - (float) i / 20.0F;
            } else {
                d1 = 0.0D;
            }
        }

        if (d1 < 1.0D) {
            if (d1 < 0.0D) {
                d1 = 0.0D;
            }

            d1 = d1 * d1;
            fogColorRed = (float) ((double) fogColorRed * d1);
            fogColorGreen = (float) ((double) fogColorGreen * d1);
            fogColorBlue = (float) ((double) fogColorBlue * d1);
        }

        if (bossColorModifier > 0.0F) {
            float f14 = bossColorModifierPrev + (bossColorModifier - bossColorModifierPrev) * partialTicks;
            fogColorRed = fogColorRed * (1.0F - f14) + fogColorRed * 0.7F * f14;
            fogColorGreen = fogColorGreen * (1.0F - f14) + fogColorGreen * 0.6F * f14;
            fogColorBlue = fogColorBlue * (1.0F - f14) + fogColorBlue * 0.6F * f14;
        }

        if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.nightVision)) {
            float f15 = getNightVisionBrightness((EntityLivingBase) entity, partialTicks);
            float f6 = 1.0F / fogColorRed;

            if (f6 > 1.0F / fogColorGreen) {
                f6 = 1.0F / fogColorGreen;
            }

            if (f6 > 1.0F / fogColorBlue) {
                f6 = 1.0F / fogColorBlue;
            }

            if (Float.isInfinite(f6)) {
                f6 = Math.nextAfter(f6, 0.0D);
            }

            fogColorRed = fogColorRed * (1.0F - f15) + fogColorRed * f6 * f15;
            fogColorGreen = fogColorGreen * (1.0F - f15) + fogColorGreen * f6 * f15;
            fogColorBlue = fogColorBlue * (1.0F - f15) + fogColorBlue * f6 * f15;
        }

        if (mc.gameSettings.anaglyph) {
            float f16 = (fogColorRed * 30.0F + fogColorGreen * 59.0F + fogColorBlue * 11.0F) / 100.0F;
            float f17 = (fogColorRed * 30.0F + fogColorGreen * 70.0F) / 100.0F;
            float f7 = (fogColorRed * 30.0F + fogColorBlue * 70.0F) / 100.0F;
            fogColorRed = f16;
            fogColorGreen = f17;
            fogColorBlue = f7;
        }

        if (Reflector.EntityViewRenderEvent_FogColors_Constructor.exists()) {
            Object object = Reflector.newInstance(Reflector.EntityViewRenderEvent_FogColors_Constructor, this, entity, block, partialTicks, fogColorRed, fogColorGreen, fogColorBlue);
            Reflector.postForgeBusEvent(object);
            fogColorRed = Reflector.getFieldValueFloat(object, Reflector.EntityViewRenderEvent_FogColors_red, fogColorRed);
            fogColorGreen = Reflector.getFieldValueFloat(object, Reflector.EntityViewRenderEvent_FogColors_green, fogColorGreen);
            fogColorBlue = Reflector.getFieldValueFloat(object, Reflector.EntityViewRenderEvent_FogColors_blue, fogColorBlue);
        }

        Shaders.setClearColor(fogColorRed, fogColorGreen, fogColorBlue, 0.0F);
    }

    private void setupFog(int startCoords, float partialTicks) {
        fogStandard = false;
        Entity entity = mc.getRenderViewEntity();

        GL11.glFogfv(GL11.GL_FOG_COLOR, setFogColorBuffer(fogColorRed, fogColorGreen, fogColorBlue));
        GL11.glNormal3f(0.0F, -1.0F, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entity, partialTicks);
        float f = -1.0F;

        if (Reflector.ForgeHooksClient_getFogDensity.exists()) {
            f = Reflector.callFloat(Reflector.ForgeHooksClient_getFogDensity, this, entity, block, partialTicks, 0.1F);
        }

        if (f >= 0.0F) {
            GlStateManager.setFogDensity(f);
        } else if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.blindness)) {
            float f4 = 5.0F;
            int i = ((EntityLivingBase) entity).getActivePotionEffect(Potion.blindness).getDuration();

            if (i < 20) {
                f4 = 5.0F + (farPlaneDistance - 5.0F) * (1.0F - (float) i / 20.0F);
            }

            GlStateManager.setFog(9729);

            if (startCoords == -1) {
                GlStateManager.setFogStart(0.0F);
                GlStateManager.setFogEnd(f4 * 0.8F);
            } else {
                GlStateManager.setFogStart(f4 * 0.25F);
                GlStateManager.setFogEnd(f4);
            }

            if (GLContext.getCapabilities().GL_NV_fog_distance && Config.isFogFancy()) {
                GL11.glFogi(34138, 34139);
            }
        } else if (cloudFog) {
            GlStateManager.setFog(2048);
            GlStateManager.setFogDensity(0.1F);
        } else if (block.getMaterial() == Material.water) {
            GlStateManager.setFog(2048);
            float f1 = Config.isClearWater() ? 0.02F : 0.1F;

            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.waterBreathing)) {
                GlStateManager.setFogDensity(0.01F);
            } else {
                float f2 = 0.1F - (float) EnchantmentHelper.getRespiration(entity) * 0.03F;
                GlStateManager.setFogDensity(Config.limit(f2, 0.0F, f1));
            }
        } else if (block.getMaterial() == Material.lava) {
            GlStateManager.setFog(2048);
            GlStateManager.setFogDensity(2.0F);
        } else {
            float f3 = farPlaneDistance;
            fogStandard = true;
            GlStateManager.setFog(9729);

            if (startCoords == -1) {
                GlStateManager.setFogStart(0.0F);
                GlStateManager.setFogEnd(f3);
            } else {
                GlStateManager.setFogStart(f3 * Config.getFogStart());
                GlStateManager.setFogEnd(f3);
            }

            if (GLContext.getCapabilities().GL_NV_fog_distance) {
                if (Config.isFogFancy()) {
                    GL11.glFogi(34138, 34139);
                }

                if (Config.isFogFast()) {
                    GL11.glFogi(34138, 34140);
                }
            }

            if (mc.theWorld.provider.doesXZShowFog((int) entity.posX, (int) entity.posZ)) {
                GlStateManager.setFogStart(f3 * 0.05F);
                GlStateManager.setFogEnd(f3);
            }

            if (Reflector.ForgeHooksClient_onFogRender.exists()) {
                Reflector.callVoid(Reflector.ForgeHooksClient_onFogRender, this, entity, block, partialTicks, startCoords, f3);
            }
        }

        Atmosphere atmosphere = Demise.INSTANCE.getModuleManager().getModule(Atmosphere.class);
        if (atmosphere.isEnabled() && atmosphere.worldFog.get()) {
            fogColorRed = (float) atmosphere.worldFogRGB.get().getRed() / 255;
            fogColorGreen = (float) atmosphere.worldFogRGB.get().getGreen() / 255;
            fogColorBlue = (float) atmosphere.worldFogRGB.get().getBlue() / 255;
        }

        GlStateManager.enableColorMaterial();
        GlStateManager.enableFog();
        GlStateManager.colorMaterial(1028, 4608);
    }

    private FloatBuffer setFogColorBuffer(float red, float green, float blue) {
        if (Config.isShaders()) {
            Shaders.setFogColor(red, green, blue);
        }

        fogColorBuffer.clear();
        fogColorBuffer.put(red).put(green).put(blue).put((float) 1.0);
        fogColorBuffer.flip();
        return fogColorBuffer;
    }

    public MapItemRenderer getMapItemRenderer() {
        return theMapItemRenderer;
    }

    private void waitForServerThread() {
        if (Config.isSmoothWorld() && Config.isSingleProcessor()) {
            if (mc.isIntegratedServerRunning()) {
                IntegratedServer integratedserver = mc.getIntegratedServer();

                if (integratedserver != null) {
                    boolean flag = mc.isGamePaused();

                    if (!flag && !(mc.currentScreen instanceof GuiDownloadTerrain)) {
                        if (serverWaitTime > 0) {
                            Lagometer.timerServer.start();
                            Config.sleep(serverWaitTime);
                            Lagometer.timerServer.end();
                        }

                        long i = System.nanoTime() / 1000000L;

                        if (lastServerTime != 0L && lastServerTicks != 0) {
                            long j = i - lastServerTime;

                            if (j < 0L) {
                                lastServerTime = i;
                                j = 0L;
                            }

                            if (j >= 50L) {
                                lastServerTime = i;
                                int k = integratedserver.getTickCounter();
                                int l = k - lastServerTicks;

                                if (l < 0) {
                                    lastServerTicks = k;
                                    l = 0;
                                }

                                if (l < 1 && serverWaitTime < 100) {
                                    serverWaitTime += 2;
                                }

                                if (l > 1 && serverWaitTime > 0) {
                                    --serverWaitTime;
                                }

                                lastServerTicks = k;
                            }
                        } else {
                            lastServerTime = i;
                            lastServerTicks = integratedserver.getTickCounter();
                        }
                    } else {
                        if (mc.currentScreen instanceof GuiDownloadTerrain) {
                            Config.sleep(20L);
                        }

                        lastServerTime = 0L;
                        lastServerTicks = 0;
                    }
                }
            }
        } else {
            lastServerTime = 0L;
            lastServerTicks = 0;
        }
    }

    private void frameInit() {
        GlErrors.frameStart();

        if (!initialized) {
            ReflectorResolver.resolve();
            TextureUtils.registerResourceListener();

            if (Config.getBitsOs() == 64 && Config.getBitsJre() == 32) {
                Config.setNotify64BitJava(true);
            }

            initialized = true;
        }

        Config.checkDisplayMode();
        World world = mc.theWorld;

        if (world != null) {
            if (Config.getNewRelease() != null) {
                String s = "HD_U".replace("HD_U", "HD Ultra").replace("L", "Light");
                String s1 = s + " " + Config.getNewRelease();
                ChatComponentText chatcomponenttext = new ChatComponentText(I18n.format("of.message.newVersion", "§n" + s1 + "§r"));
                chatcomponenttext.setChatStyle((new ChatStyle()).setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://optifine.net/downloads")));
                mc.ingameGUI.getChatGUI().printChatMessage(chatcomponenttext);
                Config.setNewRelease(null);
            }

            if (Config.isNotify64BitJava()) {
                Config.setNotify64BitJava(false);
                ChatComponentText chatcomponenttext1 = new ChatComponentText(I18n.format("of.message.java64Bit"));
                mc.ingameGUI.getChatGUI().printChatMessage(chatcomponenttext1);
            }
        }

        if (mc.currentScreen instanceof GuiMainMenu) {
            updateMainMenu((GuiMainMenu) mc.currentScreen);
        }

        if (updatedWorld != world) {
            RandomEntities.worldChanged(updatedWorld, world);
            Config.updateThreadPriorities();
            lastServerTime = 0L;
            lastServerTicks = 0;
            updatedWorld = world;
        }

        if (!setFxaaShader(Shaders.configAntialiasingLevel)) {
            Shaders.configAntialiasingLevel = 0;
        }

        if (mc.currentScreen != null && mc.currentScreen.getClass() == GuiChat.class) {
            mc.displayGuiScreen(new GuiChatOF((GuiChat) mc.currentScreen));
        }
    }

    private void frameFinish() {
        if (mc.theWorld != null && Config.isShowGlErrors() && TimedEvent.isActive("CheckGlErrorFrameFinish", 10000L)) {
            int i = GlStateManager.glGetError();

            if (i != 0 && GlErrors.isEnabled(i)) {
                String s = Config.getGlErrorString(i);
                ChatComponentText chatcomponenttext = new ChatComponentText(I18n.format("of.message.openglError", i, s));
                mc.ingameGUI.getChatGUI().printChatMessage(chatcomponenttext);
            }
        }
    }

    private void updateMainMenu(GuiMainMenu p_updateMainMenu_1_) {
        try {
            String s = null;
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            int i = calendar.get(Calendar.DATE);
            int j = calendar.get(Calendar.MONTH) + 1;

            // fuck y'all

            /*
            if (i == 8 && j == 4) {
                s = "Happy birthday, OptiFine!";
            }

            if (i == 14 && j == 8) {
                s = "Happy birthday, sp614x!";
            }

            if (s == null) {
                return;
            }

             */

            Reflector.setFieldValue(p_updateMainMenu_1_, Reflector.GuiMainMenu_splashText, s);
        } catch (Throwable ignored) {
        }
    }

    public boolean setFxaaShader(int p_setFxaaShader_1_) {
        if (!OpenGlHelper.isFramebufferEnabled()) {
            return false;
        } else if (theShaderGroup != null && theShaderGroup != fxaaShaders[2] && theShaderGroup != fxaaShaders[4]) {
            return true;
        } else if (p_setFxaaShader_1_ != 2 && p_setFxaaShader_1_ != 4) {
            if (theShaderGroup != null) {
                theShaderGroup.deleteShaderGroup();
                theShaderGroup = null;
            }
            return true;
        } else if (theShaderGroup != null && theShaderGroup == fxaaShaders[p_setFxaaShader_1_]) {
            return true;
        } else if (mc.theWorld == null) {
            return true;
        } else {
            loadShader(new ResourceLocation("shaders/post/fxaa_of_" + p_setFxaaShader_1_ + "x.json"));
            fxaaShaders[p_setFxaaShader_1_] = theShaderGroup;
            return useShader;
        }
    }

    private void checkLoadVisibleChunks(Entity p_checkLoadVisibleChunks_1_, float p_checkLoadVisibleChunks_2_, ICamera p_checkLoadVisibleChunks_3_, boolean p_checkLoadVisibleChunks_4_) {
        int i = 201435902;

        if (loadVisibleChunks) {
            loadVisibleChunks = false;
            loadAllVisibleChunks(p_checkLoadVisibleChunks_1_, p_checkLoadVisibleChunks_2_, p_checkLoadVisibleChunks_3_, p_checkLoadVisibleChunks_4_);
            mc.ingameGUI.getChatGUI().deleteChatLine(i);
        }

        if (Keyboard.isKeyDown(61) && Keyboard.isKeyDown(38)) {
            if (mc.currentScreen != null) {
                return;
            }

            loadVisibleChunks = true;
            ChatComponentText chatcomponenttext = new ChatComponentText(I18n.format("of.message.loadingVisibleChunks"));
            mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(chatcomponenttext, i);
        }
    }

    private void loadAllVisibleChunks(Entity p_loadAllVisibleChunks_1_, double p_loadAllVisibleChunks_2_, ICamera p_loadAllVisibleChunks_4_, boolean p_loadAllVisibleChunks_5_) {
        int i = mc.gameSettings.ofChunkUpdates;
        boolean flag = mc.gameSettings.ofLazyChunkLoading;

        try {
            mc.gameSettings.ofChunkUpdates = 1000;
            mc.gameSettings.ofLazyChunkLoading = false;
            RenderGlobal renderglobal = Config.getRenderGlobal();
            int j = renderglobal.getCountLoadedChunks();
            long k = System.currentTimeMillis();
            Config.dbg("Loading visible chunks");
            long l = System.currentTimeMillis() + 5000L;
            int i1 = 0;
            boolean flag1 = false;

            do {
                flag1 = false;

                for (int j1 = 0; j1 < 100; ++j1) {
                    renderglobal.displayListEntitiesDirty = true;
                    renderglobal.setupTerrain(p_loadAllVisibleChunks_1_, p_loadAllVisibleChunks_2_, p_loadAllVisibleChunks_4_, frameCount++, p_loadAllVisibleChunks_5_);

                    if (!renderglobal.hasNoChunkUpdates()) {
                        flag1 = true;
                    }

                    i1 = i1 + renderglobal.getCountChunksToUpdate();

                    while (!renderglobal.hasNoChunkUpdates()) {
                        renderglobal.updateChunks(System.nanoTime() + 1000000000L);
                    }

                    i1 = i1 - renderglobal.getCountChunksToUpdate();

                    if (!flag1) {
                        break;
                    }
                }

                if (renderglobal.getCountLoadedChunks() != j) {
                    flag1 = true;
                    j = renderglobal.getCountLoadedChunks();
                }

                if (System.currentTimeMillis() > l) {
                    Config.log("Chunks loaded: " + i1);
                    l = System.currentTimeMillis() + 5000L;
                }

            } while (flag1);

            Config.log("Chunks loaded: " + i1);
            Config.log("Finished loading visible chunks");
            RenderChunk.renderChunksUpdated = 0;
        } finally {
            mc.gameSettings.ofChunkUpdates = i;
            mc.gameSettings.ofLazyChunkLoading = flag;
        }
    }
}