package net.rai.scp_unleashed.rocketry.platform;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.oil.OilLayout;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketDestination;
import net.rai.scp_unleashed.rocketry.rocket.RocketDestinations;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PortableLaunchPadBlock extends Block implements LargeBlock {
    public static final MapCodec<PortableLaunchPadBlock> BlockCodec = simpleCodec(PortableLaunchPadBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final int Width = 3;
    public static final int Depth = 3;
    public static final OilLayout Layout = new OilLayout(Width, 1, Depth, 1, List.of(new AABB(0, 0, 0, 48, 4, 48)), List.of());
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Width * Depth - 1);
    public static final double[] StandPoint = {24.0, 4.0, 24.0};
    public static final List<LaunchCheck> Checks = List.of(LaunchCheck.Payload, LaunchCheck.Crew, LaunchCheck.Fuel, LaunchCheck.Sky, LaunchCheck.Route);
    private static boolean Dismantling;

    public PortableLaunchPadBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    public BlockPos originOf(BlockPos Cell, BlockState State) {
        return Layout.originOf(Cell, State.getValue(Facing), State.getValue(Part));
    }

    public boolean isMaster(BlockState State) {
        return State.getValue(Part) == 0;
    }

    public Vec3 standAt(BlockPos Origin, Direction Front) {
        return OilLayout.toWorld(Origin, Front, StandPoint[0], StandPoint[1], StandPoint[2]);
    }

    public int checks(ServerLevel World, BlockPos Origin, BlockState State, RocketEntity Rocket) {
        Direction Front = State.getValue(Facing);
        int Mask = LaunchCheck.Payload.bit();
        if (Rocket.hasCrew()) {
            Mask |= LaunchCheck.Crew.bit();
        }
        RocketDestination Route = RocketDestinations.portableRoute(World.getServer(), World.dimension());
        int Cost = Route == null ? LaunchPlatformKind.Rocket.getFuelCost() : Route.Fuel();
        if (Rocket.getStoredFuel() >= Cost) {
            Mask |= LaunchCheck.Fuel.bit();
        }
        List<BlockPos> Columns = new ArrayList<>();
        for (int Column = 0; Column < Width; Column++) {
            for (int Layer = 0; Layer < Depth; Layer++) {
                Columns.add(Layout.cellPos(Origin, Front, Layout.part(Column, 0, Layer)));
            }
        }
        if (LaunchSky.clear(World, Columns, Origin.getY() + 1, Spot -> false)) {
            Mask |= LaunchCheck.Sky.bit();
        }
        if (Route != null && RocketDestinations.available(World.getServer(), Route) && RocketDestinations.isLaunchSite(World.dimension())) {
            Mask |= LaunchCheck.Route.bit();
        }
        return Mask;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        return Layout.cells(Layout.masterFromPlaced(Context.getClickedPos(), Front), Front);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part);
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Layout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Layout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            Player User = Context.getPlayer();
            if (User != null && Context.getLevel().isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.oil_machine.no_room", Width, 1, Depth).withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, Layout.getPlacePart());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (World.isClientSide) {
            return;
        }
        Direction Front = State.getValue(Facing);
        BlockPos Master = Layout.masterFromPlaced(Pos, Front);
        for (int Index = 0; Index < Layout.cellCount(); Index++) {
            if (Index != Layout.getPlacePart()) {
                World.setBlock(Layout.cellPos(Master, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            BlockPos Origin = originOf(Pos, State);
            Direction Front = State.getValue(Facing);
            Dismantling = true;
            try {
                for (int Index = 0; Index < Layout.cellCount(); Index++) {
                    BlockPos Cell = Layout.cellPos(Origin, Front, Index);
                    if (Cell.equals(Pos)) {
                        continue;
                    }
                    BlockState CellState = World.getBlockState(Cell);
                    if (CellState.is(this) && CellState.getValue(Part) == Index && CellState.getValue(Facing) == Front) {
                        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            } finally {
                Dismantling = false;
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.portable_launch_pad.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.portable_launch_pad.size", Width, Depth).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.portable_launch_pad.use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.portable_launch_pad.fuel").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.launch_platform.sky").withStyle(ChatFormatting.GOLD));
        }
    }
}
