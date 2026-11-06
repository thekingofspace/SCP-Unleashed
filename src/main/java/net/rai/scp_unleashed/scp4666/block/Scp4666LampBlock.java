package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class Scp4666LampBlock extends Block {
    public static final MapCodec<Scp4666LampBlock> BlockCodec = simpleCodec(Scp4666LampBlock::new);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    private static final VoxelShape Shape = Block.box(4.0, 3.0, 4.0, 12.0, 16.0, 12.0);

    public Scp4666LampBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Lit, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Lit);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    public static void switchAround(Level World, BlockPos Centre, boolean On, int Reach) {
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-Reach, -4, -Reach), Centre.offset(Reach, 4, Reach))) {
            BlockState Found = World.getBlockState(Pos);
            if (Found.getBlock() instanceof Scp4666LampBlock && Found.getValue(Lit) != On) {
                World.setBlock(Pos, Found.setValue(Lit, On), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Lit) && Random.nextInt(18) == 0) {
            World.addParticle(ParticleTypes.SMALL_FLAME, Pos.getX() + 0.5, Pos.getY() + 0.45, Pos.getZ() + 0.5, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_cabin_lamp.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
