package net.minecraft.enchantment;

import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Random;

public class EnchantmentDurability extends Enchantment {
    protected EnchantmentDurability(ResourceLocation enchName) {
        super(34, enchName, 5, EnumEnchantmentType.BREAKABLE);
        this.setName("durability");
    }

    public int getMinEnchantability(int enchantmentLevel) {
        return 5 + (enchantmentLevel - 1) * 8;
    }

    public int getMaxEnchantability(int enchantmentLevel) {
        return super.getMinEnchantability(enchantmentLevel) + 50;
    }

    public int getMaxLevel() {
        return 3;
    }

    public boolean canApply(ItemStack stack) {
        return stack.isItemStackDamageable() || super.canApply(stack);
    }

    public static boolean negateDamage(ItemStack p_92097_0_, int p_92097_1_, Random p_92097_2_) {
        return (!(p_92097_0_.getItem() instanceof ItemArmor) || !(p_92097_2_.nextFloat() < 0.6F)) && p_92097_2_.nextInt(p_92097_1_ + 1) > 0;
    }
}
