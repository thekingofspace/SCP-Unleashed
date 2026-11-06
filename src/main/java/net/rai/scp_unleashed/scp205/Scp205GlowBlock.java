package net.rai.scp_unleashed.scp205;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp205GlowBlock extends Block {
    public static final MapCodec<Scp205GlowBlock> BlockCodec = simpleCodec(Scp205GlowBlock::new);
    public static final IntegerProperty Brightness = BlockStateProperties.LEVEL;
    public static final int SearchRadius = 14;

    public Scp205GlowBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Brightness, 15));
    }

    public static int emission(BlockState State) {
        return State.getValue(Brightness);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Brightness);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader World, BlockPos Pos, BlockState State) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (!claimed(World, Pos)) {
            World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static boolean claimed(ServerLevel World, BlockPos Pos) {
        int MinX = SectionPos.blockToSectionCoord(Pos.getX() - SearchRadius);
        int MaxX = SectionPos.blockToSectionCoord(Pos.getX() + SearchRadius);
        int MinZ = SectionPos.blockToSectionCoord(Pos.getZ() - SearchRadius);
        int MaxZ = SectionPos.blockToSectionCoord(Pos.getZ() + SearchRadius);
        for (int ChunkX = MinX; ChunkX <= MaxX; ChunkX++) {
            for (int ChunkZ = MinZ; ChunkZ <= MaxZ; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    return true;
                }
                for (BlockEntity Entity : Chunk.getBlockEntities().values()) {
                    if (Entity instanceof Scp205LampBlockEntity Lamp && Lamp.claims(Pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
