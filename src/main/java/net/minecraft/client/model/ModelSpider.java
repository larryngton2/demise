package net.minecraft.client.model;

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public class ModelSpider extends ModelBase {
    public final ModelRenderer spiderHead;
    public final ModelRenderer spiderNeck;
    public final ModelRenderer spiderBody;
    public final ModelRenderer spiderLeg1;
    public final ModelRenderer spiderLeg2;
    public final ModelRenderer spiderLeg3;
    public final ModelRenderer spiderLeg4;
    public final ModelRenderer spiderLeg5;
    public final ModelRenderer spiderLeg6;
    public final ModelRenderer spiderLeg7;
    public final ModelRenderer spiderLeg8;

    public ModelSpider() {
        float f = 0.0F;
        int i = 15;
        this.spiderHead = new ModelRenderer(this, 32, 4);
        this.spiderHead.addBox(-4.0F, -4.0F, -8.0F, 8, 8, 8, f);
        this.spiderHead.setRotationPoint(0.0F, (float) i, -3.0F);
        this.spiderNeck = new ModelRenderer(this, 0, 0);
        this.spiderNeck.addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6, f);
        this.spiderNeck.setRotationPoint(0.0F, (float) i, 0.0F);
        this.spiderBody = new ModelRenderer(this, 0, 12);
        this.spiderBody.addBox(-5.0F, -4.0F, -6.0F, 10, 8, 12, f);
        this.spiderBody.setRotationPoint(0.0F, (float) i, 9.0F);
        this.spiderLeg1 = new ModelRenderer(this, 18, 0);
        this.spiderLeg1.addBox(-15.0F, -1.0F, -1.0F, 16, 2, 2, f);
        this.spiderLeg1.setRotationPoint(-4.0F, (float) i, 2.0F);
        this.spiderLeg2 = new ModelRenderer(this, 18, 0);
        this.spiderLeg2.addBox(-1.0F, -1.0F, -1.0F, 16, 2, 2, f);
        this.spiderLeg2.setRotationPoint(4.0F, (float) i, 2.0F);
        this.spiderLeg3 = new ModelRenderer(this, 18, 0);
        this.spiderLeg3.addBox(-15.0F, -1.0F, -1.0F, 16, 2, 2, f);
        this.spiderLeg3.setRotationPoint(-4.0F, (float) i, 1.0F);
        this.spiderLeg4 = new ModelRenderer(this, 18, 0);
        this.spiderLeg4.addBox(-1.0F, -1.0F, -1.0F, 16, 2, 2, f);
        this.spiderLeg4.setRotationPoint(4.0F, (float) i, 1.0F);
        this.spiderLeg5 = new ModelRenderer(this, 18, 0);
        this.spiderLeg5.addBox(-15.0F, -1.0F, -1.0F, 16, 2, 2, f);
        this.spiderLeg5.setRotationPoint(-4.0F, (float) i, 0.0F);
        this.spiderLeg6 = new ModelRenderer(this, 18, 0);
        this.spiderLeg6.addBox(-1.0F, -1.0F, -1.0F, 16, 2, 2, f);
        this.spiderLeg6.setRotationPoint(4.0F, (float) i, 0.0F);
        this.spiderLeg7 = new ModelRenderer(this, 18, 0);
        this.spiderLeg7.addBox(-15.0F, -1.0F, -1.0F, 16, 2, 2, f);
        this.spiderLeg7.setRotationPoint(-4.0F, (float) i, -1.0F);
        this.spiderLeg8 = new ModelRenderer(this, 18, 0);
        this.spiderLeg8.addBox(-1.0F, -1.0F, -1.0F, 16, 2, 2, f);
        this.spiderLeg8.setRotationPoint(4.0F, (float) i, -1.0F);
    }

    public void render(Entity entityIn, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float scale) {
        this.setRotationAngles(p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, scale, entityIn);
        this.spiderHead.render(scale);
        this.spiderNeck.render(scale);
        this.spiderBody.render(scale);
        this.spiderLeg1.render(scale);
        this.spiderLeg2.render(scale);
        this.spiderLeg3.render(scale);
        this.spiderLeg4.render(scale);
        this.spiderLeg5.render(scale);
        this.spiderLeg6.render(scale);
        this.spiderLeg7.render(scale);
        this.spiderLeg8.render(scale);
    }

    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        this.spiderHead.rotateAngleY = netHeadYaw / (180F / (float) Math.PI);
        this.spiderHead.rotateAngleX = headPitch / (180F / (float) Math.PI);
        float f = ((float) Math.PI / 4F);
        this.spiderLeg1.rotateAngleZ = -f;
        this.spiderLeg2.rotateAngleZ = f;
        this.spiderLeg3.rotateAngleZ = -f * 0.74F;
        this.spiderLeg4.rotateAngleZ = f * 0.74F;
        this.spiderLeg5.rotateAngleZ = -f * 0.74F;
        this.spiderLeg6.rotateAngleZ = f * 0.74F;
        this.spiderLeg7.rotateAngleZ = -f;
        this.spiderLeg8.rotateAngleZ = f;
        float f1 = -0.0F;
        float f2 = 0.3926991F;
        this.spiderLeg1.rotateAngleY = f2 * 2.0F + f1;
        this.spiderLeg2.rotateAngleY = -f2 * 2.0F - f1;
        this.spiderLeg3.rotateAngleY = f2 + f1;
        this.spiderLeg4.rotateAngleY = -f2 - f1;
        this.spiderLeg5.rotateAngleY = -f2 + f1;
        this.spiderLeg6.rotateAngleY = f2 - f1;
        this.spiderLeg7.rotateAngleY = -f2 * 2.0F + f1;
        this.spiderLeg8.rotateAngleY = f2 * 2.0F - f1;
        float f3 = -(MathHelper.cos(limbSwing * 0.6662F * 2.0F + 0.0F) * 0.4F) * limbSwingAmount;
        float f4 = -(MathHelper.cos(limbSwing * 0.6662F * 2.0F + (float) Math.PI) * 0.4F) * limbSwingAmount;
        float f5 = -(MathHelper.cos(limbSwing * 0.6662F * 2.0F + ((float) Math.PI / 2F)) * 0.4F) * limbSwingAmount;
        float f6 = -(MathHelper.cos(limbSwing * 0.6662F * 2.0F + ((float) Math.PI * 3F / 2F)) * 0.4F) * limbSwingAmount;
        float f7 = Math.abs(MathHelper.sin(limbSwing * 0.6662F + 0.0F) * 0.4F) * limbSwingAmount;
        float f8 = Math.abs(MathHelper.sin(limbSwing * 0.6662F + (float) Math.PI) * 0.4F) * limbSwingAmount;
        float f9 = Math.abs(MathHelper.sin(limbSwing * 0.6662F + ((float) Math.PI / 2F)) * 0.4F) * limbSwingAmount;
        float f10 = Math.abs(MathHelper.sin(limbSwing * 0.6662F + ((float) Math.PI * 3F / 2F)) * 0.4F) * limbSwingAmount;
        this.spiderLeg1.rotateAngleY += f3;
        this.spiderLeg2.rotateAngleY -= f3;
        this.spiderLeg3.rotateAngleY += f4;
        this.spiderLeg4.rotateAngleY -= f4;
        this.spiderLeg5.rotateAngleY += f5;
        this.spiderLeg6.rotateAngleY -= f5;
        this.spiderLeg7.rotateAngleY += f6;
        this.spiderLeg8.rotateAngleY -= f6;
        this.spiderLeg1.rotateAngleZ += f7;
        this.spiderLeg2.rotateAngleZ -= f7;
        this.spiderLeg3.rotateAngleZ += f8;
        this.spiderLeg4.rotateAngleZ -= f8;
        this.spiderLeg5.rotateAngleZ += f9;
        this.spiderLeg6.rotateAngleZ -= f9;
        this.spiderLeg7.rotateAngleZ += f10;
        this.spiderLeg8.rotateAngleZ -= f10;
    }
}
