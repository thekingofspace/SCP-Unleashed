package net.rai.scp_unleashed.scp3008.furniture.electronics;

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

public class Scp3008FurnitureEBlock extends HorizontalDirectionalBlock implements SeatBlock {
    public static final MapCodec<Scp3008FurnitureEBlock> BlockCodec = simpleCodec(Settings -> new Scp3008FurnitureEBlock(Settings, Scp3008FurnitureEKind.GamingChair));
    public static final BooleanProperty On = BooleanProperty.create("on");
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final IntegerProperty PartPair = IntegerProperty.create("part", 0, 1);
    public static final IntegerProperty PartQuad = IntegerProperty.create("part", 0, 3);
    public static final IntegerProperty PartSix = IntegerProperty.create("part", 0, 5);
    private static final ThreadLocal<Scp3008FurnitureEKind> Pending = new ThreadLocal<>();

    protected final Scp3008FurnitureEKind Type;

    public Scp3008FurnitureEBlock(Properties Settings, Scp3008FurnitureEKind Type) {
        super(prime(Settings, Type));
        Pending.remove();
        this.Type = Type;
        BlockState Default = stateDefinition.any().setValue(FACING, Direction.NORTH);
        if (partProperty() != null) {
            Default = Default.setValue(partProperty(), 0);
        }
        if (Type.screen()) {
            Default = Default.setValue(On, false);
        }
        if (Type.lamp()) {
            Default = Default.setValue(Lit, false);
        }
        registerDefaultState(Default);
    }

    private static Properties prime(Properties Settings, Scp3008FurnitureEKind Type) {
        Pending.set(Type);
        return Settings;
    }

    public Scp3008FurnitureEKind kind() {
        return Type;
    }

    @Nullable
    public IntegerProperty partProperty() {
        return partProperty(Type);
    }

    @Nullable
    public static IntegerProperty partProperty(Scp3008FurnitureEKind Making) {
        return switch (Making.parts()) {
            case 2 -> PartPair;
            case 4 -> PartQuad;
            case 6 -> PartSix;
            default -> null;
        };
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Scp3008FurnitureEKind Making = Pending.get();
        Builder.add(FACING);
        if (Making == null) {
            return;
        }
        IntegerProperty Part = partProperty(Making);
        if (Part != null) {
            Builder.add(Part);
        }
        if (Making.screen()) {
            Builder.add(On);
        }
        if (Making.lamp()) {
            Builder.add(Lit);
        }
    }

    public int partOf(BlockState State) {
        IntegerProperty Part = partProperty();
        return Part != null && State.hasProperty(Part) ? State.getValue(Part) : 0;
    }

    public static BlockPos cellPos(BlockPos Main, Direction Facing, int[] Cell) {
        return Main.relative(Facing.getClockWise(), Cell[0]).above(Cell[1]).relative(Facing.getOpposite(), Cell[2]);
    }

    public static BlockPos mainPos(BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp3008FurnitureEBlock Piece)) {
            return Pos;
        }
        int[] Cell = Piece.Type.Cells[Piece.partOf(State)];
        Direction Facing = State.getValue(FACING);
        return Pos.relative(Facing.getClockWise(), -Cell[0]).below(Cell[1]).relative(Facing.getOpposite(), -Cell[2]);
    }

    public static void placeWhole(Level World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp3008FurnitureEBlock Piece) || !Piece.Type.multiblock()) {
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
        return Type.shape(partOf(State), State.getValue(FACING));
    }

    @Override
    public boolean seats(BlockState State) {
        return Type.Seat;
    }

    @Override
    public double seatHeight(BlockState State) {
        return Type.Seat ? Type.SeatTop : SeatBlock.super.seatHeight(State);
    }

    public static int lightOf(BlockState State, Scp3008FurnitureEKind Making) {
        if (Making.lamp() && State.hasProperty(Lit) && State.getValue(Lit)) {
            return Making.Lamp;
        }
        if (Making.screen() && State.hasProperty(On) && State.getValue(On)) {
            return Making.Screen;
        }
        return 0;
    }

    public static boolean switchedOn(BlockState State) {
        return State.hasProperty(On) && State.getValue(On) || State.hasProperty(Lit) && State.getValue(Lit);
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
                World.setBlock(Spot, Found.setValue(Flag, Value), Block.UPDATE_ALL);
            }
        }
    }

    protected static void play(Level World, BlockPos Pos, SoundEvent Event, float Volume, float Pitch) {
        World.playSound(null, Pos, Event, SoundSource.BLOCKS, Volume, Pitch);
    }

    public boolean toggle(Level World, BlockPos Pos, BlockState State) {
        BooleanProperty Flag = Type.lamp() ? Lit : Type.screen() ? On : null;
        if (Flag == null || !State.hasProperty(Flag)) {
            return false;
        }
        boolean Now = !State.getValue(Flag);
        setEverywhere(World, Pos, State, Flag, Now);
        play(World, Pos, Scp3008FurnitureERegistry.Click.get(), 0.5f, Now ? 1.25f : 1.0f);
        if (Type.lamp()) {
            play(World, Pos, Now ? Scp3008FurnitureERegistry.LampOn.get() : Scp3008FurnitureERegistry.LampOff.get(), 0.6f, 1.0f);
        } else {
            play(World, Pos, Now ? Scp3008FurnitureERegistry.PowerOn.get() : Scp3008FurnitureERegistry.PowerOff.get(), 0.45f, 0.95f + World.random.nextFloat() * 0.1f);
        }
        return true;
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
            return PropSeatEntity.sit(World, Pos, State, User) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (Type.lamp() || Type.switches(partOf(State))) {
            if (World.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return toggle(World, Pos, State) ? InteractionResult.CONSUME : InteractionResult.PASS;
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

    public String hint() {
        if (Type.Seat) {
            return "sit";
        }
        if (Type.lamp()) {
            return "lamp";
        }
        if (Type.storage() && Type.screen()) {
            return "both";
        }
        if (Type.storage()) {
            return "open";
        }
        return Type.screen() ? "switch" : null;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        String Hint = hint();
        if (Hint != null) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.scp3008_furne." + Hint).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
