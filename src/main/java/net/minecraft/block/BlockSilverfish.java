package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IStringSerializable;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class BlockSilverfish extends Block {
    public static final PropertyEnum<BlockSilverfish.EnumType> VARIANT = PropertyEnum.create("variant", BlockSilverfish.EnumType.class);

    public BlockSilverfish() {
        super(Material.clay);
        this.setDefaultState(this.blockState.getBaseState().withProperty(VARIANT, BlockSilverfish.EnumType.STONE));
        this.setHardness(0.0F);
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }

    public int quantityDropped(Random random) {
        return 0;
    }

    public static boolean canContainSilverfish(IBlockState blockState) {
        Block block = blockState.getBlock();
        return blockState == Blocks.stone.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.STONE) || block == Blocks.cobblestone || block == Blocks.stonebrick;
    }

    protected ItemStack createStackedBlock(IBlockState state) {
        return switch (state.getValue(VARIANT)) {
            case COBBLESTONE -> new ItemStack(Blocks.cobblestone);
            case STONEBRICK -> new ItemStack(Blocks.stonebrick);
            case MOSSY_STONEBRICK -> new ItemStack(Blocks.stonebrick, 1, BlockStoneBrick.EnumType.MOSSY.getMetadata());
            case CRACKED_STONEBRICK ->
                    new ItemStack(Blocks.stonebrick, 1, BlockStoneBrick.EnumType.CRACKED.getMetadata());
            case CHISELED_STONEBRICK ->
                    new ItemStack(Blocks.stonebrick, 1, BlockStoneBrick.EnumType.CHISELED.getMetadata());
            default -> new ItemStack(Blocks.stone);
        };
    }

    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!worldIn.isRemote && worldIn.getGameRules().getBoolean("doTileDrops")) {
            EntitySilverfish entitysilverfish = new EntitySilverfish(worldIn);
            entitysilverfish.setLocationAndAngles((double) pos.getX() + 0.5D, pos.getY(), (double) pos.getZ() + 0.5D, 0.0F, 0.0F);
            worldIn.spawnEntityInWorld(entitysilverfish);
            entitysilverfish.spawnExplosionParticle();
        }
    }

    public int getDamageValue(World worldIn, BlockPos pos) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        return iblockstate.getBlock().getMetaFromState(iblockstate);
    }

    public void getSubBlocks(Item itemIn, CreativeTabs tab, List<ItemStack> list) {
        for (BlockSilverfish.EnumType blocksilverfish$enumtype : BlockSilverfish.EnumType.values()) {
            list.add(new ItemStack(itemIn, 1, blocksilverfish$enumtype.getMetadata()));
        }
    }

    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(VARIANT, BlockSilverfish.EnumType.byMetadata(meta));
    }

    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).getMetadata();
    }

    protected BlockState createBlockState() {
        return new BlockState(this, VARIANT);
    }

    public enum EnumType implements IStringSerializable {
        STONE() {
            public IBlockState getModelBlock() {
                return Blocks.stone.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.STONE);
            }
        },
        COBBLESTONE(1, "cobblestone", "cobble") {
            public IBlockState getModelBlock() {
                return Blocks.cobblestone.getDefaultState();
            }
        },
        STONEBRICK(2, "stone_brick", "brick") {
            public IBlockState getModelBlock() {
                return Blocks.stonebrick.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.DEFAULT);
            }
        },
        MOSSY_STONEBRICK(3, "mossy_brick", "mossybrick") {
            public IBlockState getModelBlock() {
                return Blocks.stonebrick.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.MOSSY);
            }
        },
        CRACKED_STONEBRICK(4, "cracked_brick", "crackedbrick") {
            public IBlockState getModelBlock() {
                return Blocks.stonebrick.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
            }
        },
        CHISELED_STONEBRICK(5, "chiseled_brick", "chiseledbrick") {
            public IBlockState getModelBlock() {
                return Blocks.stonebrick.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED);
            }
        };

        private static final BlockSilverfish.EnumType[] META_LOOKUP = new BlockSilverfish.EnumType[values().length];
        private final int meta;
        private final String name;
        private final String unlocalizedName;

        EnumType() {
            this(0, "stone", "stone");
        }

        EnumType(int meta, String name, String unlocalizedName) {
            this.meta = meta;
            this.name = name;
            this.unlocalizedName = unlocalizedName;
        }

        public int getMetadata() {
            return this.meta;
        }

        public String toString() {
            return this.name;
        }

        public static BlockSilverfish.EnumType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        public String getName() {
            return this.name;
        }

        public String getUnlocalizedName() {
            return this.unlocalizedName;
        }

        public abstract IBlockState getModelBlock();

        public static BlockSilverfish.EnumType forModelBlock(IBlockState model) {
            for (BlockSilverfish.EnumType blocksilverfish$enumtype : values()) {
                if (model == blocksilverfish$enumtype.getModelBlock()) {
                    return blocksilverfish$enumtype;
                }
            }

            return STONE;
        }

        static {
            for (BlockSilverfish.EnumType blocksilverfish$enumtype : values()) {
                META_LOOKUP[blocksilverfish$enumtype.getMetadata()] = blocksilverfish$enumtype;
            }
        }
    }
}
