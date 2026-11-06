package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.Heightmap;

public class Scp610FleshBlock extends Block {
    public static final BooleanProperty Wild = BooleanProperty.create("wild");
    public static final int DecayChance = 4;
    public static final int Shallow = 8;

    public Scp610FleshBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Wild, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Wild);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Wild, false);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return State.getValue(Wild);
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        decay(State, World, Pos, Random);
    }

    public static void decay(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (!State.hasProperty(Wild) || !State.getValue(Wild) || Random.nextInt(DecayChance) != 0 || Scp610Hives.nearLivingHive(World, Pos)) {
            return;
        }
        World.setBlock(Pos, rotten(World, Pos), Block.UPDATE_ALL);
    }

    public static BlockState rotten(ServerLevel World, BlockPos Pos) {
        int Surface = World.getHeight(Heightmap.Types.WORLD_SURFACE, Pos.getX(), Pos.getZ());
        int Depth = Surface - 1 - Pos.getY();
        if (Depth <= 1) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Depth <= Shallow) {
            return Blocks.ROOTED_DIRT.defaultBlockState();
        }
        return Blocks.PACKED_MUD.defaultBlockState();
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moving) {
        if (!State.is(Replaced.getBlock()) && World instanceof ServerLevel Server) {
            Scp610Hives.released(Server, Pos);
        }
        super.onRemove(State, World, Pos, Replaced, Moving);
    }
}
