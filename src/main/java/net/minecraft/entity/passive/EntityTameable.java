package net.minecraft.entity.passive;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.ai.EntityAISit;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.management.PreYggdrasilConverter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class EntityTameable extends EntityAnimal implements IEntityOwnable {
    protected final EntityAISit aiSit = new EntityAISit(this);

    public EntityTameable(World worldIn) {
        super(worldIn);
        this.setupTamedAI();
    }

    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(16, (byte) 0);
        this.dataWatcher.addObject(17, "");
    }

    public void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);

        if (this.getOwnerId() == null) {
            tagCompound.setString("OwnerUUID", "");
        } else {
            tagCompound.setString("OwnerUUID", this.getOwnerId());
        }

        tagCompound.setBoolean("Sitting", this.isSitting());
    }

    public void readEntityFromNBT(NBTTagCompound tagCompund) {
        super.readEntityFromNBT(tagCompund);
        String s = "";

        if (tagCompund.hasKey("OwnerUUID", 8)) {
            s = tagCompund.getString("OwnerUUID");
        } else {
            String s1 = tagCompund.getString("Owner");
            s = PreYggdrasilConverter.getStringUUIDFromName(s1);
        }

        if (!s.isEmpty()) {
            this.setOwnerId(s);
            this.setTamed(true);
        }

        this.aiSit.setSitting(tagCompund.getBoolean("Sitting"));
        this.setSitting(tagCompund.getBoolean("Sitting"));
    }

    protected void playTameEffect(boolean play) {
        EnumParticleTypes enumparticletypes = EnumParticleTypes.HEART;

        if (!play) {
            enumparticletypes = EnumParticleTypes.SMOKE_NORMAL;
        }

        for (int i = 0; i < 7; ++i) {
            double d0 = this.rand.nextGaussian() * 0.02D;
            double d1 = this.rand.nextGaussian() * 0.02D;
            double d2 = this.rand.nextGaussian() * 0.02D;
            this.worldObj.spawnParticle(enumparticletypes, this.posX + (double) (this.rand.nextFloat() * this.width * 2.0F) - (double) this.width, this.posY + 0.5D + (double) (this.rand.nextFloat() * this.height), this.posZ + (double) (this.rand.nextFloat() * this.width * 2.0F) - (double) this.width, d0, d1, d2);
        }
    }

    public void handleStatusUpdate(byte id) {
        if (id == 7) {
            this.playTameEffect(true);
        } else if (id == 6) {
            this.playTameEffect(false);
        } else {
            super.handleStatusUpdate(id);
        }
    }

    public boolean isTamed() {
        return (this.dataWatcher.getWatchableObjectByte(16) & 4) != 0;
    }

    public void setTamed(boolean tamed) {
        byte b0 = this.dataWatcher.getWatchableObjectByte(16);

        if (tamed) {
            this.dataWatcher.updateObject(16, (byte) (b0 | 4));
        } else {
            this.dataWatcher.updateObject(16, (byte) (b0 & -5));
        }

        this.setupTamedAI();
    }

    protected void setupTamedAI() {
    }

    public boolean isSitting() {
        return (this.dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    public void setSitting(boolean sitting) {
        byte b0 = this.dataWatcher.getWatchableObjectByte(16);

        if (sitting) {
            this.dataWatcher.updateObject(16, (byte) (b0 | 1));
        } else {
            this.dataWatcher.updateObject(16, (byte) (b0 & -2));
        }
    }

    public String getOwnerId() {
        return this.dataWatcher.getWatchableObjectString(17);
    }

    public void setOwnerId(String ownerUuid) {
        this.dataWatcher.updateObject(17, ownerUuid);
    }

    public EntityLivingBase getOwner() {
        try {
            UUID uuid = UUID.fromString(this.getOwnerId());
            return this.worldObj.getPlayerEntityByUUID(uuid);
        } catch (IllegalArgumentException var2) {
            return null;
        }
    }

    public boolean isOwner(EntityLivingBase entityIn) {
        return entityIn == this.getOwner();
    }

    public EntityAISit getAISit() {
        return this.aiSit;
    }

    public boolean shouldAttackEntity(EntityLivingBase p_142018_1_, EntityLivingBase p_142018_2_) {
        return true;
    }

    public Team getTeam() {
        if (this.isTamed()) {
            EntityLivingBase entitylivingbase = this.getOwner();

            if (entitylivingbase != null) {
                return entitylivingbase.getTeam();
            }
        }

        return super.getTeam();
    }

    public boolean isOnSameTeam(EntityLivingBase otherEntity) {
        if (this.isTamed()) {
            EntityLivingBase entitylivingbase = this.getOwner();

            if (otherEntity == entitylivingbase) {
                return true;
            }

            if (entitylivingbase != null) {
                return entitylivingbase.isOnSameTeam(otherEntity);
            }
        }

        return super.isOnSameTeam(otherEntity);
    }

    public void onDeath(DamageSource cause) {
        if (!this.worldObj.isRemote && this.worldObj.getGameRules().getBoolean("showDeathMessages") && this.hasCustomName() && this.getOwner() instanceof EntityPlayerMP) {
            this.getOwner().addChatMessage(this.getCombatTracker().getDeathMessage());
        }

        super.onDeath(cause);
    }
}
