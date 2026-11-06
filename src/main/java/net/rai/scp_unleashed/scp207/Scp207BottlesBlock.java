package net.rai.scp_unleashed.scp207;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.EnumMap;
import java.util.Map;

public class Scp207BottlesBlock extends Block {
    public static final MapCodec<Scp207BottlesBlock> BottlesCodec = simpleCodec(Scp207BottlesBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final int MaxBottles = 4;
    public static final IntegerProperty Count = IntegerProperty.create("bottles", 1, MaxBottles);
    public static final BooleanProperty[] Blue = {BooleanProperty.create("blue_0"), BooleanProperty.create("blue_1"), BooleanProperty.create("blue_2"), BooleanProperty.create("blue_3")};
    public static final float[][][] Spots = {
            {{8.0f, 8.0f}},
            {{5.0f, 8.0f}, {11.0f, 8.0f}},
            {{5.0f, 5.5f}, {11.0f, 5.5f}, {8.0f, 11.0f}},
            {{5.0f, 5.0f}, {11.0f, 5.0f}, {5.0f, 11.0f}, {11.0f, 11.0f}}
    };
    public static final double FootHalf = 2.5;
    public static final double BottleHeight = 11.0;
    private static final Map<Direction, VoxelShape[]> ShapeTable = shapes();

    public Scp207BottlesBlock(Properties Properties) {
        super(Properties);
        BlockState Start = stateDefinition.any().setValue(Facing, Direction.SOUTH).setValue(Count, 1);
        for (BooleanProperty Slot : Blue) {
            Start = Start.setValue(Slot, false);
        }
        registerDefaultState(Start);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BottlesCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Count, Blue[0], Blue[1], Blue[2], Blue[3]);
    }

