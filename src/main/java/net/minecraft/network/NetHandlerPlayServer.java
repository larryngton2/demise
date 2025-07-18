package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.util.concurrent.Futures;
import io.netty.buffer.Unpooled;
import net.minecraft.block.material.Material;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityMinecartCommandBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.stats.AchievementList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.*;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NetHandlerPlayServer implements INetHandlerPlayServer, ITickable {
    private static final Logger logger = LogManager.getLogger();
    public final NetworkManager netManager;
    private final MinecraftServer serverController;
    public EntityPlayerMP playerEntity;
    private int networkTickCount;
    private int field_175090_f;
    private int floatingTickCount;
    private boolean field_147366_g;
    private int field_147378_h;
    private long lastPingTime;
    private long lastSentPingPacket;
    private int chatSpamThresholdCount;
    private int itemDropThreshold;
    private final IntHashMap<Short> field_147372_n = new IntHashMap();
    private double lastPosX;
    private double lastPosY;
    private double lastPosZ;
    private boolean hasMoved = true;

    public NetHandlerPlayServer(MinecraftServer server, NetworkManager networkManagerIn, EntityPlayerMP playerIn) {
        this.serverController = server;
        this.netManager = networkManagerIn;
        networkManagerIn.setNetHandler(this);
        this.playerEntity = playerIn;
        playerIn.playerNetServerHandler = this;
    }

    public void update() {
        this.field_147366_g = false;
        ++this.networkTickCount;
        this.serverController.theProfiler.startSection("keepAlive");

        if ((long) this.networkTickCount - this.lastSentPingPacket > 40L) {
            this.lastSentPingPacket = this.networkTickCount;
            this.lastPingTime = this.currentTimeMillis();
            this.field_147378_h = (int) this.lastPingTime;
            this.sendPacket(new S00PacketKeepAlive(this.field_147378_h));
        }

        this.serverController.theProfiler.endSection();

        if (this.chatSpamThresholdCount > 0) {
            --this.chatSpamThresholdCount;
        }

        if (this.itemDropThreshold > 0) {
            --this.itemDropThreshold;
        }

        if (this.playerEntity.getLastActiveTime() > 0L && this.serverController.getMaxPlayerIdleMinutes() > 0 && MinecraftServer.getCurrentTimeMillis() - this.playerEntity.getLastActiveTime() > ((long) this.serverController.getMaxPlayerIdleMinutes() * 1000 * 60)) {
            this.kickPlayerFromServer("You have been idle for too long!");
        }
    }

    public NetworkManager getNetworkManager() {
        return this.netManager;
    }

    public void kickPlayerFromServer(String reason) {
        final ChatComponentText chatcomponenttext = new ChatComponentText(reason);
        this.netManager.sendPacket(new S40PacketDisconnect(chatcomponenttext), p_operationComplete_1_ -> NetHandlerPlayServer.this.netManager.closeChannel(chatcomponenttext));
        this.netManager.disableAutoRead();
        Futures.getUnchecked(this.serverController.addScheduledTask(NetHandlerPlayServer.this.netManager::checkDisconnected));
    }

    public void processInput(C0CPacketInput packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.setEntityActionState(packetIn.getStrafeSpeed(), packetIn.getForwardSpeed(), packetIn.isJumping(), packetIn.isSneaking());
    }

    private boolean func_183006_b(C03PacketPlayer p_183006_1_) {
        return !Doubles.isFinite(p_183006_1_.getPositionX()) || !Doubles.isFinite(p_183006_1_.getPositionY()) || !Doubles.isFinite(p_183006_1_.getPositionZ()) || !Floats.isFinite(p_183006_1_.getPitch()) || !Floats.isFinite(p_183006_1_.getYaw());
    }

    public void processPlayer(C03PacketPlayer packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());

        if (this.func_183006_b(packetIn)) {
            this.kickPlayerFromServer("Invalid move packet received");
        } else {
            WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
            this.field_147366_g = true;

            if (!this.playerEntity.playerConqueredTheEnd) {
                double d0 = this.playerEntity.posX;
                double d1 = this.playerEntity.posY;
                double d2 = this.playerEntity.posZ;
                double d3 = 0.0D;
                double d4 = packetIn.getPositionX() - this.lastPosX;
                double d5 = packetIn.getPositionY() - this.lastPosY;
                double d6 = packetIn.getPositionZ() - this.lastPosZ;

                if (packetIn.isMoving()) {
                    d3 = d4 * d4 + d5 * d5 + d6 * d6;

                    if (!this.hasMoved && d3 < 0.25D) {
                        this.hasMoved = true;
                    }
                }

                if (this.hasMoved) {
                    this.field_175090_f = this.networkTickCount;

                    if (this.playerEntity.ridingEntity != null) {
                        float f4 = this.playerEntity.rotationYaw;
                        float f = this.playerEntity.rotationPitch;
                        this.playerEntity.ridingEntity.updateRiderPosition();
                        double d16 = this.playerEntity.posX;
                        double d17 = this.playerEntity.posY;
                        double d18 = this.playerEntity.posZ;

                        if (packetIn.getRotating()) {
                            f4 = packetIn.getYaw();
                            f = packetIn.getPitch();
                        }

                        this.playerEntity.onGround = packetIn.isOnGround();
                        this.playerEntity.onUpdateEntity();
                        this.playerEntity.setPositionAndRotation(d16, d17, d18, f4, f);

                        if (this.playerEntity.ridingEntity != null) {
                            this.playerEntity.ridingEntity.updateRiderPosition();
                        }

                        this.serverController.getConfigurationManager().serverUpdateMountedMovingPlayer(this.playerEntity);

                        if (this.playerEntity.ridingEntity != null) {
                            if (d3 > 4.0D) {
                                Entity entity = this.playerEntity.ridingEntity;
                                this.playerEntity.playerNetServerHandler.sendPacket(new S18PacketEntityTeleport(entity));
                                this.setPlayerLocation(this.playerEntity.posX, this.playerEntity.posY, this.playerEntity.posZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                            }

                            this.playerEntity.ridingEntity.isAirBorne = true;
                        }

                        if (this.hasMoved) {
                            this.lastPosX = this.playerEntity.posX;
                            this.lastPosY = this.playerEntity.posY;
                            this.lastPosZ = this.playerEntity.posZ;
                        }

                        worldserver.updateEntity(this.playerEntity);
                        return;
                    }

                    if (this.playerEntity.isPlayerSleeping()) {
                        this.playerEntity.onUpdateEntity();
                        this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                        worldserver.updateEntity(this.playerEntity);
                        return;
                    }

                    double d7 = this.playerEntity.posY;
                    this.lastPosX = this.playerEntity.posX;
                    this.lastPosY = this.playerEntity.posY;
                    this.lastPosZ = this.playerEntity.posZ;
                    double d8 = this.playerEntity.posX;
                    double d9 = this.playerEntity.posY;
                    double d10 = this.playerEntity.posZ;
                    float f1 = this.playerEntity.rotationYaw;
                    float f2 = this.playerEntity.rotationPitch;

                    if (packetIn.isMoving() && packetIn.getPositionY() == -999.0D) {
                        packetIn.setMoving(false);
                    }

                    if (packetIn.isMoving()) {
                        d8 = packetIn.getPositionX();
                        d9 = packetIn.getPositionY();
                        d10 = packetIn.getPositionZ();

                        if (Math.abs(packetIn.getPositionX()) > 3.0E7D || Math.abs(packetIn.getPositionZ()) > 3.0E7D) {
                            this.kickPlayerFromServer("Illegal position");
                            return;
                        }
                    }

                    if (packetIn.getRotating()) {
                        f1 = packetIn.getYaw();
                        f2 = packetIn.getPitch();
                    }

                    this.playerEntity.onUpdateEntity();
                    this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, f1, f2);

                    if (!this.hasMoved) {
                        return;
                    }

                    double d11 = d8 - this.playerEntity.posX;
                    double d12 = d9 - this.playerEntity.posY;
                    double d13 = d10 - this.playerEntity.posZ;
                    double d14 = this.playerEntity.motionX * this.playerEntity.motionX + this.playerEntity.motionY * this.playerEntity.motionY + this.playerEntity.motionZ * this.playerEntity.motionZ;
                    double d15 = d11 * d11 + d12 * d12 + d13 * d13;

                    if (d15 - d14 > 100.0D && (!this.serverController.isSinglePlayer() || !this.serverController.getServerOwner().equals(this.playerEntity.getName()))) {
                        logger.warn("{} moved too quickly! {},{},{} ({}, {}, {})", this.playerEntity.getName(), d11, d12, d13, d11, d12, d13);
                        this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                        return;
                    }

                    float f3 = 0.0625F;
                    boolean flag = worldserver.getCollidingBoundingBoxes(this.playerEntity, this.playerEntity.getEntityBoundingBox().contract(f3, f3, f3)).isEmpty();

                    if (this.playerEntity.onGround && !packetIn.isOnGround() && d12 > 0.0D) {
                        this.playerEntity.jump();
                    }

                    this.playerEntity.moveEntity(d11, d12, d13);
                    this.playerEntity.onGround = packetIn.isOnGround();
                    d11 = d8 - this.playerEntity.posX;
                    d12 = d9 - this.playerEntity.posY;

                    if (d12 > -0.5D || d12 < 0.5D) {
                        d12 = 0.0D;
                    }

                    d13 = d10 - this.playerEntity.posZ;
                    d15 = d11 * d11 + d12 * d12 + d13 * d13;
                    boolean flag1 = false;

                    if (d15 > 0.0625D && !this.playerEntity.isPlayerSleeping() && !this.playerEntity.theItemInWorldManager.isCreative()) {
                        flag1 = true;
                        logger.warn("{} moved wrongly!", this.playerEntity.getName());
                    }

                    this.playerEntity.setPositionAndRotation(d8, d9, d10, f1, f2);
                    this.playerEntity.addMovementStat(this.playerEntity.posX - d0, this.playerEntity.posY - d1, this.playerEntity.posZ - d2);

                    if (!this.playerEntity.noClip) {
                        boolean flag2 = worldserver.getCollidingBoundingBoxes(this.playerEntity, this.playerEntity.getEntityBoundingBox().contract(f3, f3, f3)).isEmpty();

                        if (flag && (flag1 || !flag2) && !this.playerEntity.isPlayerSleeping()) {
                            this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, f1, f2);
                            return;
                        }
                    }

                    AxisAlignedBB axisalignedbb = this.playerEntity.getEntityBoundingBox().expand(f3, f3, f3).addCoord(0.0D, -0.55D, 0.0D);

                    if (!this.serverController.isFlightAllowed() && !this.playerEntity.capabilities.allowFlying && !worldserver.checkBlockCollision(axisalignedbb)) {
                        if (d12 >= -0.03125D) {
                            ++this.floatingTickCount;

                            if (this.floatingTickCount > 80) {
                                logger.warn("{} was kicked for floating too long!", this.playerEntity.getName());
                                this.kickPlayerFromServer("Flying is not enabled on this server");
                                return;
                            }
                        }
                    } else {
                        this.floatingTickCount = 0;
                    }

                    this.playerEntity.onGround = packetIn.isOnGround();
                    this.serverController.getConfigurationManager().serverUpdateMountedMovingPlayer(this.playerEntity);
                    this.playerEntity.handleFalling(this.playerEntity.posY - d7, packetIn.isOnGround());
                } else if (this.networkTickCount - this.field_175090_f > 20) {
                    this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                }
            }
        }
    }

    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch) {
        this.setPlayerLocation(x, y, z, yaw, pitch, Collections.emptySet());
    }

    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch, Set<S08PacketPlayerPosLook.EnumFlags> relativeSet) {
        this.hasMoved = false;
        this.lastPosX = x;
        this.lastPosY = y;
        this.lastPosZ = z;

        if (relativeSet.contains(S08PacketPlayerPosLook.EnumFlags.X)) {
            this.lastPosX += this.playerEntity.posX;
        }

        if (relativeSet.contains(S08PacketPlayerPosLook.EnumFlags.Y)) {
            this.lastPosY += this.playerEntity.posY;
        }

        if (relativeSet.contains(S08PacketPlayerPosLook.EnumFlags.Z)) {
            this.lastPosZ += this.playerEntity.posZ;
        }

        float f = yaw;
        float f1 = pitch;

        if (relativeSet.contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) {
            f = yaw + this.playerEntity.rotationYaw;
        }

        if (relativeSet.contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) {
            f1 = pitch + this.playerEntity.rotationPitch;
        }

        this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, f, f1);
        this.playerEntity.playerNetServerHandler.sendPacket(new S08PacketPlayerPosLook(x, y, z, yaw, pitch, relativeSet));
    }

    public void processPlayerDigging(C07PacketPlayerDigging packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        BlockPos blockpos = packetIn.getPosition();
        this.playerEntity.markPlayerActive();

        switch (packetIn.getStatus()) {
            case DROP_ITEM:
                if (!this.playerEntity.isSpectator()) {
                    this.playerEntity.dropOneItem(false);
                }

                return;

            case DROP_ALL_ITEMS:
                if (!this.playerEntity.isSpectator()) {
                    this.playerEntity.dropOneItem(true);
                }

                return;

            case RELEASE_USE_ITEM:
                this.playerEntity.stopUsingItem();
                return;

            case START_DESTROY_BLOCK:
            case ABORT_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK:
                double d0 = this.playerEntity.posX - ((double) blockpos.getX() + 0.5D);
                double d1 = this.playerEntity.posY - ((double) blockpos.getY() + 0.5D) + 1.5D;
                double d2 = this.playerEntity.posZ - ((double) blockpos.getZ() + 0.5D);
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;

                if (d3 > 36.0D) {
                    return;
                } else if (blockpos.getY() >= this.serverController.getBuildLimit()) {
                    return;
                } else {
                    if (packetIn.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                        if (!this.serverController.isBlockProtected(worldserver, blockpos, this.playerEntity) && worldserver.getWorldBorder().contains(blockpos)) {
                            this.playerEntity.theItemInWorldManager.onBlockClicked(blockpos, packetIn.getFacing());
                        } else {
                            this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldserver, blockpos));
                        }
                    } else {
                        if (packetIn.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                            this.playerEntity.theItemInWorldManager.blockRemoving(blockpos);
                        } else if (packetIn.getStatus() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
                            this.playerEntity.theItemInWorldManager.cancelDestroyingBlock();
                        }

                        if (worldserver.getBlockState(blockpos).getBlock().getMaterial() != Material.air) {
                            this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldserver, blockpos));
                        }
                    }

                    return;
                }

            default:
                throw new IllegalArgumentException("Invalid player action");
        }
    }

    public void processPlayerBlockPlacement(C08PacketPlayerBlockPlacement packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        ItemStack itemstack = this.playerEntity.inventory.getCurrentItem();
        boolean flag = false;
        BlockPos blockpos = packetIn.getPosition();
        EnumFacing enumfacing = EnumFacing.getFront(packetIn.getPlacedBlockDirection());
        this.playerEntity.markPlayerActive();

        if (packetIn.getPlacedBlockDirection() == 255) {
            if (itemstack == null) {
                return;
            }

            this.playerEntity.theItemInWorldManager.tryUseItem(this.playerEntity, worldserver, itemstack);
        } else if (blockpos.getY() < this.serverController.getBuildLimit() - 1 || enumfacing != EnumFacing.UP && blockpos.getY() < this.serverController.getBuildLimit()) {
            if (this.hasMoved && this.playerEntity.getDistanceSq((double) blockpos.getX() + 0.5D, (double) blockpos.getY() + 0.5D, (double) blockpos.getZ() + 0.5D) < 64.0D && !this.serverController.isBlockProtected(worldserver, blockpos, this.playerEntity) && worldserver.getWorldBorder().contains(blockpos)) {
                this.playerEntity.theItemInWorldManager.activateBlockOrUseItem(this.playerEntity, worldserver, itemstack, blockpos, enumfacing, packetIn.getPlacedBlockOffsetX(), packetIn.getPlacedBlockOffsetY(), packetIn.getPlacedBlockOffsetZ());
            }

            flag = true;
        } else {
            ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("build.tooHigh", this.serverController.getBuildLimit());
            chatcomponenttranslation.getChatStyle().setColor(EnumChatFormatting.RED);
            this.playerEntity.playerNetServerHandler.sendPacket(new S02PacketChat(chatcomponenttranslation));
            flag = true;
        }

        if (flag) {
            this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldserver, blockpos));
            this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldserver, blockpos.offset(enumfacing)));
        }

        itemstack = this.playerEntity.inventory.getCurrentItem();

        if (itemstack != null && itemstack.stackSize == 0) {
            this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem] = null;
            itemstack = null;
        }

        if (itemstack == null || itemstack.getMaxItemUseDuration() == 0) {
            this.playerEntity.isChangingQuantityOnly = true;
            this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem] = ItemStack.copyItemStack(this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem]);
            Slot slot = this.playerEntity.openContainer.getSlotFromInventory(this.playerEntity.inventory, this.playerEntity.inventory.currentItem);
            this.playerEntity.openContainer.detectAndSendChanges();
            this.playerEntity.isChangingQuantityOnly = false;

            if (!ItemStack.areItemStacksEqual(this.playerEntity.inventory.getCurrentItem(), packetIn.getStack())) {
                this.sendPacket(new S2FPacketSetSlot(this.playerEntity.openContainer.windowId, slot.slotNumber, this.playerEntity.inventory.getCurrentItem()));
            }
        }
    }

    public void handleSpectate(C18PacketSpectate packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());

        if (this.playerEntity.isSpectator()) {
            Entity entity = null;

            for (WorldServer worldserver : this.serverController.worldServers) {
                if (worldserver != null) {
                    entity = packetIn.getEntity(worldserver);

                    if (entity != null) {
                        break;
                    }
                }
            }

            if (entity != null) {
                this.playerEntity.setSpectatingEntity(this.playerEntity);
                this.playerEntity.mountEntity(null);

                if (entity.worldObj != this.playerEntity.worldObj) {
                    WorldServer worldserver1 = this.playerEntity.getServerForPlayer();
                    WorldServer worldserver2 = (WorldServer) entity.worldObj;
                    this.playerEntity.dimension = entity.dimension;
                    this.sendPacket(new S07PacketRespawn(this.playerEntity.dimension, worldserver1.getDifficulty(), worldserver1.getWorldInfo().getTerrainType(), this.playerEntity.theItemInWorldManager.getGameType()));
                    worldserver1.removePlayerEntityDangerously(this.playerEntity);
                    this.playerEntity.isDead = false;
                    this.playerEntity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch);

                    if (this.playerEntity.isEntityAlive()) {
                        worldserver1.updateEntityWithOptionalForce(this.playerEntity, false);
                        worldserver2.spawnEntityInWorld(this.playerEntity);
                        worldserver2.updateEntityWithOptionalForce(this.playerEntity, false);
                    }

                    this.playerEntity.setWorld(worldserver2);
                    this.serverController.getConfigurationManager().preparePlayer(this.playerEntity, worldserver1);
                    this.playerEntity.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
                    this.playerEntity.theItemInWorldManager.setWorld(worldserver2);
                    this.serverController.getConfigurationManager().updateTimeAndWeatherForPlayer(this.playerEntity, worldserver2);
                    this.serverController.getConfigurationManager().syncPlayerInventory(this.playerEntity);
                } else {
                    this.playerEntity.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
                }
            }
        }
    }

    public void handleResourcePackStatus(C19PacketResourcePackStatus packetIn) {
    }

    public void onDisconnect(IChatComponent reason) {
        logger.info("{} lost connection: {}", this.playerEntity.getName(), reason);
        this.serverController.refreshStatusNextTick();
        ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("multiplayer.player.left", this.playerEntity.getDisplayName());
        chatcomponenttranslation.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        this.serverController.getConfigurationManager().sendChatMsg(chatcomponenttranslation);
        this.playerEntity.mountEntityAndWakeUp();
        this.serverController.getConfigurationManager().playerLoggedOut(this.playerEntity);

        if (this.serverController.isSinglePlayer() && this.playerEntity.getName().equals(this.serverController.getServerOwner())) {
            logger.info("Stopping singleplayer server as player logged out");
            this.serverController.initiateShutdown();
        }
    }

    public void sendPacket(final Packet packetIn) {
        if (packetIn instanceof S02PacketChat s02packetchat) {
            EntityPlayer.EnumChatVisibility entityplayer$enumchatvisibility = this.playerEntity.getChatVisibility();

            if (entityplayer$enumchatvisibility == EntityPlayer.EnumChatVisibility.HIDDEN) {
                return;
            }

            if (entityplayer$enumchatvisibility == EntityPlayer.EnumChatVisibility.SYSTEM && !s02packetchat.isChat()) {
                return;
            }
        }

        try {
            this.netManager.sendPacket(packetIn);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Sending packet");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Packet being sent");
            crashreportcategory.addCrashSectionCallable("Packet class", () -> packetIn.getClass().getCanonicalName());
            throw new ReportedException(crashreport);
        }
    }

    public void processHeldItemChange(C09PacketHeldItemChange packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());

        if (packetIn.getSlotId() >= 0 && packetIn.getSlotId() < InventoryPlayer.getHotbarSize()) {
            this.playerEntity.inventory.currentItem = packetIn.getSlotId();
            this.playerEntity.markPlayerActive();
        } else {
            logger.warn("{} tried to set an invalid carried item", this.playerEntity.getName());
        }
    }

    public void processChatMessage(C01PacketChatMessage packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());

        if (this.playerEntity.getChatVisibility() == EntityPlayer.EnumChatVisibility.HIDDEN) {
            ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("chat.cannotSend");
            chatcomponenttranslation.getChatStyle().setColor(EnumChatFormatting.RED);
            this.sendPacket(new S02PacketChat(chatcomponenttranslation));
        } else {
            this.playerEntity.markPlayerActive();
            String s = packetIn.getMessage();
            s = StringUtils.normalizeSpace(s);

            for (int i = 0; i < s.length(); ++i) {
                if (!ChatAllowedCharacters.isAllowedCharacter(s.charAt(i))) {
                    this.kickPlayerFromServer("Illegal characters in chat");
                    return;
                }
            }

            if (s.startsWith("/")) {
                this.handleSlashCommand(s);
            } else {
                IChatComponent ichatcomponent = new ChatComponentTranslation("chat.type.text", this.playerEntity.getDisplayName(), s);
                this.serverController.getConfigurationManager().sendChatMsgImpl(ichatcomponent, false);
            }

            this.chatSpamThresholdCount += 20;

            if (this.chatSpamThresholdCount > 200 && !this.serverController.getConfigurationManager().canSendCommands(this.playerEntity.getGameProfile())) {
                this.kickPlayerFromServer("disconnect.spam");
            }
        }
    }

    private void handleSlashCommand(String command) {
        this.serverController.getCommandManager().executeCommand(this.playerEntity, command);
    }

    public void handleAnimation(C0APacketAnimation packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();
        this.playerEntity.swingItem();
    }

    public void processEntityAction(C0BPacketEntityAction packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();

        switch (packetIn.getAction()) {
            case START_SNEAKING:
                this.playerEntity.setSneaking(true);
                break;

            case STOP_SNEAKING:
                this.playerEntity.setSneaking(false);
                break;

            case START_SPRINTING:
                this.playerEntity.setSprinting(true);
                break;

            case STOP_SPRINTING:
                this.playerEntity.setSprinting(false);
                break;

            case STOP_SLEEPING:
                this.playerEntity.wakeUpPlayer(false, true, true);
                this.hasMoved = false;
                break;

            case RIDING_JUMP:
                if (this.playerEntity.ridingEntity instanceof EntityHorse) {
                    ((EntityHorse) this.playerEntity.ridingEntity).setJumpPower(packetIn.getAuxData());
                }

                break;

            case OPEN_INVENTORY:
                if (this.playerEntity.ridingEntity instanceof EntityHorse) {
                    ((EntityHorse) this.playerEntity.ridingEntity).openGUI(this.playerEntity);
                }

                break;

            default:
                throw new IllegalArgumentException("Invalid client command!");
        }
    }

    public void processUseEntity(C02PacketUseEntity packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        Entity entity = packetIn.getEntityFromWorld(worldserver);
        this.playerEntity.markPlayerActive();

        if (entity != null) {
            boolean flag = this.playerEntity.canEntityBeSeen(entity);
            double d0 = 36.0D;

            if (!flag) {
                d0 = 9.0D;
            }

            if (this.playerEntity.getDistanceSqToEntity(entity) < d0) {
                if (packetIn.getAction() == C02PacketUseEntity.Action.INTERACT) {
                    this.playerEntity.interactWith(entity);
                } else if (packetIn.getAction() == C02PacketUseEntity.Action.INTERACT_AT) {
                    entity.interactAt(this.playerEntity, packetIn.getHitVec());
                } else if (packetIn.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    if (entity instanceof EntityItem || entity instanceof EntityXPOrb || entity instanceof EntityArrow || entity == this.playerEntity) {
                        this.kickPlayerFromServer("Attempting to attack an invalid entity");
                        this.serverController.logWarning("Player " + this.playerEntity.getName() + " tried to attack an invalid entity");
                        return;
                    }

                    this.playerEntity.attackTargetEntityWithCurrentItem(entity);
                }
            }
        }
    }

    public void processClientStatus(C16PacketClientStatus packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();
        C16PacketClientStatus.EnumState c16packetclientstatus$enumstate = packetIn.getStatus();

        switch (c16packetclientstatus$enumstate) {
            case PERFORM_RESPAWN:
                if (this.playerEntity.playerConqueredTheEnd) {
                    this.playerEntity = this.serverController.getConfigurationManager().recreatePlayerEntity(this.playerEntity, 0, true);
                } else if (this.playerEntity.getServerForPlayer().getWorldInfo().isHardcoreModeEnabled()) {
                    if (this.serverController.isSinglePlayer() && this.playerEntity.getName().equals(this.serverController.getServerOwner())) {
                        this.playerEntity.playerNetServerHandler.kickPlayerFromServer("You have died. Game over, man, it's game over!");
                        this.serverController.deleteWorldAndStopServer();
                    } else {
                        UserListBansEntry userlistbansentry = new UserListBansEntry(this.playerEntity.getGameProfile(), null, "(You just lost the game)", null, "Death in Hardcore");
                        this.serverController.getConfigurationManager().getBannedPlayers().addEntry(userlistbansentry);
                        this.playerEntity.playerNetServerHandler.kickPlayerFromServer("You have died. Game over, man, it's game over!");
                    }
                } else {
                    if (this.playerEntity.getHealth() > 0.0F) {
                        return;
                    }

                    this.playerEntity = this.serverController.getConfigurationManager().recreatePlayerEntity(this.playerEntity, 0, false);
                }

                break;

            case REQUEST_STATS:
                this.playerEntity.getStatFile().func_150876_a(this.playerEntity);
                break;

            case OPEN_INVENTORY_ACHIEVEMENT:
                this.playerEntity.triggerAchievement(AchievementList.openInventory);
        }
    }

    public void processCloseWindow(C0DPacketCloseWindow packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.closeContainer();
    }

    public void processClickWindow(C0EPacketClickWindow packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();

        if (this.playerEntity.openContainer.windowId == packetIn.getWindowId() && this.playerEntity.openContainer.getCanCraft(this.playerEntity)) {
            if (this.playerEntity.isSpectator()) {
                List<ItemStack> list = Lists.newArrayList();

                for (int i = 0; i < this.playerEntity.openContainer.inventorySlots.size(); ++i) {
                    list.add(this.playerEntity.openContainer.inventorySlots.get(i).getStack());
                }

                this.playerEntity.updateCraftingInventory(this.playerEntity.openContainer, list);
            } else {
                ItemStack itemstack = this.playerEntity.openContainer.slotClick(packetIn.getSlotId(), packetIn.getUsedButton(), packetIn.getMode(), this.playerEntity);

                if (ItemStack.areItemStacksEqual(packetIn.getClickedItem(), itemstack)) {
                    this.playerEntity.playerNetServerHandler.sendPacket(new S32PacketConfirmTransaction(packetIn.getWindowId(), packetIn.getActionNumber(), true));
                    this.playerEntity.isChangingQuantityOnly = true;
                    this.playerEntity.openContainer.detectAndSendChanges();
                    this.playerEntity.updateHeldItem();
                    this.playerEntity.isChangingQuantityOnly = false;
                } else {
                    this.field_147372_n.addKey(this.playerEntity.openContainer.windowId, packetIn.getActionNumber());
                    this.playerEntity.playerNetServerHandler.sendPacket(new S32PacketConfirmTransaction(packetIn.getWindowId(), packetIn.getActionNumber(), false));
                    this.playerEntity.openContainer.setCanCraft(this.playerEntity, false);
                    List<ItemStack> list1 = Lists.newArrayList();

                    for (int j = 0; j < this.playerEntity.openContainer.inventorySlots.size(); ++j) {
                        list1.add(this.playerEntity.openContainer.inventorySlots.get(j).getStack());
                    }

                    this.playerEntity.updateCraftingInventory(this.playerEntity.openContainer, list1);
                }
            }
        }
    }

    public void processEnchantItem(C11PacketEnchantItem packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();

        if (this.playerEntity.openContainer.windowId == packetIn.getWindowId() && this.playerEntity.openContainer.getCanCraft(this.playerEntity) && !this.playerEntity.isSpectator()) {
            this.playerEntity.openContainer.enchantItem(this.playerEntity, packetIn.getButton());
            this.playerEntity.openContainer.detectAndSendChanges();
        }
    }

    public void processCreativeInventoryAction(C10PacketCreativeInventoryAction packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());

        if (this.playerEntity.theItemInWorldManager.isCreative()) {
            boolean flag = packetIn.getSlotId() < 0;
            ItemStack itemstack = packetIn.getStack();

            if (itemstack != null && itemstack.hasTagCompound() && itemstack.getTagCompound().hasKey("BlockEntityTag", 10)) {
                NBTTagCompound nbttagcompound = itemstack.getTagCompound().getCompoundTag("BlockEntityTag");

                if (nbttagcompound.hasKey("x") && nbttagcompound.hasKey("y") && nbttagcompound.hasKey("z")) {
                    BlockPos blockpos = new BlockPos(nbttagcompound.getInteger("x"), nbttagcompound.getInteger("y"), nbttagcompound.getInteger("z"));
                    TileEntity tileentity = this.playerEntity.worldObj.getTileEntity(blockpos);

                    if (tileentity != null) {
                        NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                        tileentity.writeToNBT(nbttagcompound1);
                        nbttagcompound1.removeTag("x");
                        nbttagcompound1.removeTag("y");
                        nbttagcompound1.removeTag("z");
                        itemstack.setTagInfo("BlockEntityTag", nbttagcompound1);
                    }
                }
            }

            boolean flag1 = packetIn.getSlotId() >= 1 && packetIn.getSlotId() < 36 + InventoryPlayer.getHotbarSize();
            boolean flag2 = itemstack == null || itemstack.getItem() != null;
            boolean flag3 = itemstack == null || itemstack.getMetadata() >= 0 && itemstack.stackSize <= 64 && itemstack.stackSize > 0;

            if (flag1 && flag2 && flag3) {
                this.playerEntity.inventoryContainer.putStackInSlot(packetIn.getSlotId(), itemstack);

                this.playerEntity.inventoryContainer.setCanCraft(this.playerEntity, true);
            } else if (flag && flag2 && flag3 && this.itemDropThreshold < 200) {
                this.itemDropThreshold += 20;
                EntityItem entityitem = this.playerEntity.dropPlayerItemWithRandomChoice(itemstack, true);

                if (entityitem != null) {
                    entityitem.setAgeToCreativeDespawnTime();
                }
            }
        }
    }

    public void processConfirmTransaction(C0FPacketConfirmTransaction packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        Short oshort = this.field_147372_n.lookup(this.playerEntity.openContainer.windowId);

        if (oshort != null && packetIn.getUid() == oshort && this.playerEntity.openContainer.windowId == packetIn.getWindowId() && !this.playerEntity.openContainer.getCanCraft(this.playerEntity) && !this.playerEntity.isSpectator()) {
            this.playerEntity.openContainer.setCanCraft(this.playerEntity, true);
        }
    }

    public void processUpdateSign(C12PacketUpdateSign packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();
        WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        BlockPos blockpos = packetIn.getPosition();

        if (worldserver.isBlockLoaded(blockpos)) {
            TileEntity tileentity = worldserver.getTileEntity(blockpos);

            if (!(tileentity instanceof TileEntitySign tileentitysign)) {
                return;
            }

            if (!tileentitysign.getIsEditable() || tileentitysign.getPlayer() != this.playerEntity) {
                this.serverController.logWarning("Player " + this.playerEntity.getName() + " just tried to change non-editable sign");
                return;
            }

            IChatComponent[] aichatcomponent = packetIn.getLines();

            for (int i = 0; i < aichatcomponent.length; ++i) {
                tileentitysign.signText[i] = new ChatComponentText(EnumChatFormatting.getTextWithoutFormattingCodes(aichatcomponent[i].getUnformattedText()));
            }

            tileentitysign.markDirty();
            worldserver.markBlockForUpdate(blockpos);
        }
    }

    public void processKeepAlive(C00PacketKeepAlive packetIn) {
        if (packetIn.getKey() == this.field_147378_h) {
            int i = (int) (this.currentTimeMillis() - this.lastPingTime);
            this.playerEntity.ping = (this.playerEntity.ping * 3 + i) / 4;
        }
    }

    private long currentTimeMillis() {
        return System.nanoTime() / 1000000L;
    }

    public void processPlayerAbilities(C13PacketPlayerAbilities packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.capabilities.isFlying = packetIn.isFlying() && this.playerEntity.capabilities.allowFlying;
    }

    public void processTabComplete(C14PacketTabComplete packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        List<String> list = Lists.newArrayList();

        list.addAll(this.serverController.getTabCompletions(this.playerEntity, packetIn.getMessage(), packetIn.getTargetBlock()));

        this.playerEntity.playerNetServerHandler.sendPacket(new S3APacketTabComplete(list.toArray(new String[0])));
    }

    public void processClientSettings(C15PacketClientSettings packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.handleClientSettings(packetIn);
    }

    public void processVanilla250Packet(C17PacketCustomPayload packetIn) {
        PacketThreadUtil.checkThreadAndEnqueue(packetIn, this, this.playerEntity.getServerForPlayer());

        if ("MC|BEdit".equals(packetIn.getChannelName())) {
            PacketBuffer packetbuffer3 = new PacketBuffer(Unpooled.wrappedBuffer(packetIn.getBufferData()));

            try {
                ItemStack itemstack1 = packetbuffer3.readItemStackFromBuffer();

                if (itemstack1 != null) {
                    if (!ItemWritableBook.isNBTValid(itemstack1.getTagCompound())) {
                        throw new IOException("Invalid book tag!");
                    }

                    ItemStack itemstack3 = this.playerEntity.inventory.getCurrentItem();

                    if (itemstack3 == null) {
                        return;
                    }

                    if (itemstack1.getItem() == Items.writable_book && itemstack1.getItem() == itemstack3.getItem()) {
                        itemstack3.setTagInfo("pages", itemstack1.getTagCompound().getTagList("pages", 8));
                    }

                }
            } catch (Exception exception3) {
                logger.error("Couldn't handle book info", exception3);
            } finally {
                packetbuffer3.release();
            }

        } else if ("MC|BSign".equals(packetIn.getChannelName())) {
            PacketBuffer packetbuffer2 = new PacketBuffer(Unpooled.wrappedBuffer(packetIn.getBufferData()));

            try {
                ItemStack itemstack = packetbuffer2.readItemStackFromBuffer();

                if (itemstack != null) {
                    if (!ItemEditableBook.validBookTagContents(itemstack.getTagCompound())) {
                        throw new IOException("Invalid book tag!");
                    }

                    ItemStack itemstack2 = this.playerEntity.inventory.getCurrentItem();

                    if (itemstack2 == null) {
                        return;
                    }

                    if (itemstack.getItem() == Items.written_book && itemstack2.getItem() == Items.writable_book) {
                        itemstack2.setTagInfo("author", new NBTTagString(this.playerEntity.getName()));
                        itemstack2.setTagInfo("title", new NBTTagString(itemstack.getTagCompound().getString("title")));
                        itemstack2.setTagInfo("pages", itemstack.getTagCompound().getTagList("pages", 8));
                        itemstack2.setItem(Items.written_book);
                    }

                }
            } catch (Exception exception4) {
                logger.error("Couldn't sign book", exception4);
            } finally {
                packetbuffer2.release();
            }

        } else if ("MC|TrSel".equals(packetIn.getChannelName())) {
            try {
                int i = packetIn.getBufferData().readInt();
                Container container = this.playerEntity.openContainer;

                if (container instanceof ContainerMerchant) {
                    ((ContainerMerchant) container).setCurrentRecipeIndex(i);
                }
            } catch (Exception exception2) {
                logger.error("Couldn't select trade", exception2);
            }
        } else if ("MC|AdvCdm".equals(packetIn.getChannelName())) {
            if (!this.serverController.isCommandBlockEnabled()) {
                this.playerEntity.addChatMessage(new ChatComponentTranslation("advMode.notEnabled"));
            } else if (this.playerEntity.canCommandSenderUseCommand(2, "") && this.playerEntity.capabilities.isCreativeMode) {
                PacketBuffer packetbuffer = packetIn.getBufferData();

                try {
                    int j = packetbuffer.readByte();
                    CommandBlockLogic commandblocklogic = null;

                    if (j == 0) {
                        TileEntity tileentity = this.playerEntity.worldObj.getTileEntity(new BlockPos(packetbuffer.readInt(), packetbuffer.readInt(), packetbuffer.readInt()));

                        if (tileentity instanceof TileEntityCommandBlock) {
                            commandblocklogic = ((TileEntityCommandBlock) tileentity).getCommandBlockLogic();
                        }
                    } else if (j == 1) {
                        Entity entity = this.playerEntity.worldObj.getEntityByID(packetbuffer.readInt());

                        if (entity instanceof EntityMinecartCommandBlock) {
                            commandblocklogic = ((EntityMinecartCommandBlock) entity).getCommandBlockLogic();
                        }
                    }

                    String s1 = packetbuffer.readStringFromBuffer(packetbuffer.readableBytes());
                    boolean flag = packetbuffer.readBoolean();

                    if (commandblocklogic != null) {
                        commandblocklogic.setCommand(s1);
                        commandblocklogic.setTrackOutput(flag);

                        if (!flag) {
                            commandblocklogic.setLastOutput(null);
                        }

                        commandblocklogic.updateCommand();
                        this.playerEntity.addChatMessage(new ChatComponentTranslation("advMode.setCommand.success", s1));
                    }
                } catch (Exception exception1) {
                    logger.error("Couldn't set command block", exception1);
                } finally {
                    packetbuffer.release();
                }
            } else {
                this.playerEntity.addChatMessage(new ChatComponentTranslation("advMode.notAllowed"));
            }
        } else if ("MC|Beacon".equals(packetIn.getChannelName())) {
            if (this.playerEntity.openContainer instanceof ContainerBeacon containerbeacon) {
                try {
                    PacketBuffer packetbuffer1 = packetIn.getBufferData();
                    int k = packetbuffer1.readInt();
                    int l = packetbuffer1.readInt();
                    Slot slot = containerbeacon.getSlot(0);

                    if (slot.getHasStack()) {
                        slot.decrStackSize(1);
                        IInventory iinventory = containerbeacon.func_180611_e();
                        iinventory.setField(1, k);
                        iinventory.setField(2, l);
                        iinventory.markDirty();
                    }
                } catch (Exception exception) {
                    logger.error("Couldn't set beacon", exception);
                }
            }
        } else if ("MC|ItemName".equals(packetIn.getChannelName()) && this.playerEntity.openContainer instanceof ContainerRepair containerrepair) {

            if (packetIn.getBufferData() != null && packetIn.getBufferData().readableBytes() >= 1) {
                String s = ChatAllowedCharacters.filterAllowedCharacters(packetIn.getBufferData().readStringFromBuffer(32767));

                if (s.length() <= 30) {
                    containerrepair.updateItemName(s);
                }
            } else {
                containerrepair.updateItemName("");
            }
        }
    }
}
