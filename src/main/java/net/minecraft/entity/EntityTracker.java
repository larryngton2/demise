package net.minecraft.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.*;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.*;
import net.minecraft.network.Packet;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;

public class EntityTracker {
    private static final Logger logger = LogManager.getLogger();
    private final WorldServer theWorld;
    private final Set<EntityTrackerEntry> trackedEntities = Sets.newHashSet();
    private final IntHashMap<EntityTrackerEntry> trackedEntityHashTable = new IntHashMap();
    private final int maxTrackingDistanceThreshold;

    public EntityTracker(WorldServer theWorldIn) {
        this.theWorld = theWorldIn;
        this.maxTrackingDistanceThreshold = theWorldIn.getMinecraftServer().getConfigurationManager().getEntityViewDistance();
    }

    public void trackEntity(Entity entityIn) {
        if (entityIn instanceof EntityPlayerMP entityplayermp) {
            this.trackEntity(entityIn, 512, 2);

            for (EntityTrackerEntry entitytrackerentry : this.trackedEntities) {
                if (entitytrackerentry.trackedEntity != entityplayermp) {
                    entitytrackerentry.updatePlayerEntity(entityplayermp);
                }
            }
        } else if (entityIn instanceof EntityFishHook) {
            this.addEntityToTracker(entityIn, 64, 5, true);
        } else if (entityIn instanceof EntityArrow) {
            this.addEntityToTracker(entityIn, 64, 20, false);
        } else if (entityIn instanceof EntitySmallFireball) {
            this.addEntityToTracker(entityIn, 64, 10, false);
        } else if (entityIn instanceof EntityFireball) {
            this.addEntityToTracker(entityIn, 64, 10, false);
        } else if (entityIn instanceof EntitySnowball) {
            this.addEntityToTracker(entityIn, 64, 10, true);
        } else if (entityIn instanceof EntityEnderPearl) {
            this.addEntityToTracker(entityIn, 64, 10, true);
        } else if (entityIn instanceof EntityEnderEye) {
            this.addEntityToTracker(entityIn, 64, 4, true);
        } else if (entityIn instanceof EntityEgg) {
            this.addEntityToTracker(entityIn, 64, 10, true);
        } else if (entityIn instanceof EntityPotion) {
            this.addEntityToTracker(entityIn, 64, 10, true);
        } else if (entityIn instanceof EntityExpBottle) {
            this.addEntityToTracker(entityIn, 64, 10, true);
        } else if (entityIn instanceof EntityFireworkRocket) {
            this.addEntityToTracker(entityIn, 64, 10, true);
        } else if (entityIn instanceof EntityItem) {
            this.addEntityToTracker(entityIn, 64, 20, true);
        } else if (entityIn instanceof EntityMinecart) {
            this.addEntityToTracker(entityIn, 80, 3, true);
        } else if (entityIn instanceof EntityBoat) {
            this.addEntityToTracker(entityIn, 80, 3, true);
        } else if (entityIn instanceof EntitySquid) {
            this.addEntityToTracker(entityIn, 64, 3, true);
        } else if (entityIn instanceof EntityWither) {
            this.addEntityToTracker(entityIn, 80, 3, false);
        } else if (entityIn instanceof EntityBat) {
            this.addEntityToTracker(entityIn, 80, 3, false);
        } else if (entityIn instanceof EntityDragon) {
            this.addEntityToTracker(entityIn, 160, 3, true);
        } else if (entityIn instanceof IAnimals) {
            this.addEntityToTracker(entityIn, 80, 3, true);
        } else if (entityIn instanceof EntityTNTPrimed) {
            this.addEntityToTracker(entityIn, 160, 10, true);
        } else if (entityIn instanceof EntityFallingBlock) {
            this.addEntityToTracker(entityIn, 160, 20, true);
        } else if (entityIn instanceof EntityHanging) {
            this.addEntityToTracker(entityIn, 160, Integer.MAX_VALUE, false);
        } else if (entityIn instanceof EntityArmorStand) {
            this.addEntityToTracker(entityIn, 160, 3, true);
        } else if (entityIn instanceof EntityXPOrb) {
            this.addEntityToTracker(entityIn, 160, 20, true);
        } else if (entityIn instanceof EntityEnderCrystal) {
            this.addEntityToTracker(entityIn, 256, Integer.MAX_VALUE, false);
        }
    }

    public void trackEntity(Entity entityIn, int trackingRange, int updateFrequency) {
        this.addEntityToTracker(entityIn, trackingRange, updateFrequency, false);
    }