    private static Map<Direction, VoxelShape[]> shapes() {
        Map<Direction, VoxelShape[]> Made = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            VoxelShape[] PerCount = new VoxelShape[MaxBottles];
            for (int Index = 0; Index < MaxBottles; Index++) {
                VoxelShape Union = Shapes.empty();
                for (float[] Spot : Spots[Index]) {
                    double[] Turned = turned(Spot[0], Spot[1], Side);
                    Union = Shapes.or(Union, box(Turned[0] - FootHalf, 0.0, Turned[1] - FootHalf, Turned[0] + FootHalf, BottleHeight, Turned[1] + FootHalf));
                }
                PerCount[Index] = Union.optimize();
            }
            Made.put(Side, PerCount);
        }
        return Made;
    }

    public static double[] turned(double X, double Z, Direction Side) {
        double Dx = X - 8.0;
        double Dz = Z - 8.0;
        return switch (Side) {
            case WEST -> new double[]{8.0 - Dz, 8.0 + Dx};
            case NORTH -> new double[]{8.0 - Dx, 8.0 - Dz};
            case EAST -> new double[]{8.0 + Dz, 8.0 - Dx};
            default -> new double[]{X, Z};
        };
    }

    public static Vec3 spot(BlockPos Pos, BlockState State, int Slot) {
        int Bottles = State.getValue(Count);
        float[] Spot = Spots[Bottles - 1][Math.min(Slot, Bottles - 1)];
        double[] Turned = turned(Spot[0], Spot[1], State.getValue(Facing));
        return new Vec3(Pos.getX() + Turned[0] / 16.0, Pos.getY() + 0.4, Pos.getZ() + Turned[1] / 16.0);
    }

    public static int count(BlockState State) {
        return State.getValue(Count);
    }

    public static boolean isBlue(BlockState State, int Slot) {
        return Slot >= 0 && Slot < MaxBottles && State.getValue(Blue[Slot]);
    }

    public static int blueCount(BlockState State) {
        int Found = 0;
        for (int Slot = 0; Slot < count(State); Slot++) {
            if (isBlue(State, Slot)) {
                Found++;
            }
        }
        return Found;
    }

    public static BlockState fresh(Direction Front, boolean IsBlue) {
        return Scp207Registry.Bottles.get().defaultBlockState().setValue(Facing, Front).setValue(Blue[0], IsBlue);
    }

    public static BlockState added(BlockState State, boolean IsBlue) {
        int Bottles = count(State);
        if (Bottles >= MaxBottles) {
            return State;
        }
        return State.setValue(Count, Bottles + 1).setValue(Blue[Bottles], IsBlue);
    }

    public static BlockState removed(BlockState State) {
        int Bottles = count(State);
        return State.setValue(Blue[Bottles - 1], false).setValue(Count, Math.max(1, Bottles - 1));
    }

    public static ItemStack lastBottle(BlockState State) {
        return Scp207BottleItem.stack(isBlue(State, count(State) - 1));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeTable.get(State.getValue(Facing))[count(State) - 1];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
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
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return canSupportCenter(World, Pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == Direction.DOWN && !canSurvive(State, World, Pos)) {
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
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        return Scp207BottleItem.is(Context.getItemInHand()) && count(State) < MaxBottles || super.canBeReplaced(State, Context);
    }

    public static InteractionResult place(BlockPlaceContext Context, boolean IsBlue) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState Existing = World.getBlockState(Pos);
        BlockState Wanted;
        if (Existing.is(Scp207Registry.Bottles.get())) {
            if (count(Existing) >= MaxBottles) {
                return InteractionResult.FAIL;
            }
            Wanted = added(Existing, IsBlue);
        } else {
            if (!Context.canPlace()) {
                return InteractionResult.FAIL;
            }
            Wanted = fresh(Context.getHorizontalDirection().getOpposite(), IsBlue);
            if (!Wanted.canSurvive(World, Pos) || !World.isUnobstructed(Wanted, Pos, CollisionContext.empty())) {
                return InteractionResult.FAIL;
            }
        }
        Player User = Context.getPlayer();
        if (!World.isClientSide) {
            World.setBlock(Pos, Wanted, Block.UPDATE_ALL);
            settle(World, Pos, Wanted, User);
            ItemStack Held = Context.getItemInHand();
            if (User instanceof ServerPlayer Server) {
                CriteriaTriggers.PLACED_BLOCK.trigger(Server, Pos, Held);
                Server.awardStat(Stats.ITEM_USED.get(Held.getItem()));
            }
            Held.consume(1, User);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    private static void settle(Level World, BlockPos Pos, BlockState State, Player User) {
        int Slot = count(State) - 1;
        Scp207Registry.playLayers(World, spot(Pos, State, Slot), Scp207Registry.ClinkLayers, SoundSource.BLOCKS, 0.7f, 0.95f + World.random.nextFloat() * 0.15f);
        World.gameEvent(GameEvent.BLOCK_PLACE, Pos, GameEvent.Context.of(User, State));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (Stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!Scp207BottleItem.is(Stack) || count(State) >= MaxBottles) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            BlockState Wanted = added(State, Scp207BottleItem.isOne(Stack));
            World.setBlock(Pos, Wanted, Block.UPDATE_ALL);
            settle(World, Pos, Wanted, User);
            User.awardStat(Stats.ITEM_USED.get(Stack.getItem()));
            Stack.consume(1, User);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            takeOne(World, Pos, State, User, true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static void takeOne(Level World, BlockPos Pos, BlockState State, Player User, boolean IntoHand) {
        ItemStack Taken = lastBottle(State);
        Vec3 Where = spot(Pos, State, count(State) - 1);
        if (count(State) <= 1) {
            World.removeBlock(Pos, false);
        } else {
            World.setBlock(Pos, removed(State), Block.UPDATE_ALL);
        }
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        Scp207Registry.playLayers(World, Where, Scp207Registry.ClinkLayers, SoundSource.BLOCKS, 0.6f, 1.05f + World.random.nextFloat() * 0.15f);
        if (IntoHand) {
            ItemHandlerHelper.giveItemToPlayer(User, Taken);
        } else if (!User.hasInfiniteMaterials()) {
            popResource(World, Pos, Taken);
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState State, Level World, BlockPos Pos, Player User, boolean WillHarvest, FluidState Fluid) {
        if (count(State) <= 1) {
            return super.onDestroyedByPlayer(State, World, Pos, User, WillHarvest, Fluid);
        }
        if (World.isClientSide) {
            World.setBlock(Pos, removed(State), Block.UPDATE_ALL_IMMEDIATE);
        } else {
            takeOne(World, Pos, State, User, false);
        }
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader World, BlockPos Pos, BlockState State) {
        return lastBottle(State);
    }
}
