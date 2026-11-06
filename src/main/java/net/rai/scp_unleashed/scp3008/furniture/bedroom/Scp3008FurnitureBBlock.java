package net.rai.scp_unleashed.scp3008.furniture.bedroom;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.SeatBlock;

import javax.annotation.Nullable;
import java.util.List;

public class Scp3008FurnitureBBlock extends HorizontalDirectionalBlock implements SeatBlock, EntityBlock {
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final BooleanProperty Filled = BooleanProperty.create("filled");
    public static final BooleanProperty Lowered = BooleanProperty.create("lowered");
    public static final IntegerProperty PartOfTwo = IntegerProperty.create("part", 0, 1);
    public static final IntegerProperty PartOfFour = IntegerProperty.create("part", 0, 3);
    public static final int LampLight = 13;
    public static final double LowSupportMin = 0.7;
    public static final double LowSupportMax = 0.86;
    private static final ThreadLocal<Scp3008FurnitureBPiece> Pending = new ThreadLocal<>();
    public static final MapCodec<Scp3008FurnitureBBlock> BlockCodec = simpleCodec(Settings -> make(Scp3008FurnitureBPiece.Nightstand, Settings));

    private final Scp3008FurnitureBPiece Piece;

    public static Scp3008FurnitureBBlock make(Scp3008FurnitureBPiece Piece, Properties Settings) {
        Pending.set(Piece);
        try {
            return new Scp3008FurnitureBBlock(Settings, Piece);
        } finally {
            Pending.remove();
        }
    }

    protected Scp3008FurnitureBBlock(Properties Settings, Scp3008FurnitureBPiece Piece) {
        super(Settings);
        this.Piece = Piece;
        BlockState State = stateDefinition.any().setValue(FACING, Direction.NORTH);
        IntegerProperty Part = partProperty(Piece);
        if (Part != null) {
            State = State.setValue(Part, 0);
        }
        if (State.hasProperty(Open)) {
            State = State.setValue(Open, false);
        }
        if (State.hasProperty(Lit)) {
            State = State.setValue(Lit, false).setValue(Lowered, false);
        }
        if (State.hasProperty(Filled)) {
            State = State.setValue(Filled, false);
        }
        registerDefaultState(State);
    }

    @Nullable
    public static IntegerProperty partProperty(Scp3008FurnitureBPiece Piece) {
        return switch (Piece.parts()) {
            case 2 -> PartOfTwo;
            case 4 -> PartOfFour;
            default -> null;
        };
    }

    public Scp3008FurnitureBPiece piece() {
        return Piece;
    }

    public int part(BlockState State) {
        IntegerProperty Part = partProperty(Piece);
        return Part == null || !State.hasProperty(Part) ? 0 : State.getValue(Part);
    }

    public BlockState withPart(BlockState State, int Part) {
        IntegerProperty Property = partProperty(Piece);
        return Property == null ? State : State.setValue(Property, Part);
    }

    public BlockPos offset(Direction Facing, int Part) {
        Direction Side = Facing.getClockWise();
        Direction Back = Facing.getOpposite();
        int X = Piece.cellX(Part);
        int Y = Piece.cellY(Part);
        int Z = Piece.cellZ(Part);
        return new BlockPos(Side.getStepX() * X + Back.getStepX() * Z, Y, Side.getStepZ() * X + Back.getStepZ() * Z);
    }

    public BlockPos main(BlockPos Pos, BlockState State) {
        return Pos.subtract(offset(State.getValue(FACING), part(State)));
    }

