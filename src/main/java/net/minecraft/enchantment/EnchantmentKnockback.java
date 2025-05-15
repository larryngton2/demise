package net.minecraft.enchantment;

import net.minecraft.util.ResourceLocation;

public class EnchantmentKnockback extends Enchantment {
    protected EnchantmentKnockback(ResourceLocation p_i45768_2_) {
        super(19, p_i45768_2_, 5, EnumEnchantmentType.WEAPON);
        this.setName("knockback");
    }

    public int getMinEnchantability(int enchantmentLevel) {
        return 5 + 20 * (enchantmentLevel - 1);
    }

    public int getMaxEnchantability(int enchantmentLevel) {
        return super.getMinEnchantability(enchantmentLevel) + 50;
    }

    public int getMaxLevel() {
        return 2;
    }
}
