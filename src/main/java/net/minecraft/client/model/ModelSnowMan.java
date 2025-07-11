package net.minecraft.client.model;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public class ModelSnowMan extends ModelBase {
    public final ModelRenderer body;
    public final ModelRenderer bottomBody;
    public final ModelRenderer head;
    public final ModelRenderer rightHand;
    public final ModelRenderer leftHand;

    public ModelSnowMan() {
        float f = 4.0F;
        float f1 = 0.0F;
        this.head = (new ModelRenderer(this, 0, 0)).setTextureSize(64, 64);
        this.head.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, f1 - 0.5F);
        this.head.setRotationPoint(0.0F, 0.0F + f, 0.0F);
        this.rightHand = (new ModelRenderer(this, 32, 0)).setTextureSize(64, 64);
        this.rightHand.addBox(-1.0F, 0.0F, -1.0F, 12, 2, 2, f1 - 0.5F);
        this.rightHand.setRotationPoint(0.0F, 0.0F + f + 9.0F - 7.0F, 0.0F);
        this.leftHand = (new ModelRenderer(this, 32, 0)).setTextureSize(64, 64);
        this.leftHand.addBox(-1.0F, 0.0F, -1.0F, 12, 2, 2, f1 - 0.5F);
        this.leftHand.setRotationPoint(0.0F, 0.0F + f + 9.0F - 7.0F, 0.0F);
        this.body = (new ModelRenderer(this, 0, 16)).setTextureSize(64, 64);
        this.body.addBox(-5.0F, -10.0F, -5.0F, 10, 10, 10, f1 - 0.5F);
        this.body.setRotationPoint(0.0F, 0.0F + f + 9.0F, 0.0F);
        this.bottomBody = (new ModelRenderer(this, 0, 36)).setTextureSize(64, 64);
        this.bottomBody.addBox(-6.0F, -12.0F, -6.0F, 12, 12, 12, f1 - 0.5F);
        this.bottomBody.setRotationPoint(0.0F, 0.0F + f + 20.0F, 0.0F);
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        this.head.rotateAngleY = netHeadYaw / (180F / (float) Math.PI);
        this.head.rotateAngleX = headPitch / (180F / (float) Math.PI);
        this.body.rotateAngleY = netHeadYaw / (180F / (float) Math.PI) * 0.25F;
        float f = MathHelper.sin(this.body.rotateAngleY);
        float f1 = MathHelper.cos(this.body.rotateAngleY);
        this.rightHand.rotateAngleZ = 1.0F;
        this.leftHand.rotateAngleZ = -1.0F;
        this.rightHand.rotateAngleY = 0.0F + this.body.rotateAngleY;
        this.leftHand.rotateAngleY = (float) Math.PI + this.body.rotateAngleY;
        this.rightHand.rotationPointX = f1 * 5.0F;
        this.rightHand.rotationPointZ = -f * 5.0F;
        this.leftHand.rotationPointX = -f1 * 5.0F;
        this.leftHand.rotationPointZ = f * 5.0F;
    }

    public void render(Entity entityIn, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float scale) {
        this.setRotationAngles(p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, scale, entityIn);
        this.body.render(scale);
        this.bottomBody.render(scale);
        this.head.render(scale);
        this.rightHand.render(scale);
        this.leftHand.render(scale);
    }
}
