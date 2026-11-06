package net.rai.scp_unleashed.containment;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OneWayGlassBlock extends Block {
    public static final MapCodec<OneWayGlassBlock> BlockCodec = simpleCodec(OneWayGlassBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Up = BlockStateProperties.UP;
    public static final BooleanProperty Down = BlockStateProperties.DOWN;
    public static final BooleanProperty Left = BooleanProperty.create("left");
    public static final BooleanProperty Right = BooleanProperty.create("right");

    public OneWayGlassBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Up, false).setValue(Down, false).setValue(Left, false).setValue(Right, false));
    }

    @Override
    protected MapCodec<? extends OneWayGlassBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Up, Down, Left, Right);
    }

    @Nullable
    public static BooleanProperty linkFor(Direction Front, Direction Side) {
        if (Side == Direction.UP) {
            return Up;
        }
        if (Side == Direction.DOWN) {
            return Down;
        }
        if (Side == Front.getClockWise()) {
            return Left;
        }
        return Side == Front.getCounterClockWise() ? Right : null;
    }

    public boolean linksTo(BlockState State, BlockState Neighbour) {
        return Neighbour.is(this) && Neighbour.getValue(Facing) == State.getValue(Facing);
    }

    protected BlockState withLinks(BlockState State, BlockGetter World, BlockPos Pos) {
        Direction Front = State.getValue(Facing);
        BlockState Linked = State;
        for (Direction Side : Direction.values()) {
            BooleanProperty Link = linkFor(Front, Side);
            if (Link != null) {
                Linked = Linked.setValue(Link, linksTo(State, World.getBlockState(Pos.relative(Side))));
            }
        }
        return Linked;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Wanted = Context.getHorizontalDirection().getOpposite();
        Direction Front = Context.isSecondaryUseActive() ? Wanted : joinedFacing(Context.getLevel(), Context.getClickedPos(), Wanted);
        return withLinks(defaultBlockState().setValue(Facing, Front), Context.getLevel(), Context.getClickedPos());
    }

    private Direction joinedFacing(BlockGetter World, BlockPos Pos, Direction Wanted) {
        Direction Found = null;
        for (Direction Side : Direction.values()) {
            BlockState Neighbour = World.getBlockState(Pos.relative(Side));
            if (!Neighbour.is(this)) {
                continue;
            }
            Direction Front = Neighbour.getValue(Facing);
            if (Side.getAxis() == Front.getAxis()) {
                continue;
            }
            if (Front == Wanted) {
                return Wanted;
            }
            if (Found == null) {
                Found = Front;
            }
        }
        return Found != null ? Found : Wanted;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        BooleanProperty Link = linkFor(State.getValue(Facing), Side);
        return Link == null ? State : State.setValue(Link, linksTo(State, Neighbour));
    }

    @Override
    protected VoxelShape getVisualShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    public boolean blocksLight(BlockState State) {
        return true;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return !blocksLight(State);
    }

    @Override
    protected int getLightBlock(BlockState State, BlockGetter World, BlockPos Pos) {
        return blocksLight(State) ? World.getMaxLightLevel() : 0;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        if (Flip == Mirror.NONE) {
            return State;
        }
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing))).setValue(Left, State.getValue(Right)).setValue(Right, State.getValue(Left));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            appendDetails(Lines);
        }
    }

    protected void appendDetails(List<Component> Lines) {
        Lines.add(Component.translatable("block.scp_unleashed.one_way_glass.placing").withStyle(ChatFormatting.GOLD));
        Lines.add(Component.translatable("block.scp_unleashed.one_way_glass.merging").withStyle(ChatFormatting.GOLD));
        Lines.add(Component.translatable("block.scp_unleashed.containment.blast_proof").withStyle(ChatFormatting.GOLD));
    }
}
