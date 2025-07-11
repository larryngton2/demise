package net.minecraft.entity.ai;

import com.google.common.base.Predicate;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.Team;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class EntityAIFindEntityNearestPlayer extends EntityAIBase {
    private static final Logger LOGGER = LogManager.getLogger();
    private final EntityLiving entityLiving;
    private final Predicate<Entity> predicate;
    private final EntityAINearestAttackableTarget.Sorter sorter;
    private EntityLivingBase entityTarget;

    public EntityAIFindEntityNearestPlayer(EntityLiving entityLivingIn) {
        this.entityLiving = entityLivingIn;

        if (entityLivingIn instanceof EntityCreature) {
            LOGGER.warn("Use NearestAttackableTargetGoal.class for PathfinerMob mobs!");
        }

        this.predicate = p_apply_1_ -> {
            if (!(p_apply_1_ instanceof EntityPlayer)) {
                return false;
            } else if (((EntityPlayer) p_apply_1_).capabilities.disableDamage) {
                return false;
            } else {
                double d0 = EntityAIFindEntityNearestPlayer.this.maxTargetRange();

                if (p_apply_1_.isSneaking()) {
                    d0 *= 0.800000011920929D;
                }

                if (p_apply_1_.isInvisible()) {
                    float f = ((EntityPlayer) p_apply_1_).getArmorVisibility();

                    if (f < 0.1F) {
                        f = 0.1F;
                    }

                    d0 *= 0.7F * f;
                }

                return !((double) p_apply_1_.getDistanceToEntity(EntityAIFindEntityNearestPlayer.this.entityLiving) > d0) && EntityAITarget.isSuitableTarget(EntityAIFindEntityNearestPlayer.this.entityLiving, (EntityLivingBase) p_apply_1_, false, true);
            }
        };
        this.sorter = new EntityAINearestAttackableTarget.Sorter(entityLivingIn);
    }

    public boolean shouldExecute() {
        double d0 = this.maxTargetRange();
        List<EntityPlayer> list = this.entityLiving.worldObj.getEntitiesWithinAABB(EntityPlayer.class, this.entityLiving.getEntityBoundingBox().expand(d0, 4.0D, d0), this.predicate);
        list.sort(this.sorter);

        if (list.isEmpty()) {
            return false;
        } else {
            this.entityTarget = list.get(0);
            return true;
        }
    }

    public boolean continueExecuting() {
        EntityLivingBase entitylivingbase = this.entityLiving.getAttackTarget();

        if (entitylivingbase == null) {
            return false;
        } else if (!entitylivingbase.isEntityAlive()) {
            return false;
        } else if (entitylivingbase instanceof EntityPlayer && ((EntityPlayer) entitylivingbase).capabilities.disableDamage) {
            return false;
        } else {
            Team team = this.entityLiving.getTeam();
            Team team1 = entitylivingbase.getTeam();

            if (team != null && team1 == team) {
                return false;
            } else {
                double d0 = this.maxTargetRange();
                return !(this.entityLiving.getDistanceSqToEntity(entitylivingbase) > d0 * d0) && (!(entitylivingbase instanceof EntityPlayerMP) || !((EntityPlayerMP) entitylivingbase).theItemInWorldManager.isCreative());
            }
        }
    }

    public void startExecuting() {
        this.entityLiving.setAttackTarget(this.entityTarget);
        super.startExecuting();
    }

    public void resetTask() {
        this.entityLiving.setAttackTarget(null);
        super.startExecuting();
    }

    protected double maxTargetRange() {
        IAttributeInstance iattributeinstance = this.entityLiving.getEntityAttribute(SharedMonsterAttributes.followRange);
        return iattributeinstance == null ? 16.0D : iattributeinstance.getAttributeValue();
    }
}