    public BlockPos partPos(BlockPos Main, Direction Facing, int Part) {
        return Main.offset(offset(Facing, Part));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
        Scp3008FurnitureBPiece Kind = Pending.get();
        if (Kind == null) {
            return;
        }
        IntegerProperty Part = partProperty(Kind);
        if (Part != null) {
            Builder.add(Part);
        }
        if (Kind.Opens != Scp3008FurnitureBPiece.Opening.None) {
            Builder.add(Open);
        }
        if (Kind.Action == Scp3008FurnitureBPiece.Trick.Lamp) {
            Builder.add(Lit, Lowered);
        }
        if (Kind.Action == Scp3008FurnitureBPiece.Trick.Bath) {
            Builder.add(Filled);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Facing = Context.getHorizontalDirection().getOpposite();
        if (Piece.Placement == Scp3008FurnitureBPiece.Mount.Wall && Context.getClickedFace().getAxis().isHorizontal()) {
            Facing = Context.getClickedFace();
        }
        BlockState State = defaultBlockState().setValue(FACING, Facing);
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        for (int Part = 1; Part < Piece.parts(); Part++) {
            BlockPos Spot = partPos(Pos, Facing, Part);
            if (!World.isInWorldBounds(Spot) || !World.getWorldBorder().isWithinBounds(Spot) || !World.getBlockState(Spot).canBeReplaced(Context)) {
                return null;
            }
        }
        if (State.hasProperty(Lowered)) {
            State = State.setValue(Lowered, lowSupport(World, Pos.below()));
        }
        return State;
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (!World.isClientSide && Piece.multi() && part(State) == 0) {
            spread(World, Pos, State);
        }
    }

    public static boolean fits(LevelReader World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp3008FurnitureBBlock Furniture)) {
            return false;
        }
        Direction Facing = State.getValue(FACING);
        for (int Part = 1; Part < Furniture.Piece.parts(); Part++) {
            BlockPos Spot = Furniture.partPos(Pos, Facing, Part);
            BlockState There = World.getBlockState(Spot);
            if (World.isOutsideBuildHeight(Spot) || !(There.isAir() || There.canBeReplaced() || There.is(State.getBlock()))) {
                return false;
            }
        }
        return true;
    }

    public static void placeWhole(Level World, BlockPos Pos, BlockState State) {
        placeWhole((LevelAccessor) World, Pos, State);
    }

    public static void placeWhole(LevelAccessor World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp3008FurnitureBBlock Furniture)) {
            return;
        }
        BlockState Main = Furniture.withPart(State, 0);
        if (Main.hasProperty(Lowered)) {
            Main = Main.setValue(Lowered, lowSupport(World, Pos.below()));
        }
        if (!fits(World, Pos, Main)) {
            if (World.getBlockState(Pos).is(Furniture)) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            return;
        }
        if (!World.getBlockState(Pos).equals(Main)) {
            World.setBlock(Pos, Main, Block.UPDATE_ALL);
        }
        Furniture.spread(World, Pos, Main);
    }

    private void spread(LevelAccessor World, BlockPos Main, BlockState State) {
        Direction Facing = State.getValue(FACING);
        for (int Part = 1; Part < Piece.parts(); Part++) {
            World.setBlock(partPos(Main, Facing, Part), withPart(State, Part), Block.UPDATE_ALL);
        }
    }

    public static boolean lowSupport(BlockGetter World, BlockPos Below) {
        BlockState Support = World.getBlockState(Below);
        if (Support.isAir()) {
            return false;
        }
        VoxelShape Shape = Support.getCollisionShape(World, Below);
        if (Shape.isEmpty()) {
            return false;
        }
        double Top = Shape.max(Direction.Axis.Y);
        return Top > LowSupportMin && Top < LowSupportMax;
    }

    private boolean siblingHolds(BlockState State, Direction Side, BlockState Neighbour) {
        Direction Facing = State.getValue(FACING);
        int Part = part(State);
        int X = Piece.cellX(Part);
        int Y = Piece.cellY(Part);
        int Z = Piece.cellZ(Part);
        if (Side == Direction.UP) {
            Y++;
        } else if (Side == Direction.DOWN) {
            Y--;
        } else if (Side == Facing.getClockWise()) {
            X++;
        } else if (Side == Facing.getCounterClockWise()) {
            X--;
        } else if (Side == Facing.getOpposite()) {
            Z++;
        } else {
            Z--;
        }
        int Expected = Piece.partAt(X, Y, Z);
        if (Expected < 0) {
            return true;
        }
        return Neighbour.is(this) && Neighbour.getValue(FACING) == Facing && part(Neighbour) == Expected;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Piece.multi() && !siblingHolds(State, Side, Neighbour)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (Side == Direction.DOWN && State.hasProperty(Lowered)) {
            return State.setValue(Lowered, lowSupport(World, NeighbourPos));
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    private boolean alternate(BlockState State) {
        return (State.hasProperty(Open) && State.getValue(Open)) || (State.hasProperty(Lowered) && State.getValue(Lowered));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Piece.shape(part(State), State.getValue(FACING), alternate(State));
    }

    @Override
    public boolean seats(BlockState State) {
        return Piece.seats();
    }

    @Nullable
    public Scp3008FurnitureBStorage storageAt(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!Piece.storage()) {
            return null;
        }
        return World.getBlockEntity(main(Pos, State)) instanceof Scp3008FurnitureBStorage Storage ? Storage : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (Piece.Action == Scp3008FurnitureBPiece.Trick.Toilet && User.isSecondaryUseActive()) {
            if (!World.isClientSide) {
                flush((ServerLevel) World, Pos);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (Piece.seats()) {
            return sit(State, World, Pos, User);
        }
        if (Piece.storage()) {
            if (World.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            Scp3008FurnitureBStorage Storage = storageAt(World, Pos, State);
            if (Storage != null) {
                User.openMenu(Storage);
            }
            return InteractionResult.CONSUME;
        }
        if (Piece.Action == Scp3008FurnitureBPiece.Trick.Lamp) {
            if (!World.isClientSide) {
                toggleLamp(World, Pos, State);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (Piece.Opens == Scp3008FurnitureBPiece.Opening.Glass) {
            if (!World.isClientSide) {
                setOpen(World, main(Pos, State), !State.getValue(Open));
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult sit(BlockState State, Level World, BlockPos Pos, Player User) {
        if (User.isSecondaryUseActive() || User.isPassenger() || PropSeatEntity.occupied(World, Pos)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (PropSeatEntity.sit(World, Pos, State, User)) {
            if (Piece.Action == Scp3008FurnitureBPiece.Trick.Chair) {
                play(World, Pos, Scp3008FurnitureBRegistry.ChairHiss.get(), Scp3008FurnitureBRegistry.ChairCreak.get());
            } else if (Piece.Action == Scp3008FurnitureBPiece.Trick.Bed) {
                play(World, Pos, Scp3008FurnitureBRegistry.BedSit.get());
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public void toggleLamp(Level World, BlockPos Pos, BlockState State) {
        boolean On = !State.getValue(Lit);
        World.setBlock(Pos, State.setValue(Lit, On), Block.UPDATE_ALL);
        if (On) {
            play(World, Pos, Scp3008FurnitureBRegistry.LampClick.get(), Scp3008FurnitureBRegistry.LampHum.get());
        } else {
            play(World, Pos, Scp3008FurnitureBRegistry.LampClick.get());
        }
        World.gameEvent(null, On ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, Pos);
    }

    private void flush(ServerLevel World, BlockPos Pos) {
        play(World, Pos, Scp3008FurnitureBRegistry.Flush.get(), Scp3008FurnitureBRegistry.FlushSwirl.get(), Scp3008FurnitureBRegistry.DoorLatch.get());
        World.sendParticles(ParticleTypes.BUBBLE_POP, Pos.getX() + 0.5, Pos.getY() + 0.6, Pos.getZ() + 0.45, 6, 0.15, 0.02, 0.15, 0.01);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (Piece.Action == Scp3008FurnitureBPiece.Trick.Sink && Stack.is(Items.GLASS_BOTTLE)) {
            if (!World.isClientSide) {
                User.setItemInHand(Hand, ItemUtils.createFilledResult(Stack, User, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
                play(World, Pos, Scp3008FurnitureBRegistry.Tap.get());
                World.gameEvent(null, GameEvent.FLUID_PICKUP, Pos);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (Piece.Action == Scp3008FurnitureBPiece.Trick.Bath) {
            boolean Full = State.getValue(Filled);
            if (!Full && Stack.is(Items.WATER_BUCKET)) {
                if (!World.isClientSide) {
                    User.setItemInHand(Hand, ItemUtils.createFilledResult(Stack, User, new ItemStack(Items.BUCKET)));
                    setAcross(World, main(Pos, State), Filled, true);
                    play(World, Pos, Scp3008FurnitureBRegistry.BathFill.get(), Scp3008FurnitureBRegistry.BathBubbles.get());
                    World.gameEvent(null, GameEvent.FLUID_PLACE, Pos);
                }
                return ItemInteractionResult.sidedSuccess(World.isClientSide);
            }
            if (Full && Stack.is(Items.BUCKET)) {
                if (!World.isClientSide) {
                    User.setItemInHand(Hand, ItemUtils.createFilledResult(Stack, User, new ItemStack(Items.WATER_BUCKET)));
                    setAcross(World, main(Pos, State), Filled, false);
                    play(World, Pos, Scp3008FurnitureBRegistry.Tap.get());
                    World.gameEvent(null, GameEvent.FLUID_PICKUP, Pos);
                }
                return ItemInteractionResult.sidedSuccess(World.isClientSide);
            }
            if (Full && Stack.is(Items.GLASS_BOTTLE)) {
                if (!World.isClientSide) {
                    User.setItemInHand(Hand, ItemUtils.createFilledResult(Stack, User, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
                    play(World, Pos, Scp3008FurnitureBRegistry.Tap.get());
                    World.gameEvent(null, GameEvent.FLUID_PICKUP, Pos);
                }
                return ItemInteractionResult.sidedSuccess(World.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public void setAcross(Level World, BlockPos Main, BooleanProperty Property, boolean Value) {
        BlockState MainState = World.getBlockState(Main);
        if (!MainState.is(this) || !MainState.hasProperty(Property)) {
            return;
        }
        Direction Facing = MainState.getValue(FACING);
        for (int Part = 0; Part < Piece.parts(); Part++) {
            BlockPos Spot = partPos(Main, Facing, Part);
            BlockState There = World.getBlockState(Spot);
            if (There.is(this) && There.getValue(Property) != Value) {
                World.setBlock(Spot, There.setValue(Property, Value), Block.UPDATE_CLIENTS);
            }
        }
    }

    public void setOpen(Level World, BlockPos Main, boolean Opened) {
        BlockState MainState = World.getBlockState(Main);
        if (!MainState.is(this) || !MainState.hasProperty(Open) || MainState.getValue(Open) == Opened) {
            return;
        }
        setAcross(World, Main, Open, Opened);
        switch (Piece.Opens) {
            case Doors -> play(World, Main, Opened ? Scp3008FurnitureBRegistry.DoorOpen.get() : Scp3008FurnitureBRegistry.DoorClose.get(), Scp3008FurnitureBRegistry.DoorLatch.get());
            case Drawers -> play(World, Main, Opened ? Scp3008FurnitureBRegistry.DrawerOpen.get() : Scp3008FurnitureBRegistry.DrawerClose.get(), Scp3008FurnitureBRegistry.DrawerSlide.get(),
                    Opened ? Scp3008FurnitureBRegistry.DrawerSlide.get() : Scp3008FurnitureBRegistry.DrawerKnock.get());
            case Lid -> play(World, Main, Opened ? Scp3008FurnitureBRegistry.LidOpen.get() : Scp3008FurnitureBRegistry.LidClose.get());
            case Glass -> play(World, Main, Opened ? Scp3008FurnitureBRegistry.GlassOpen.get() : Scp3008FurnitureBRegistry.GlassClose.get(), Scp3008FurnitureBRegistry.DoorLatch.get());
            default -> {
            }
        }
        World.gameEvent(null, Opened ? GameEvent.CONTAINER_OPEN : GameEvent.CONTAINER_CLOSE, Main);
    }

    private static void play(Level World, BlockPos Pos, SoundEvent... Layers) {
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        for (SoundEvent Layer : Layers) {
            World.playSound(null, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Layer, SoundSource.BLOCKS, 1.0f, Pitch);
        }
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (World.getBlockEntity(Pos) instanceof Scp3008FurnitureBStorage Storage) {
            Storage.recheckOpen();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean Moved) {
        if (!State.is(NewState.getBlock())) {
            if (Piece.seats() && !World.isClientSide) {
                PropSeatEntity.release(World, Pos);
            }
            if (Piece.storage() && part(State) == 0) {
                Containers.dropContentsOnDestroy(State, NewState, World, Pos);
            }
            if (Piece.multi() && !World.isClientSide) {
                removeSiblings(World, Pos, State);
            }
        }
        super.onRemove(State, World, Pos, NewState, Moved);
    }

    private void removeSiblings(Level World, BlockPos Pos, BlockState State) {
        Direction Facing = State.getValue(FACING);
        BlockPos Main = main(Pos, State);
        for (int Part = 0; Part < Piece.parts(); Part++) {
            BlockPos Spot = partPos(Main, Facing, Part);
            if (Spot.equals(Pos)) {
                continue;
            }
            BlockState There = World.getBlockState(Spot);
            if (There.is(this) && There.getValue(FACING) == Facing && part(There) == Part) {
                World.setBlock(Spot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return Piece.storage() && part(State) == 0 ? new Scp3008FurnitureBStorage(Pos, State) : null;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(FACING, Turn.rotate(State.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        if (Piece.Wide > 1) {
            return State;
        }
        return State.rotate(Flip.getRotation(State.getValue(FACING)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
