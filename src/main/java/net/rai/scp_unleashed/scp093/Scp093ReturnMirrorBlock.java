package net.rai.scp_unleashed.scp093;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.TallProps;

import java.util.Map;

public class Scp093ReturnMirrorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp093ReturnMirrorBlock> BlockCodec = simpleCodec(Scp093ReturnMirrorBlock::new);
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int Light = 6;
    private static final Map<Direction, VoxelShape> LowerShapes = PropShapes.horizontal(PropShapes.of(PropShapes.box(0, 0, 3, 16, 3, 13), PropShapes.box(1, 3, 6, 15, 16, 10)));
    private static final Map<Direction, VoxelShape> UpperShapes = PropShapes.horizontal(PropShapes.of(PropShapes.box(1, 0, 6, 15, 16, 10)));

    public Scp093ReturnMirrorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half);
    }

    public static void place(Level World, BlockPos Lower, Direction Front, int Flags) {
        BlockState Base = Scp093Registry.ReturnMirror.get().defaultBlockState().setValue(FACING, Front);
        World.setBlock(Lower, Base.setValue(Half, DoubleBlockHalf.LOWER), Flags);
        World.setBlock(Lower.above(), Base.setValue(Half, DoubleBlockHalf.UPPER), Flags);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        BlockState Checked = TallProps.partnerCheck(this, State, Side, NeighborState);
        return Checked != null ? Checked : super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Half) == DoubleBlockHalf.LOWER ? LowerShapes : UpperShapes).get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!World.isClientSide) {
            travel(World, User);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            travel(World, User);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    private static void travel(Level World, Player User) {
        if (!(User instanceof ServerPlayer Traveller) || User instanceof FakePlayer || User.isSpectator()) {
            return;
        }
        if (!Scp093Wastes.inside(World)) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp093.return_dead").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        Scp093Transit.leave(Traveller);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(2) != 0) {
            return;
        }
        Direction Front = State.getValue(FACING);
        double Across = (Random.nextDouble() - 0.5) * 0.6;
        double X = Pos.getX() + 0.5 + Front.getStepX() * 0.18 + Front.getClockWise().getStepX() * Across;
        double Z = Pos.getZ() + 0.5 + Front.getStepZ() * 0.18 + Front.getClockWise().getStepZ() * Across;
        World.addParticle(new DustParticleOptions(Scp093MirrorBlock.Sheen, 0.7f), X, Pos.getY() + 0.15 + Random.nextDouble() * 0.75, Z, Front.getStepX() * 0.012, 0.012,
                Front.getStepZ() * 0.012);
    }
}
