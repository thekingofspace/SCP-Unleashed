package net.rai.scp_unleashed.zoneprops;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.PropWater;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ZonePairedPropBlock extends ZonePropBlock implements LargeBlock {
    public static final MapCodec<ZonePairedPropBlock> PairCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ZonePropKind.KindCodec.fieldOf("kind").forGetter(ZonePropBlock::getKind), propertiesCodec()).apply(Instance, ZonePairedPropBlock::new));
    public static final EnumProperty<PairPart> Pair = EnumProperty.create("pair", PairPart.class);

    private final Map<PairPart, Map<Direction, VoxelShape>> PartOutlines = new EnumMap<>(PairPart.class);

    public ZonePairedPropBlock(ZonePropKind Kind, Properties Properties) {
        super(Kind, Properties);
        for (PairPart Piece : PairPart.values()) {
            PartOutlines.put(Piece, PropShapes.horizontal(Kind.getShape(Piece.ordinal())));
        }
        registerDefaultState(defaultBlockState().setValue(Pair, PairPart.Main));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return PairCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Pair);
    }

    private boolean sideways() {
        return Kind.getMount() == ZoneMount.SidePair;
    }

    private Direction facingFor(BlockPlaceContext Context) {
        return sideways() ? Context.getHorizontalDirection().getOpposite() : Context.getHorizontalDirection();
    }

    public Direction towardExtension(Direction Facing) {
        return sideways() ? Facing.getCounterClockWise() : Facing;
    }

    private Direction towardPartner(BlockState State) {
        Direction Toward = towardExtension(State.getValue(FACING));
        return State.getValue(Pair) == PairPart.Main ? Toward : Toward.getOpposite();
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.relative(towardExtension(facingFor(Context))));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return PropWater.placed(defaultBlockState().setValue(FACING, facingFor(Context)), Context);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (World.isClientSide) {
            return;
        }
        BlockPos Other = Pos.relative(towardExtension(State.getValue(FACING)));
        boolean Water = World.getFluidState(Other).getType() == Fluids.WATER;
        World.setBlock(Other, State.setValue(Pair, PairPart.Extension).setValue(PropWater.Waterlogged, Water), Block.UPDATE_ALL);
        World.blockUpdated(Pos, Blocks.AIR);
        State.updateNeighbourShapes(World, Pos, Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == towardPartner(State)) {
            boolean Partner = NeighborState.is(this) && NeighborState.getValue(Pair) != State.getValue(Pair) && NeighborState.getValue(FACING) == State.getValue(FACING);
            if (!Partner) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Pair) == PairPart.Extension) {
            BlockPos MainPos = Pos.relative(towardPartner(State));
            BlockState MainState = World.getBlockState(MainPos);
            if (MainState.is(this) && MainState.getValue(Pair) == PairPart.Main) {
                World.setBlock(MainPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, MainPos, Block.getId(MainState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected VoxelShape outline(BlockState State) {
        return PartOutlines.get(State.getValue(Pair)).get(State.getValue(FACING));
    }
}
