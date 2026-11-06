package net.rai.scp_unleashed.greenhouse;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropTooltips;

import java.util.List;

public class GreenhouseGlassWallBlock extends Block {
    public static final MapCodec<GreenhouseGlassWallBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            GlassWallKind.KindCodec.fieldOf("kind").forGetter(GreenhouseGlassWallBlock::getKind), propertiesCodec()).apply(Instance, GreenhouseGlassWallBlock::new));
    public static final EnumProperty<Direction.Axis> WallAxis = BlockStateProperties.HORIZONTAL_AXIS;

    private final GlassWallKind Kind;
    private final VoxelShape ShapeAlongX;
    private final VoxelShape ShapeAlongZ;

    public GreenhouseGlassWallBlock(GlassWallKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        this.ShapeAlongX = Kind.shape(true);
        this.ShapeAlongZ = Kind.shape(false);
        registerDefaultState(stateDefinition.any().setValue(WallAxis, Direction.Axis.Z));
    }

    public GlassWallKind getKind() {
        return Kind;
    }

    @Override
    protected MapCodec<? extends GreenhouseGlassWallBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(WallAxis);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(WallAxis, Context.getHorizontalDirection().getAxis());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        if (Turn == Rotation.CLOCKWISE_90 || Turn == Rotation.COUNTERCLOCKWISE_90) {
            return State.setValue(WallAxis, State.getValue(WallAxis) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return State;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(WallAxis) == Direction.Axis.X ? ShapeAlongX : ShapeAlongZ;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected boolean skipRendering(BlockState State, BlockState Adjacent, Direction Side) {
        if (Adjacent.getBlock() instanceof GreenhouseGlassWallBlock Other && Adjacent.getValue(WallAxis) == State.getValue(WallAxis)) {
            if (Side == Direction.UP) {
                return Kind.panelAbove() && Other.getKind().panelBelow();
            }
            if (Side == Direction.DOWN) {
                return Kind.panelBelow() && Other.getKind().panelAbove();
            }
            if (Side.getAxis() != State.getValue(WallAxis)) {
                return Other.getKind() == Kind;
            }
        }
        return super.skipRendering(State, Adjacent, Side);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add("block.scp_unleashed.greenhouse_glass_wall", Lines);
    }
}
