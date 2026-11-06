package net.rai.scp_unleashed.scp163;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Scp163DeviceBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty Busy = BooleanProperty.create("busy");
    public static final int UseTicks = 40;
    public static final double CuriousReach = 8.0;
    public static final double WorkReach = 2.0;
    public static final MapCodec<Scp163DeviceBlock> ConsoleCodec = simpleCodec(Properties0 -> new Scp163DeviceBlock(Properties0, Kind.Console));
    public static final MapCodec<Scp163DeviceBlock> SignallerCodec = simpleCodec(Properties0 -> new Scp163DeviceBlock(Properties0, Kind.Signaller));
    private static final Map<Direction, VoxelShape> ConsoleShapes = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> ConsoleFences = new EnumMap<>(Direction.class);
    private static final VoxelShape SignallerShape = Shapes.or(Block.box(4.0, 0.0, 4.0, 12.0, 2.0, 12.0), Block.box(6.0, 2.0, 6.0, 10.0, 11.0, 10.0),
            Block.box(4.0, 11.0, 6.0, 12.0, 15.0, 10.0));
    private static final VoxelShape SignallerFence = Block.box(4.0, 0.0, 4.0, 12.0, Scp163Footing.FenceHeight, 12.0);

    public enum Kind {
        Console,
        Signaller
    }

    static {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            ConsoleShapes.put(Side, Shapes.or(turn(Side, 3.0, 0.0, 5.0, 13.0, 6.0, 13.0), turn(Side, 1.0, 6.0, 3.0, 15.0, 8.0, 13.0),
                    turn(Side, 1.0, 8.0, 7.0, 15.0, 12.0, 13.0)));
            ConsoleFences.put(Side, turn(Side, 1.0, 0.0, 3.0, 15.0, Scp163Footing.FenceHeight, 13.0));
        }
    }

    private final Kind Type;

    public Scp163DeviceBlock(Properties Properties, Kind Type) {
        super(Properties);
        this.Type = Type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Busy, false));
    }

    private static VoxelShape turn(Direction Side, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Side) {
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    public Kind kind() {
        return Type;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Type == Kind.Console ? ConsoleCodec : SignallerCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Busy);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Type == Kind.Console ? ConsoleShapes.get(State.getValue(FACING)) : SignallerShape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        if (!Scp163Footing.keepsOff(Context, Pos)) {
            return getShape(State, World, Pos, Context);
        }
        return Type == Kind.Console ? ConsoleFences.get(State.getValue(FACING)) : SignallerFence;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Computation) {
        return false;
    }

    public static boolean isDevice(BlockState State) {
        return State.getBlock() instanceof Scp163DeviceBlock;
    }

    public static void pulse(ServerLevel World, BlockPos Pos, int Ticks) {
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof Scp163DeviceBlock Device)) {
            return;
        }
        if (!State.getValue(Busy)) {
            World.setBlock(Pos, State.setValue(Busy, true), Block.UPDATE_ALL);
        }
        World.scheduleTick(Pos, Device, Ticks);
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Device.Type == Kind.Console ? Scp163Registry.ConsoleLayers : Scp163Registry.SignalLayers;
        float Pitch = 0.9f + World.random.nextFloat() * 0.25f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, 0.55f, Pitch);
        }
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Busy)) {
            return;
        }
        if (!World.getEntitiesOfClass(Scp163Castaway.class, new AABB(Pos).inflate(WorkReach), Scp163Castaway::isFiddling).isEmpty()) {
            World.scheduleTick(Pos, this, UseTicks / 2);
            return;
        }
        World.setBlock(Pos, State.setValue(Busy, false), Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World instanceof ServerLevel Server) {
            pulse(Server, Pos, UseTicks);
            for (Scp163Castaway Watcher : Server.getEntitiesOfClass(Scp163Castaway.class, new AABB(Pos).inflate(CuriousReach), Scp163Castaway::isAlive)) {
                Watcher.noticeTinkering(User);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Busy)) {
            return;
        }
        if (Type == Kind.Signaller) {
            for (int Index = 0; Index < 2; Index++) {
                World.addParticle(Scp163Registry.Breath.get(), Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.2, Pos.getY() + 1.0 + Random.nextDouble() * 0.3,
                        Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.2, 0.0, 0.06 + Random.nextDouble() * 0.05, 0.0);
            }
        } else if (Random.nextInt(2) == 0) {
            World.addParticle(Scp163Registry.Breath.get(), Pos.getX() + 0.2 + Random.nextDouble() * 0.6, Pos.getY() + 0.8, Pos.getZ() + 0.2 + Random.nextDouble() * 0.6,
                    0.0, 0.02, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Key = Type == Kind.Console ? "block.scp_unleashed.scp163_console" : "block.scp_unleashed.scp163_signaller";
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".details").withStyle(ChatFormatting.GOLD));
        }
    }
}
