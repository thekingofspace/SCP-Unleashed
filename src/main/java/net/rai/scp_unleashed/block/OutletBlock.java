package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.inkwire.InkHideable;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.ConnectorTier;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.Wires;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OutletBlock extends BaseEntityBlock implements WireConnector, InkHideable {
    public static final MapCodec<OutletBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ConnectorTier.TierCodec.fieldOf("tier").forGetter(OutletBlock::getConnectorTier),
            propertiesCodec()
    ).apply(Instance, OutletBlock::new));
    public static final DirectionProperty Facing = BlockStateProperties.FACING;
    public static final BooleanProperty Connected = BooleanProperty.create("connected");
    public static final BooleanProperty Hidden = BooleanProperty.create("hidden");

    private final ConnectorTier Tier;
    private final Map<Direction, VoxelShape> FacingShapes = new EnumMap<>(Direction.class);

    public OutletBlock(ConnectorTier Tier, Properties Properties) {
        super(Properties);
        this.Tier = Tier;
        boolean Heavy = Tier != ConnectorTier.Basic;
        for (Direction OutletFacing : Direction.values()) {
            FacingShapes.put(OutletFacing, Heavy
                    ? Shapes.or(mountedBox(OutletFacing, 6.0, 0.0, 3.0), mountedBox(OutletFacing, 4.0, 3.0, 13.0))
                    : Shapes.or(mountedBox(OutletFacing, 4.0, 0.0, 2.0), mountedBox(OutletFacing, 2.0, 2.0, 11.0)));
        }
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Connected, false).setValue(Hidden, false));
    }

    private static VoxelShape mountedBox(Direction OutletFacing, double HalfSize, double Near, double Far) {
        double[] Min = {8.0 - HalfSize, 8.0 - HalfSize, 8.0 - HalfSize};
        double[] Max = {8.0 + HalfSize, 8.0 + HalfSize, 8.0 + HalfSize};
        int Axis = OutletFacing.getAxis().ordinal();
        boolean Positive = OutletFacing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        Min[Axis] = Positive ? Near : 16.0 - Far;
        Max[Axis] = Positive ? Far : 16.0 - Near;
        return box(Min[0], Min[1], Min[2], Max[0], Max[1], Max[2]);
    }

    @Override
    public ConnectorTier getConnectorTier() {
        return Tier;
    }

    @Override
    public boolean isFullBlock() {
        return false;
    }

    @Override
    public Direction linkFace(BlockState State, Direction Clicked) {
        return State.getValue(Facing);
    }

    @Override
    public BlockPos hostPos(BlockState State, BlockPos Pos) {
        return Pos.relative(State.getValue(Facing).getOpposite());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Connected, Hidden);
    }

    @Override
    public BooleanProperty hiddenProperty() {
        return Hidden;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return State.getValue(Hidden) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return FacingShapes.get(State.getValue(Facing));
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        Direction OutletFacing = State.getValue(Facing);
        BlockPos SupportPos = Pos.relative(OutletFacing.getOpposite());
        return World.getBlockState(SupportPos).isFaceSturdy(World, SupportPos, OutletFacing, SupportType.CENTER);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Clicked = defaultBlockState().setValue(Facing, Context.getClickedFace());
        if (Clicked.canSurvive(Context.getLevel(), Context.getClickedPos())) {
            return Clicked;
        }
        for (Direction Looking : Context.getNearestLookingDirections()) {
            BlockState Candidate = defaultBlockState().setValue(Facing, Looking.getOpposite());
            if (Candidate.canSurvive(Context.getLevel(), Context.getClickedPos())) {
                return Candidate;
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == State.getValue(Facing).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld && ServerWorld.getBlockEntity(Pos) instanceof OutletBlockEntity Outlet) {
            Wires.detachAll(ServerWorld, Pos, Outlet);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Description = switch (Tier) {
            case Basic -> "block.scp_unleashed.outlet.tooltip";
            case Tether -> "block.scp_unleashed.tether_outlet.tooltip";
            default -> "block.scp_unleashed.advanced_outlet.tooltip";
        };
        Lines.add(Component.translatable(Description).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.outlet.max_demand", Tier.getMaxDemand()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.outlet.max_connections", Wires.MaxLinksPerOutlet).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.connector.wires", Tier.getStrongest().getCoil().getDescription()).withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new OutletBlockEntity(Pos, State);
    }
}
