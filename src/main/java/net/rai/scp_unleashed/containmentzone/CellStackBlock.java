package net.rai.scp_unleashed.containmentzone;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.decor.PillarPart;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.PropTooltips;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CellStackBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<CellStackBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            CellStackKind.KindCodec.fieldOf("kind").forGetter(CellStackBlock::getKind), propertiesCodec()).apply(Instance, CellStackBlock::new));
    public static final EnumProperty<PillarPart> Part = EnumProperty.create("part", PillarPart.class);

    private final CellStackKind Kind;
    private final Map<PillarPart, Map<Direction, VoxelShape>> Outlines = new EnumMap<>(PillarPart.class);

    public CellStackBlock(CellStackKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        for (PillarPart Piece : PillarPart.values()) {
            Outlines.put(Piece, PropShapes.horizontal(Kind.getShape(Piece)));
        }
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Part, PillarPart.Single));
    }

    public CellStackKind getKind() {
        return Kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Part);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = defaultBlockState().setValue(FACING, CellPropBlock.facingFor(Context, Kind.isWallMounted()));
        return withPart(Placed, Context.getLevel(), Context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side.getAxis() == Direction.Axis.Y) {
            return withPart(State, World, Pos);
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    protected BlockState withPart(BlockState State, BlockGetter World, BlockPos Pos) {
        return State.setValue(Part, PillarPart.of(joins(State, World.getBlockState(Pos.below())), joins(State, World.getBlockState(Pos.above()))));
    }

    protected boolean joins(BlockState State, BlockState Other) {
        return Other.is(this) && Other.getValue(FACING) == State.getValue(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(Part)).get(State.getValue(FACING));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, Kind.getDetails());
    }
}
