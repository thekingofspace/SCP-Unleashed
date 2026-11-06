package net.rai.scp_unleashed.scp106;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp106DoorwayBlock extends Scp106PocketBlock implements LargeBlock {
    public static final MapCodec<Scp106DoorwayBlock> DoorwayCodec = simpleCodec(Scp106DoorwayBlock::new);
    public static final EnumProperty<Direction.Axis> Axis = BlockStateProperties.HORIZONTAL_AXIS;
    public static final EnumProperty<Scp106DoorwayPart> Part = EnumProperty.create("part", Scp106DoorwayPart.class);
    public static final int Rows = 3;
    public static final int Settled = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape AlongX = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape AlongZ = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public Scp106DoorwayBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Axis, Direction.Axis.X).setValue(Part, Scp106DoorwayPart.Single));
    }

    @Override
    protected MapCodec<? extends Scp106PocketBlock> codec() {
        return DoorwayCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Axis, Part);
    }

    public BlockState piece(Direction.Axis Along, Scp106DoorwayPart Piece) {
        return defaultBlockState().setValue(Axis, Along).setValue(Part, Piece);
    }

    public static BlockPos lowerPos(BlockState State, BlockPos Pos) {
        return Pos.below(State.getValue(Part).getRow());
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above(), Pos.above(2));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return piece(Context.getHorizontalDirection().getClockWise().getAxis(), Scp106DoorwayPart.Lower);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (State.getValue(Part) != Scp106DoorwayPart.Lower) {
            return;
        }
        for (int Row = 1; Row < Rows; Row++) {
            World.setBlock(Pos.above(Row), State.setValue(Part, Scp106DoorwayPart.atRow(Row)), Block.UPDATE_ALL);
        }
    }

    private boolean partnered(BlockState State, BlockState Neighbor, int Row) {
        return Neighbor.is(this) && Neighbor.getValue(Part).isTall() && Neighbor.getValue(Part).getRow() == Row && Neighbor.getValue(Axis) == State.getValue(Axis);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbor, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        Scp106DoorwayPart Piece = State.getValue(Part);
        if (Piece.isTall() && Side.getAxis() == Direction.Axis.Y) {
            int Row = Piece.getRow() + (Side == Direction.UP ? 1 : -1);
            if (Row >= 0 && Row < Rows && !partnered(State, Neighbor, Row)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(State, Side, Neighbor, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        Scp106DoorwayPart Piece = State.getValue(Part);
        if (!World.isClientSide && User.isCreative() && Piece.isTall() && Piece != Scp106DoorwayPart.Lower) {
            BlockPos Foot = lowerPos(State, Pos);
            BlockState FootState = World.getBlockState(Foot);
            if (FootState.is(this) && FootState.getValue(Part) == Scp106DoorwayPart.Lower) {
                World.setBlock(Foot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, Foot, Block.getId(FootState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    private static boolean isLegacy(BlockState State, Direction.Axis Along) {
        return State.getBlock() instanceof Scp106DoorwayBlock && State.getValue(Part) == Scp106DoorwayPart.Single && State.getValue(Axis) == Along;
    }

    public boolean migrate(LevelAccessor World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!State.is(this) || State.getValue(Part) != Scp106DoorwayPart.Single) {
            return false;
        }
        Direction.Axis Along = State.getValue(Axis);
        BlockPos Bottom = Pos;
        for (int Step = 0; Step < Rows && isLegacy(World.getBlockState(Bottom.below()), Along); Step++) {
            Bottom = Bottom.below();
        }
        int Run = 0;
        while (Run < Rows && isLegacy(World.getBlockState(Bottom.above(Run)), Along)) {
            Run++;
        }
        if (Run < Rows - 1) {
            return false;
        }
        BlockPos Top = Bottom.above(Rows - 1);
        if (Run < Rows && (World.isOutsideBuildHeight(Top) || !World.getBlockState(Top).isAir())) {
            return false;
        }
        for (int Row = 0; Row < Rows; Row++) {
            World.setBlock(Bottom.above(Row), piece(Along, Scp106DoorwayPart.atRow(Row)), Settled);
        }
        return true;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return State.getValue(Part) == Scp106DoorwayPart.Single;
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        migrate(World, Pos);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return switch (Turn) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> State.setValue(Axis, State.getValue(Axis) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            default -> State;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Axis) == Direction.Axis.X ? AlongX : AlongZ;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Visitor) {
        if (World instanceof ServerLevel ServerWorld && Visitor instanceof LivingEntity Living && Living.isAlive() && Scp106Pocket.inPocket(ServerWorld)) {
            Scp106Doorways.queue(ServerWorld, Pos, Living);
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(State.getValue(Part).isTall() ? 8 : 6) != 0) {
            return;
        }
        boolean Along = State.getValue(Axis) == Direction.Axis.X;
        double X = Pos.getX() + (Along ? Random.nextDouble() : 0.5 + (Random.nextBoolean() ? 0.32 : -0.32));
        double Z = Pos.getZ() + (Along ? 0.5 + (Random.nextBoolean() ? 0.32 : -0.32) : Random.nextDouble());
        World.addParticle(Scp106Registry.Goo.get(), X, Pos.getY() + Random.nextDouble(), Z, 0.0, -0.01, 0.0);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp106_doorway.tooltip").withStyle(ChatFormatting.DARK_GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp106_doorway.size").withStyle(ChatFormatting.GOLD));
        }
    }
}
