package net.rai.scp_unleashed.scp3008.furniture.children;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.SeatBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp3008FurnitureCBlock extends HorizontalDirectionalBlock implements SeatBlock {
    public static final MapCodec<Scp3008FurnitureCBlock> BlockCodec = simpleCodec(Settings -> new Scp3008FurnitureCBlock(Settings, Scp3008FurnitureCKind.ChildChair));
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final IntegerProperty PartPair = IntegerProperty.create("part", 0, 1);
    public static final IntegerProperty PartQuad = IntegerProperty.create("part", 0, 3);
    public static final IntegerProperty Aisle = IntegerProperty.create("aisle", 1, Scp3008FurnitureCKind.AisleCount);
    private static final ThreadLocal<Scp3008FurnitureCKind> Pending = new ThreadLocal<>();

    protected final Scp3008FurnitureCKind Type;

    public Scp3008FurnitureCBlock(Properties Settings, Scp3008FurnitureCKind Type) {
        super(prime(Settings, Type));
        Pending.remove();
        this.Type = Type;
        BlockState Default = stateDefinition.any().setValue(FACING, Direction.NORTH);
        if (partProperty() != null) {
            Default = Default.setValue(partProperty(), 0);
        }
        if (Type.Lamp) {
            Default = Default.setValue(Lit, false);
        }
        if (Type.hasOpen()) {
            Default = Default.setValue(Open, Type.StartsOpen);
        }
        if (Type.Aisle) {
            Default = Default.setValue(Aisle, 1);
        }
        registerDefaultState(Default);
    }

    private static Properties prime(Properties Settings, Scp3008FurnitureCKind Type) {
        Pending.set(Type);
        return Settings;
    }

    public Scp3008FurnitureCKind kind() {
        return Type;
    }

    @Nullable
    public IntegerProperty partProperty() {
        return partProperty(Type);
    }

    @Nullable
    public static IntegerProperty partProperty(Scp3008FurnitureCKind Making) {
        return switch (Making.parts()) {
            case 2 -> PartPair;
            case 4 -> PartQuad;
            default -> null;
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Scp3008FurnitureCKind Making = Pending.get();
        Builder.add(FACING);
        if (Making == null) {
            return;
        }
        if (Making.parts() == 2) {
            Builder.add(PartPair);
        }
        if (Making.parts() == 4) {
            Builder.add(PartQuad);
        }
        if (Making.Lamp) {
            Builder.add(Lit);
        }
        if (Making.hasOpen()) {
            Builder.add(Open);
        }
        if (Making.Aisle) {
            Builder.add(Aisle);
        }
    }

    public int partOf(BlockState State) {
        IntegerProperty Part = partProperty();
        return Part != null && State.hasProperty(Part) ? State.getValue(Part) : 0;
    }

    public static boolean isOpen(BlockState State) {
        return State.hasProperty(Open) && State.getValue(Open);
    }

    public static BlockPos cellPos(BlockPos Main, Direction Facing, int[] Cell) {
        return Main.relative(Facing.getClockWise(), Cell[0]).above(Cell[1]).relative(Facing.getOpposite(), Cell[2]);
    }

    public static BlockPos mainPos(BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp3008FurnitureCBlock Piece)) {
            return Pos;
        }
        int[] Cell = Piece.Type.Cells[Piece.partOf(State)];
        Direction Facing = State.getValue(FACING);
        return Pos.relative(Facing.getClockWise(), -Cell[0]).below(Cell[1]).relative(Facing.getOpposite(), -Cell[2]);
    }

    public static void placeWhole(Level World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp3008FurnitureCBlock Piece) || !Piece.Type.multiblock()) {
            return;
        }
        BlockPos Main = mainPos(Pos, State);
        Direction Facing = State.getValue(FACING);
        IntegerProperty Part = Piece.partProperty();
        for (int Index = 0; Index < Piece.Type.parts(); Index++) {
            BlockPos Spot = cellPos(Main, Facing, Piece.Type.Cells[Index]);
            BlockState Wanted = State.setValue(Part, Index);
            if (!World.getBlockState(Spot).equals(Wanted)) {
                World.setBlock(Spot, Wanted, Block.UPDATE_ALL);
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Facing = Context.getHorizontalDirection().getOpposite();
        BlockState Placed = defaultBlockState().setValue(FACING, Facing);
        if (Type.multiblock()) {
            Level World = Context.getLevel();
            for (int Index = 1; Index < Type.parts(); Index++) {
                BlockPos Spot = cellPos(Context.getClickedPos(), Facing, Type.Cells[Index]);
                if (!World.isInWorldBounds(Spot) || !World.getWorldBorder().isWithinBounds(Spot) || !World.getBlockState(Spot).canBeReplaced(Context)) {
                    return null;
                }
            }
        }
        return Placed;
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (!World.isClientSide) {
            placeWhole(World, Pos, State);
        }
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Other, LevelAccessor World, BlockPos Pos, BlockPos OtherPos) {
        if (Type.multiblock()) {
            Direction Facing = State.getValue(FACING);
            BlockPos Main = mainPos(Pos, State);
            int Own = partOf(State);
            for (int Index = 0; Index < Type.parts(); Index++) {
                if (Index == Own || !cellPos(Main, Facing, Type.Cells[Index]).equals(OtherPos)) {
                    continue;
                }
                boolean Kept = Other.is(this) && Other.getValue(FACING) == Facing && partOf(Other) == Index;
                return Kept ? State : Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(State, Side, Other, World, Pos, OtherPos);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Type.shape(partOf(State), State.getValue(FACING), isOpen(State));
    }

    @Override
    public boolean seats(BlockState State) {
        return Type.Seat;
    }

    public static int lightOf(BlockState State, Scp3008FurnitureCKind Making) {
        if (!Making.Lamp || !State.hasProperty(Lit) || !State.getValue(Lit)) {
            return 0;
        }
        IntegerProperty Part = partProperty(Making);
        return Making.Glow[Part != null && State.hasProperty(Part) ? State.getValue(Part) : 0];
    }

    protected void setEverywhere(Level World, BlockPos Pos, BlockState State, BooleanProperty Flag, boolean Value) {
        if (!Type.multiblock()) {
            World.setBlock(Pos, State.setValue(Flag, Value), Block.UPDATE_ALL);
            return;
        }
        BlockPos Main = mainPos(Pos, State);
        Direction Facing = State.getValue(FACING);
        for (int Index = 0; Index < Type.parts(); Index++) {
            BlockPos Spot = cellPos(Main, Facing, Type.Cells[Index]);
            BlockState Found = World.getBlockState(Spot);
            if (Found.is(this) && Found.hasProperty(Flag)) {
                World.setBlock(Spot, Found.setValue(Flag, Value), Block.UPDATE_CLIENTS);
            }
        }
    }

    protected static void play(Level World, BlockPos Pos, SoundEvent Event, float Volume, float Pitch) {
        World.playSound(null, Pos, Event, SoundSource.BLOCKS, Volume, Pitch);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (Type.Seat) {
            if (User.isSecondaryUseActive() || User.isPassenger() || PropSeatEntity.occupied(World, Pos)) {
                return InteractionResult.PASS;
            }
            if (World.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (PropSeatEntity.sit(World, Pos, State, User)) {
                if (Type == Scp3008FurnitureCKind.RockingElk) {
                    play(World, Pos, Scp3008FurnitureCRegistry.RockCreak.get(), 0.6f, 1.0f + World.random.nextFloat() * 0.15f);
                }
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        if (Type.Lamp) {
            if (World.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            boolean On = !State.getValue(Lit);
            setEverywhere(World, Pos, State, Lit, On);
            play(World, Pos, Scp3008FurnitureCRegistry.SwitchClick.get(), 0.5f, On ? 1.3f : 1.05f);
            play(World, Pos, On ? Scp3008FurnitureCRegistry.LampOn.get() : Scp3008FurnitureCRegistry.LampOff.get(), 0.6f, 1.0f);
            return InteractionResult.CONSUME;
        }
        if (Type.Aisle) {
            if (World.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            int Number = State.getValue(Aisle);
            int Next = User.isSecondaryUseActive() ? (Number <= 1 ? Scp3008FurnitureCKind.AisleCount : Number - 1) : (Number >= Scp3008FurnitureCKind.AisleCount ? 1 : Number + 1);
            World.setBlock(Pos, State.setValue(Aisle, Next), Block.UPDATE_ALL);
            play(World, Pos, Scp3008FurnitureCRegistry.SignTurn.get(), 0.6f, 0.95f + World.random.nextFloat() * 0.1f);
            return InteractionResult.CONSUME;
        }
        if (Type.Opens) {
            if (World.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            boolean Opening = !State.getValue(Open);
            setEverywhere(World, Pos, State, Open, Opening);
            if (Type == Scp3008FurnitureCKind.Parasol) {
                play(World, Pos, Opening ? Scp3008FurnitureCRegistry.ParasolOpen.get() : Scp3008FurnitureCRegistry.ParasolClose.get(), 0.8f, 1.0f);
            } else {
                play(World, Pos, Opening ? Scp3008FurnitureCRegistry.OvenOpen.get() : Scp3008FurnitureCRegistry.OvenClose.get(), 0.7f, 1.0f);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (Type.Seat && !World.isClientSide && !State.is(NewState.getBlock())) {
            PropSeatEntity.release(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        String Hint = Type.Seat ? "sit" : Type.Lamp ? "lamp" : Type.Aisle ? "aisle" : Type.Opens ? "open" : null;
        if (Hint != null) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.scp3008_furnc." + Hint).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
