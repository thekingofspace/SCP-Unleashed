package net.rai.scp_unleashed.scp5001.terminal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp5001.world.Scp5001WorldRegistry;

public class Scp5001TerminalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp5001TerminalBlock> BlockCodec = simpleCodec(Scp5001TerminalBlock::new);
    public static final IntegerProperty Pool = IntegerProperty.create("pool", 0, 7);
    public static final VoxelShape Shape = Shapes.or(Block.box(2.5, 0.0, 2.5, 13.5, 2.0, 13.5), Block.box(5.0, 2.0, 6.0, 11.0, 9.0, 12.5),
            Block.box(2.0, 9.0, 3.0, 14.0, 13.5, 13.0));

    public Scp5001TerminalBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Pool, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Pool);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User instanceof ServerPlayer Reader) {
            PacketDistributor.sendToPlayer(Reader, Scp5001TerminalPayload.read(Reader.server, Pos, State.getValue(Pool)));
            World.playSound(null, Pos, Scp5001WorldRegistry.TerminalOpen.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