    public void addEntityToTracker(Entity entityIn, int trackingRange, final int updateFrequency, boolean sendVelocityUpdates) {
        if (trackingRange > this.maxTrackingDistanceThreshold) {
            trackingRange = this.maxTrackingDistanceThreshold;
        }

        try {
            if (this.trackedEntityHashTable.containsItem(entityIn.getEntityId())) {
                throw new IllegalStateException("Entity is already tracked!");
            }

            EntityTrackerEntry entitytrackerentry = new EntityTrackerEntry(entityIn, trackingRange, updateFrequency, sendVelocityUpdates);
            this.trackedEntities.add(entitytrackerentry);
            this.trackedEntityHashTable.addKey(entityIn.getEntityId(), entitytrackerentry);
            entitytrackerentry.updatePlayerEntities(this.theWorld.playerEntities);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Adding entity to track");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity To Track");
            crashreportcategory.addCrashSection("Tracking range", trackingRange + " blocks");
            crashreportcategory.addCrashSectionCallable("Update interval", () -> {
                String s = "Once per " + updateFrequency + " ticks";

                if (updateFrequency == Integer.MAX_VALUE) {
                    s = "Maximum (" + s + ")";
                }

                return s;
            });
            entityIn.addEntityCrashInfo(crashreportcategory);
            CrashReportCategory crashreportcategory1 = crashreport.makeCategory("Entity That Is Already Tracked");
            this.trackedEntityHashTable.lookup(entityIn.getEntityId()).trackedEntity.addEntityCrashInfo(crashreportcategory1);

            try {
                throw new ReportedException(crashreport);
            } catch (ReportedException reportedexception) {
                logger.error("\"Silently\" catching entity tracking error.", reportedexception);
            }
        }
    }

    public void untrackEntity(Entity entityIn) {
        if (entityIn instanceof EntityPlayerMP entityplayermp) {

            for (EntityTrackerEntry entitytrackerentry : this.trackedEntities) {
                entitytrackerentry.removeFromTrackedPlayers(entityplayermp);
            }
        }

        EntityTrackerEntry entitytrackerentry1 = this.trackedEntityHashTable.removeObject(entityIn.getEntityId());

        if (entitytrackerentry1 != null) {
            this.trackedEntities.remove(entitytrackerentry1);
            entitytrackerentry1.sendDestroyEntityPacketToTrackedPlayers();
        }
    }

    public void updateTrackedEntities() {
        List<EntityPlayerMP> list = Lists.newArrayList();

        for (EntityTrackerEntry entitytrackerentry : this.trackedEntities) {
            entitytrackerentry.updatePlayerList(this.theWorld.playerEntities);

            if (entitytrackerentry.playerEntitiesUpdated && entitytrackerentry.trackedEntity instanceof EntityPlayerMP) {
                list.add((EntityPlayerMP) entitytrackerentry.trackedEntity);
            }
        }

        for (EntityPlayerMP entityplayermp : list) {
            for (EntityTrackerEntry entitytrackerentry1 : this.trackedEntities) {
                if (entitytrackerentry1.trackedEntity != entityplayermp) {
                    entitytrackerentry1.updatePlayerEntity(entityplayermp);
                }
            }
        }
    }

    public void func_180245_a(EntityPlayerMP p_180245_1_) {
        for (EntityTrackerEntry entitytrackerentry : this.trackedEntities) {
            if (entitytrackerentry.trackedEntity == p_180245_1_) {
                entitytrackerentry.updatePlayerEntities(this.theWorld.playerEntities);
            } else {
                entitytrackerentry.updatePlayerEntity(p_180245_1_);
            }
        }
    }

    public void sendToAllTrackingEntity(Entity entityIn, Packet p_151247_2_) {
        EntityTrackerEntry entitytrackerentry = this.trackedEntityHashTable.lookup(entityIn.getEntityId());

        if (entitytrackerentry != null) {
            entitytrackerentry.sendPacketToTrackedPlayers(p_151247_2_);
        }
    }

    public void func_151248_b(Entity entityIn, Packet p_151248_2_) {
        EntityTrackerEntry entitytrackerentry = this.trackedEntityHashTable.lookup(entityIn.getEntityId());

        if (entitytrackerentry != null) {
            entitytrackerentry.func_151261_b(p_151248_2_);
        }
    }

    public void removePlayerFromTrackers(EntityPlayerMP p_72787_1_) {
        for (EntityTrackerEntry entitytrackerentry : this.trackedEntities) {
            entitytrackerentry.removeTrackedPlayerSymmetric(p_72787_1_);
        }
    }

    public void func_85172_a(EntityPlayerMP p_85172_1_, Chunk p_85172_2_) {
        for (EntityTrackerEntry entitytrackerentry : this.trackedEntities) {
            if (entitytrackerentry.trackedEntity != p_85172_1_ && entitytrackerentry.trackedEntity.chunkCoordX == p_85172_2_.xPosition && entitytrackerentry.trackedEntity.chunkCoordZ == p_85172_2_.zPosition) {
                entitytrackerentry.updatePlayerEntity(p_85172_1_);
            }
        }
    }
}
