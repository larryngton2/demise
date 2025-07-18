package net.minecraft.client.model;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityIronGolem;

public class ModelIronGolem extends ModelBase {
    public final ModelRenderer ironGolemHead;
    public final ModelRenderer ironGolemBody;
    public final ModelRenderer ironGolemRightArm;
    public final ModelRenderer ironGolemLeftArm;
    public final ModelRenderer ironGolemLeftLeg;
    public final ModelRenderer ironGolemRightLeg;

    public ModelIronGolem() {
        this(0.0F);
    }

    public ModelIronGolem(float p_i1161_1_) {
        this(p_i1161_1_, -7.0F);
    }

    public ModelIronGolem(float p_i46362_1_, float p_i46362_2_) {
        int i = 128;
        int j = 128;
        this.ironGolemHead = (new ModelRenderer(this)).setTextureSize(i, j);
        this.ironGolemHead.setRotationPoint(0.0F, 0.0F + p_i46362_2_, -2.0F);
        this.ironGolemHead.setTextureOffset(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8, 10, 8, p_i46362_1_);
        this.ironGolemHead.setTextureOffset(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2, 4, 2, p_i46362_1_);
        this.ironGolemBody = (new ModelRenderer(this)).setTextureSize(i, j);
        this.ironGolemBody.setRotationPoint(0.0F, 0.0F + p_i46362_2_, 0.0F);
        this.ironGolemBody.setTextureOffset(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18, 12, 11, p_i46362_1_);
        this.ironGolemBody.setTextureOffset(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9, 5, 6, p_i46362_1_ + 0.5F);
        this.ironGolemRightArm = (new ModelRenderer(this)).setTextureSize(i, j);
        this.ironGolemRightArm.setRotationPoint(0.0F, -7.0F, 0.0F);
        this.ironGolemRightArm.setTextureOffset(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4, 30, 6, p_i46362_1_);
        this.ironGolemLeftArm = (new ModelRenderer(this)).setTextureSize(i, j);
        this.ironGolemLeftArm.setRotationPoint(0.0F, -7.0F, 0.0F);
        this.ironGolemLeftArm.setTextureOffset(60, 58).addBox(9.0F, -2.5F, -3.0F, 4, 30, 6, p_i46362_1_);
        this.ironGolemLeftLeg = (new ModelRenderer(this, 0, 22)).setTextureSize(i, j);
        this.ironGolemLeftLeg.setRotationPoint(-4.0F, 18.0F + p_i46362_2_, 0.0F);
        this.ironGolemLeftLeg.setTextureOffset(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6, 16, 5, p_i46362_1_);
        this.ironGolemRightLeg = (new ModelRenderer(this, 0, 22)).setTextureSize(i, j);
        this.ironGolemRightLeg.mirror = true;
        this.ironGolemRightLeg.setTextureOffset(60, 0).setRotationPoint(5.0F, 18.0F + p_i46362_2_, 0.0F);
        this.ironGolemRightLeg.addBox(-3.5F, -3.0F, -3.0F, 6, 16, 5, p_i46362_1_);
    }

    public void render(Entity entityIn, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float scale) {
        this.setRotationAngles(p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, scale, entityIn);
        this.ironGolemHead.render(scale);
        this.ironGolemBody.render(scale);
        this.ironGolemLeftLeg.render(scale);
        this.ironGolemRightLeg.render(scale);
        this.ironGolemRightArm.render(scale);
        this.ironGolemLeftArm.render(scale);
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        this.ironGolemHead.rotateAngleY = netHeadYaw / (180F / (float) Math.PI);
        this.ironGolemHead.rotateAngleX = headPitch / (180F / (float) Math.PI);
        this.ironGolemLeftLeg.rotateAngleX = -1.5F * this.func_78172_a(limbSwing, 13.0F) * limbSwingAmount;
        this.ironGolemRightLeg.rotateAngleX = 1.5F * this.func_78172_a(limbSwing, 13.0F) * limbSwingAmount;
        this.ironGolemLeftLeg.rotateAngleY = 0.0F;
        this.ironGolemRightLeg.rotateAngleY = 0.0F;
    }

    public void setLivingAnimations(EntityLivingBase entitylivingbaseIn, float p_78086_2_, float p_78086_3_, float partialTickTime) {
        EntityIronGolem entityirongolem = (EntityIronGolem) entitylivingbaseIn;
        int i = entityirongolem.getAttackTimer();

        if (i > 0) {
            this.ironGolemRightArm.rotateAngleX = -2.0F + 1.5F * this.func_78172_a((float) i - partialTickTime, 10.0F);
            this.ironGolemLeftArm.rotateAngleX = -2.0F + 1.5F * this.func_78172_a((float) i - partialTickTime, 10.0F);
        } else {
            int j = entityirongolem.getHoldRoseTick();

            if (j > 0) {
                this.ironGolemRightArm.rotateAngleX = -0.8F + 0.025F * this.func_78172_a((float) j, 70.0F);
                this.ironGolemLeftArm.rotateAngleX = 0.0F;
            } else {
                this.ironGolemRightArm.rotateAngleX = (-0.2F + 1.5F * this.func_78172_a(p_78086_2_, 13.0F)) * p_78086_3_;
                this.ironGolemLeftArm.rotateAngleX = (-0.2F - 1.5F * this.func_78172_a(p_78086_2_, 13.0F)) * p_78086_3_;
            }
        }
    }

    private float func_78172_a(float p_78172_1_, float p_78172_2_) {
        return (Math.abs(p_78172_1_ % p_78172_2_ - p_78172_2_ * 0.5F) - p_78172_2_ * 0.25F) / (p_78172_2_ * 0.25F);
    }
}
