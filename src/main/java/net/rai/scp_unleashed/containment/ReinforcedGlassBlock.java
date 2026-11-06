package net.rai.scp_unleashed.containment;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ReinforcedGlassBlock extends Block {
    public static final MapCodec<ReinforcedGlassBlock> BlockCodec = simpleCodec(ReinforcedGlassBlock::new);
    public static final EnumProperty<Direction.Axis> WindowAxis = BlockStateProperties.AXIS;
    public static final BooleanProperty Up = OneWayGlassBlock.Up;
    public static final BooleanProperty Down = OneWayGlassBlock.Down;
    public static final BooleanProperty Left = OneWayGlassBlock.Left;
    public static final BooleanProperty Right = OneWayGlassBlock.Right;
    public static final float SteepPitch = 60.0f;

    public ReinforcedGlassBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(WindowAxis, Direction.Axis.Z).setValue(Up, false).setValue(Down, false).setValue(Left, false).setValue(Right, false));
    }

    @Override
    protected MapCodec<? extends ReinforcedGlassBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(WindowAxis, Up, Down, Left, Right);
    }

    @Nullable
    public static BooleanProperty linkFor(Direction.Axis Normal, Direction Side) {
        Direction Top = Normal == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
        Direction Leftward = Normal == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        if (Side == Top) {
            return Up;
        }
        if (Side == Top.getOpposite()) {
            return Down;
        }
        if (Side == Leftward) {
            return Left;
        }
        return Side == Leftward.getOpposite() ? Right : null;
    }

    public boolean linksTo(BlockState State, BlockState Neighbour) {
        return Neighbour.is(this) && Neighbour.getValue(WindowAxis) == State.getValue(WindowAxis);
    }

    protected BlockState withLinks(BlockState State, BlockGetter World, BlockPos Pos) {
        Direction.Axis Normal = State.getValue(WindowAxis);
        BlockState Linked = State;
        for (Direction Side : Direction.values()) {
            BooleanProperty Link = linkFor(Normal, Side);
            if (Link != null) {
                Linked = Linked.setValue(Link, linksTo(State, World.getBlockState(Pos.relative(Side))));
            }
        }
        return Linked;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return withLinks(defaultBlockState().setValue(WindowAxis, placementAxis(Context)), Context.getLevel(), Context.getClickedPos());
    }

    private Direction.Axis placementAxis(BlockPlaceContext Context) {
        BlockGetter World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Direction Face = Context.getClickedFace();
        BlockState Against = World.getBlockState(Pos.relative(Face.getOpposite()));
        if (Against.is(this) && Against.getValue(WindowAxis) != Face.getAxis()) {
            return Against.getValue(WindowAxis);
        }
        Set<Direction.Axis> Candidates = EnumSet.noneOf(Direction.Axis.class);
        for (Direction Side : Direction.values()) {
            BlockState Neighbour = World.getBlockState(Pos.relative(Side));
            if (Neighbour.is(this) && Neighbour.getValue(WindowAxis) != Side.getAxis()) {
                Candidates.add(Neighbour.getValue(WindowAxis));
            }
        }
        if (Candidates.size() == 1) {
            return Candidates.iterator().next();
        }
        Player User = Context.getPlayer();
        if (User != null && Math.abs(User.getXRot()) > SteepPitch) {
            return Direction.Axis.Y;
        }
        return Context.getHorizontalDirection().getAxis();
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        BooleanProperty Link = linkFor(State.getValue(WindowAxis), Side);
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

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        Direction.Axis Before = State.getValue(WindowAxis);
        Direction.Axis After = Turn.rotate(Direction.get(Direction.AxisDirection.POSITIVE, Before)).getAxis();
        BlockState Turned = State.setValue(WindowAxis, After);
        for (Direction Side : Direction.values()) {
            BooleanProperty Link = linkFor(Before, Side);
            if (Link != null) {
                Turned = Turned.setValue(linkFor(After, Turn.rotate(Side)), State.getValue(Link));
            }
        }
        return Turned;
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        Direction.Axis Normal = State.getValue(WindowAxis);
        BlockState Flipped = State;
        for (Direction Side : Direction.values()) {
            BooleanProperty Link = linkFor(Normal, Side);
            if (Link != null) {
                Flipped = Flipped.setValue(linkFor(Normal, Flip.mirror(Side)), State.getValue(Link));
            }
        }
        return Flipped;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.reinforced_glass.placing").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.reinforced_glass.merging").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.containment.blast_proof").withStyle(ChatFormatting.GOLD));
        }
    }
}
