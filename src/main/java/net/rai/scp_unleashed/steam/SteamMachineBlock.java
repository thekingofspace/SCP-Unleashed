package net.rai.scp_unleashed.steam;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SteamMachineBlock extends BaseEntityBlock implements LargeBlock {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MachineLight> Light = EnumProperty.create("light", MachineLight.class);
    private static boolean Dismantling;

    protected SteamMachineBlock(Properties Properties) {
        super(Properties);
    }

    public abstract MachineLayout layout();

    public abstract IntegerProperty partProperty();

    protected abstract VoxelShape[][] shapes();

    protected abstract boolean isPortCell(int Part);

    public abstract int lampPart();

    protected void dismantled(ServerLevel World, BlockPos MasterPos, BlockState MasterState) {
    }

    public int partOf(BlockState State) {
        return State.getValue(partProperty());
    }

    public BlockPos masterPos(BlockState State, BlockPos Pos) {
        return layout().masterOf(Pos, partOf(State), State.getValue(Facing));
    }

    public boolean isPort(BlockState State, Direction Side, int Part, Direction Local) {
        return partOf(State) == Part && Side == MachineLayout.toWorld(Local, State.getValue(Facing));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, partProperty(), Light);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return shapes()[MachineLayout.turnOf(State.getValue(Facing))][partOf(State)];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return supportShape(MachineLayout.turnOf(State.getValue(Facing)), partOf(State));
    }

    protected VoxelShape supportShape(int Turn, int Part) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState State, BlockGetter World, BlockPos Pos) {
        return 1.0f;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        return layout().cells(Context.getClickedPos(), Context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        List<BlockPos> Footprint = getFootprint(Context);
        if (!LargeBlock.canPlace(Context, Footprint)) {
            Player User = Context.getPlayer();
            if (User != null && Context.getLevel().isClientSide) {
                MachineLayout Layout = layout();
                User.displayClientMessage(Component.translatable("message.scp_unleashed.steam_machine.no_room", Layout.Width(), Layout.Height(), Layout.Depth(),
                        LargeBlock.blockedCells(Context, Footprint).size()).withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(partProperty(), layout().origin()).setValue(Light, MachineLight.Off);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        Direction Front = State.getValue(Facing);
        MachineLayout Layout = layout();
        for (int Part = 0; Part < Layout.count(); Part++) {
            if (Part != Layout.origin()) {
                World.setBlock(Layout.cellPos(Pos, Front, Part), State.setValue(partProperty(), Part).setValue(Light, MachineLight.Off), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this)) {
            if (isPortCell(partOf(State))) {
                World.invalidateCapabilities(Pos);
            }
            if (World instanceof ServerLevel ServerWorld) {
                placedOn(ServerWorld, Pos, State);
            }
        }
    }

    protected void placedOn(ServerLevel World, BlockPos Pos, BlockState State) {
        if (isPortCell(partOf(State))) {
            PressureNetworkManager.blockChanged(World, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        boolean Replaced = !State.is(NewState.getBlock());
        if (Replaced && World instanceof ServerLevel ServerWorld) {
            MachineLayout Layout = layout();
            int Part = partOf(State);
            Direction Front = State.getValue(Facing);
            if (Part == Layout.Master()) {
                dismantled(ServerWorld, Pos, State);
            }
            if (!Dismantling) {
                Dismantling = true;
                try {
                    BlockPos Origin = Layout.originOf(Pos, Part, Front);
                    for (int Index = 0; Index < Layout.count(); Index++) {
                        BlockPos Cell = Layout.cellPos(Origin, Front, Index);
                        if (Cell.equals(Pos)) {
                            continue;
                        }
                        BlockState CellState = World.getBlockState(Cell);
                        if (CellState.is(this) && partOf(CellState) == Index && CellState.getValue(Facing) == Front) {
                            World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                        }
                    }
                } finally {
                    Dismantling = false;
                }
            }
            removedFrom(ServerWorld, Pos, State);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
        if (Replaced && isPortCell(partOf(State))) {
            World.invalidateCapabilities(Pos);
        }
    }

    protected void removedFrom(ServerLevel World, BlockPos Pos, BlockState State) {
        if (isPortCell(partOf(State))) {
            PressureNetworkManager.blockChanged(World, Pos);
        }
    }

    public BlockPos lampPos(BlockState MasterState, BlockPos MasterPos) {
        Direction Front = MasterState.getValue(Facing);
        return layout().cellPos(layout().originOf(MasterPos, partOf(MasterState), Front), Front, lampPart());
    }

    public void showLight(Level World, BlockPos MasterPos, BlockState MasterState, MachineLight Shown) {
        BlockPos Lamp = lampPos(MasterState, MasterPos);
        BlockState LampState = World.getBlockState(Lamp);
        if (LampState.is(this) && partOf(LampState) == lampPart() && LampState.getValue(Light) != Shown) {
            World.setBlock(Lamp, LampState.setValue(Light, Shown), Block.UPDATE_CLIENTS);
        }
    }

    public MachineLight shownLight(Level World, BlockPos MasterPos, BlockState MasterState) {
        BlockState LampState = World.getBlockState(lampPos(MasterState, MasterPos));
        return LampState.is(this) ? LampState.getValue(Light) : MachineLight.Off;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State;
    }
}
