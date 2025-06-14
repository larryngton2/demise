package net.minecraft.world.biome;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.*;

import java.util.Random;

public class BiomeGenJungle extends BiomeGenBase {
    private final boolean field_150614_aC;
    private static final IBlockState field_181620_aE = Blocks.log.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE);
    private static final IBlockState field_181621_aF = Blocks.leaves.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE).withProperty(BlockLeaves.CHECK_DECAY, Boolean.FALSE);
    private static final IBlockState field_181622_aG = Blocks.leaves.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.OAK).withProperty(BlockLeaves.CHECK_DECAY, Boolean.FALSE);

    public BiomeGenJungle(int id, boolean p_i45379_2_) {
        super(id);
        this.field_150614_aC = p_i45379_2_;

        if (p_i45379_2_) {
            this.theBiomeDecorator.treesPerChunk = 2;
        } else {
            this.theBiomeDecorator.treesPerChunk = 50;
        }

        this.theBiomeDecorator.grassPerChunk = 25;
        this.theBiomeDecorator.flowersPerChunk = 4;

        if (!p_i45379_2_) {
            this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntityOcelot.class, 2, 1, 1));
        }

        this.spawnableCreatureList.add(new BiomeGenBase.SpawnListEntry(EntityChicken.class, 10, 4, 4));
    }

    public WorldGenAbstractTree genBigTreeChance(Random rand) {
        return rand.nextInt(10) == 0 ? this.worldGeneratorBigTree : (rand.nextInt(2) == 0 ? new WorldGenShrub(field_181620_aE, field_181622_aG) : (!this.field_150614_aC && rand.nextInt(3) == 0 ? new WorldGenMegaJungle(false, 10, 20, field_181620_aE, field_181621_aF) : new WorldGenTrees(false, 4 + rand.nextInt(7), field_181620_aE, field_181621_aF, true)));
    }

    public WorldGenerator getRandomWorldGenForGrass(Random rand) {
        return rand.nextInt(4) == 0 ? new WorldGenTallGrass(BlockTallGrass.EnumType.FERN) : new WorldGenTallGrass(BlockTallGrass.EnumType.GRASS);
    }

    public void decorate(World worldIn, Random rand, BlockPos pos) {
        super.decorate(worldIn, rand, pos);
        int i = rand.nextInt(16) + 8;
        int j = rand.nextInt(16) + 8;
        int k = rand.nextInt(worldIn.getHeight(pos.add(i, 0, j)).getY() * 2);
        (new WorldGenMelon()).generate(worldIn, rand, pos.add(i, k, j));
        WorldGenVines worldgenvines = new WorldGenVines();

        for (j = 0; j < 50; ++j) {
            k = rand.nextInt(16) + 8;
            int l = 128;
            int i1 = rand.nextInt(16) + 8;
            worldgenvines.generate(worldIn, rand, pos.add(k, 128, i1));
        }
    }
}
