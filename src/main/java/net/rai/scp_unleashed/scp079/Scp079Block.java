package net.rai.scp_unleashed.scp079;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.door.Screwdrivable;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp079Block extends BaseEntityBlock implements LargeBlock, WireHostBlock, Screwdrivable {
    public static final MapCodec<Scp079Block> BlockCodec = simpleCodec(Scp079Block::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, 1);
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;
    public static final BooleanProperty Upgraded = BooleanProperty.create("upgraded");
    public static final int MasterPart = 0;
    public static final int ScreenPart = 1;
    public static final int CellCount = 2;
    public static final float RunningBreakFactor = 0.5f;
    public static final double[][] Boxes = {
            {0, 0, 2, 10, 15, 15},
            {11, 1, 6, 27, 15, 13},
            {14, 0, 8, 24, 1, 12},
            {13, 3, 13, 25, 13, 16},
            {11, 0, 0, 31, 2, 5},
            {28, 0, 7, 31, 3, 14}
    };
    private static final VoxelShape[] CellShapes = new VoxelShape[4 * CellCount];
    private static final VoxelShape[] SupportShapes = new VoxelShape[4 * CellCount];
    private static boolean Dismantling;

    static {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            for (int Index = 0; Index < CellCount; Index++) {
                VoxelShape Shape = Shapes.empty();
                for (double[] Box : Boxes) {
                    double Low = Index * 16.0;
                    double X0 = Math.max(Box[0], Low);
                    double X1 = Math.min(Box[3], Low + 16.0);
                    if (X1 <= X0) {
                        continue;
                    }
                    Shape = Shapes.or(Shape, turned(Front, Low + 16.0 - X1, Box[1], Box[2], Low + 16.0 - X0, Box[4], Box[5]));
                }
                Shape = Shape.optimize();
                CellShapes[slot(Front, Index)] = Shape;
                SupportShapes[slot(Front, Index)] = Index == MasterPart ? Shapes.or(Shape, turned(Front, 15.0, 0.0, 0.0, 16.0, 16.0, 16.0)).optimize() : Shape;
            }
        }
    }

    public Scp079Block(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, MasterPart).setValue(Powered, false).setValue(Upgraded, false));
    }

    public static int lightOf(BlockState State) {
        if (!State.getValue(Powered)) {
            return 0;
        }
        return State.getValue(Part) == ScreenPart ? 6 : 3;
    }

    private static int slot(Direction Front, int Index) {
        return Front.get2DDataValue() * CellCount + Index;
    }

    private static VoxelShape turned(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Front) {
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    public static Direction rightOf(Direction Front) {
        return Front.getCounterClockWise();
    }

    public static Direction inletSide(Direction Front) {
        return Front.getClockWise();
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Index) {
        return Index == MasterPart ? Origin : Origin.relative(rightOf(Front));
    }

    public static BlockPos originOf(BlockPos CellPos, BlockState State) {
        return State.getValue(Part) == MasterPart ? CellPos : CellPos.relative(rightOf(State.getValue(Facing)).getOpposite());
    }

    @Nullable
    public static Scp079BlockEntity masterEntity(Level World, BlockPos CellPos, BlockState State) {
        if (!(State.getBlock() instanceof Scp079Block)) {
            return null;
        }
        return World.getBlockEntity(originOf(CellPos, State)) instanceof Scp079BlockEntity Computer ? Computer : null;
    }

    public static Vec3 screenCentre(BlockPos Origin, Direction Front) {
        Direction Right = rightOf(Front);
        Vec3 Base = Vec3.atBottomCenterOf(Origin);
        double Across = (19.0 - 8.0) / 16.0;
        double Forward = (8.0 - 7.0) / 16.0;
        return Base.add(Right.getStepX() * Across + Front.getStepX() * Forward, 8.5 / 16.0, Right.getStepZ() * Across + Front.getStepZ() * Forward);
    }

    public static void setPowered(Level World, BlockPos Origin, BlockState OriginState, boolean Lit) {
        Direction Front = OriginState.getValue(Facing);
        for (int Index = 0; Index < CellCount; Index++) {
            BlockPos Cell = cellPos(Origin, Front, Index);
            BlockState Current = World.getBlockState(Cell);
            if (Current.getBlock() instanceof Scp079Block && Current.getValue(Part) == Index && Current.getValue(Powered) != Lit) {
                World.setBlock(Cell, Current.setValue(Powered, Lit), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        return List.of(Context.getClickedPos(), cellPos(Context.getClickedPos(), Front, ScreenPart));
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return originOf(Pos, State);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part, Powered, Upgraded);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return CellShapes[slot(State.getValue(Facing), State.getValue(Part))];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return CellShapes[slot(State.getValue(Facing), State.getValue(Part))];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return SupportShapes[slot(State.getValue(Facing), State.getValue(Part))];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState State) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, MasterPart);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        World.setBlock(cellPos(Pos, State.getValue(Facing), ScreenPart), State.setValue(Part, ScreenPart), Block.UPDATE_ALL);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            Dismantling = true;
            try {
                BlockPos Origin = originOf(Pos, State);
                Direction Front = State.getValue(Facing);
                if (State.getValue(Part) == MasterPart && World.getBlockEntity(Pos) instanceof Scp079BlockEntity Computer) {
                    Computer.dropUpgrade(World, Pos);
                    Computer.powerDown();
                }
                for (int Index = 0; Index < CellCount; Index++) {
                    BlockPos Cell = cellPos(Origin, Front, Index);
                    if (Cell.equals(Pos)) {
                        continue;
                    }
                    BlockState CellState = World.getBlockState(Cell);
                    if (CellState.is(this) && CellState.getValue(Part) == Index && CellState.getValue(Facing) == Front) {
                        if (Index == MasterPart && World.getBlockEntity(Cell) instanceof Scp079BlockEntity Computer) {
                            Computer.dropUpgrade(World, Cell);
                            Computer.powerDown();
                        }
                        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            } finally {
                Dismantling = false;
            }
            if (World instanceof ServerLevel ServerWorld) {
                WireNetworkManager.get(ServerWorld).markDirty();
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected float getDestroyProgress(BlockState State, Player User, BlockGetter World, BlockPos Pos) {
        if (!State.canHarvestBlock(World, Pos, User)) {
            return 0.0f;
        }
        float Progress = super.getDestroyProgress(State, User, World, Pos);
        if (!State.getValue(Powered)) {
            return Progress;
        }
        if (World instanceof ServerLevel ServerWorld) {
            Scp079BlockEntity Computer = masterEntity(ServerWorld, Pos, State);
            if (Computer != null) {
                Computer.protest(ServerWorld);
            }
        }
        return Progress * RunningBreakFactor;
    }

    @Override
    protected void attack(BlockState State, Level World, BlockPos Pos, Player User) {
        if (State.getValue(Powered) && World instanceof ServerLevel ServerWorld) {
            Scp079BlockEntity Computer = masterEntity(World, Pos, State);
            if (Computer != null && Computer.isPowered() && State.canHarvestBlock(World, Pos, User)) {
                Computer.protest(ServerWorld);
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.resists"), true);
            }
        }
        super.attack(State, World, Pos, User);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && State.getValue(Part) != MasterPart && !User.isCreative() && State.canHarvestBlock(World, Pos, User)) {
            BlockPos Origin = originOf(Pos, State);
            if (isPartner(World.getBlockState(Origin), State, MasterPart)) {
                World.destroyBlock(Origin, true, User);
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState State, LootParams.Builder Params) {
        if (State.getValue(Part) == MasterPart) {
            return super.getDrops(State, Params);
        }
        Vec3 Spot = Params.getOptionalParameter(LootContextParams.ORIGIN);
        if (Spot == null) {
            return List.of();
        }
        ServerLevel World = Params.getLevel();
        BlockPos Origin = originOf(BlockPos.containing(Spot), State);
        BlockState OriginState = World.getBlockState(Origin);
        if (!isPartner(OriginState, State, MasterPart)) {
            return List.of();
        }
        Params.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(Origin)).withOptionalParameter(LootContextParams.BLOCK_ENTITY, World.getBlockEntity(Origin));
        return super.getDrops(OriginState, Params);
    }

    private boolean isPartner(BlockState Other, BlockState State, int Index) {
        return Other.is(this) && Other.getValue(Part) == Index && Other.getValue(Facing) == State.getValue(Facing);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (Stack.getItem() instanceof BlockItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(Stack, State, World, Pos, User, Hand, Hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        Scp079BlockEntity Computer = masterEntity(World, Pos, State);
        if (Computer == null) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide && User instanceof ServerPlayer Viewer) {
            Computer.openTerminal(Viewer);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        Scp079BlockEntity Computer = masterEntity(World, Pos, State);
        if (Computer == null || !(World instanceof ServerLevel ServerWorld)) {
            return InteractionResult.PASS;
        }
        if (Sneaking) {
            return pack(ServerWorld, State, Computer, User);
        }
        if (!Computer.isUpgraded()) {
            if (User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.no_upgrade"), true);
            }
            return InteractionResult.FAIL;
        }
        ItemStack Removed = Computer.removeUpgrade();
        give(World, Computer.getBlockPos(), User, Removed);
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.upgrade_removed", Removed.getHoverName()), true);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult pack(ServerLevel World, BlockState State, Scp079BlockEntity Computer, @Nullable Player User) {
        BlockPos Origin = Computer.getBlockPos();
        if (Computer.isPowered()) {
            refuse(World, State, Computer, User);
            return InteractionResult.FAIL;
        }
        ItemStack Upgrade = Computer.isUpgraded() ? Computer.removeUpgrade() : ItemStack.EMPTY;
        ItemStack Packed = Computer.toItem();
        Direction Front = State.getValue(Facing);
        Dismantling = true;
        try {
            for (int Index = CellCount - 1; Index >= 0; Index--) {
                BlockPos Cell = cellPos(Origin, Front, Index);
                BlockState CellState = World.getBlockState(Cell);
                if (CellState.is(this)) {
                    World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        } finally {
            Dismantling = false;
        }
        WireNetworkManager.get(World).markDirty();
        World.playSound(null, Origin, Scp079Registry.Pack.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        give(World, Origin, User, Packed);
        give(World, Origin, User, Upgrade);
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.packed"), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static void refuse(ServerLevel World, BlockState State, Scp079BlockEntity Computer, @Nullable Player User) {
        BlockPos Origin = Computer.getBlockPos();
        Vec3 Centre = screenCentre(Origin, State.getValue(Facing));
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Centre.x, Centre.y, Centre.z, 10, 0.3, 0.2, 0.3, 0.12);
        World.playSound(null, Origin, Scp079Registry.Refuse.get(), SoundSource.BLOCKS, 0.9f, 0.9f + World.getRandom().nextFloat() * 0.2f);
        Computer.refuseMove();
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.refuses"), true);
        }
    }

    public static void give(Level World, BlockPos Pos, @Nullable Player User, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return;
        }
        if (User != null) {
            ItemHandlerHelper.giveItemToPlayer(User, Stack);
        } else {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Stack);
        }
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == MasterPart ? new Scp079BlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != MasterPart) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, Scp079Registry.ComputerEntity.get(), Scp079BlockEntity::clientTick)
                : createTickerHelper(Type, Scp079Registry.ComputerEntity.get(), Scp079BlockEntity::serverTick);
    }
}
