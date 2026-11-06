package net.rai.scp_unleashed.scp465;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class Scp465PartyBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty Tint = IntegerProperty.create("tint", 0, 3);
    public static final int Tints = 4;

    private final Scp465Decor Kind;
    private final MapCodec<Scp465PartyBlock> BlockCodec;
    private final Map<Direction, VoxelShape> Outlines;

    public Scp465PartyBlock(Scp465Decor Kind, Properties Settings) {
        super(Settings);
        this.Kind = Kind;
        this.BlockCodec = simpleCodec(Copied -> new Scp465PartyBlock(Kind, Copied));
        this.Outlines = PropShapes.horizontal(Kind.outline());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Tint, 0));
    }

    public Scp465Decor kind() {
        return Kind;
    }

    public static boolean isPartyBlock(BlockState State) {
        return State.getBlock() instanceof Scp465PartyBlock;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Tint);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        int Picked = Math.floorMod(Pos.getX() * 3 + Pos.getZ() * 5 + Pos.getY(), Tints);
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()).setValue(Tint, Picked);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Kind.solid() ? Outlines.get(State.getValue(FACING)) : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return !Kind.solid() || Kind == Scp465Decor.DanceFloor;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return !Kind.solid() || Kind == Scp465Decor.DanceFloor;
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!Kind.serves() || User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Scp465Celebration.serve((ServerLevel) World, Pos, User, Kind);
        return InteractionResult.CONSUME;
    }
}
