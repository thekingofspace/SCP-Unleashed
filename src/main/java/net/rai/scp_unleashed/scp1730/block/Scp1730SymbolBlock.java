package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.decal.DecalRegistry;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class Scp1730SymbolBlock extends BaseEntityBlock {
    public static final MapCodec<Scp1730SymbolBlock> SymbolCodec = simpleCodec(Scp1730SymbolBlock::new);
    public static final int GlyphCount = 12;
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Glyph = IntegerProperty.create("glyph", 0, GlyphCount - 1);
    private static final Map<Direction, VoxelShape> Thin = new EnumMap<>(Direction.class);

    static {
        Thin.put(Direction.NORTH, Block.box(0, 0, 15, 16, 16, 16));
        Thin.put(Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 1));
        Thin.put(Direction.WEST, Block.box(15, 0, 0, 16, 16, 16));
        Thin.put(Direction.EAST, Block.box(0, 0, 0, 1, 16, 16));
    }

    public Scp1730SymbolBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Glyph, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return SymbolCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Glyph);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp1730SymbolBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Face = Context.getClickedFace();
        if (!Face.getAxis().isHorizontal()) {
            Face = Context.getHorizontalDirection().getOpposite();
        }
        BlockState Made = defaultBlockState().setValue(Facing, Face).setValue(Glyph, Context.getLevel().getRandom().nextInt(GlyphCount));
        return Made.canSurvive(Context.getLevel(), Context.getClickedPos()) ? Made : null;
    }

    public static BlockPos wallOf(BlockPos Pos, Direction Facing) {
        return Pos.relative(Facing.getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        Direction Side = State.getValue(Facing);
        BlockPos Wall = wallOf(Pos, Side);
        return World.getBlockState(Wall).isFaceSturdy(World, Wall, Side);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        return Side == State.getValue(Facing).getOpposite() && !State.canSurvive(World, Pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Thin.get(State.getValue(Facing));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return true;
    }

    public static boolean cleans(ItemStack Stack) {
        return Stack.is(DecalRegistry.DecalScraper.get()) || Stack.is(Items.WET_SPONGE) || Stack.is(Items.SPONGE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!cleans(Stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server) {
            scrub(Server, Pos, State);
            if (Stack.isDamageableItem()) {
                Stack.hurtAndBreak(1, User, Hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    public static void scrub(ServerLevel World, BlockPos Pos, BlockState State) {
        World.removeBlock(Pos, false);
        World.playSound(null, Pos, Scp1730Registry.Scrub.get(), SoundSource.BLOCKS, 1.0f, 0.9f + World.random.nextFloat() * 0.2f);
        Direction Side = State.getValue(Facing);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACK_CONCRETE_POWDER.defaultBlockState()),
                Pos.getX() + 0.5 - Side.getStepX() * 0.4, Pos.getY() + 0.5, Pos.getZ() + 0.5 - Side.getStepZ() * 0.4, 12, 0.25, 0.3, 0.25, 0.02);
    }
}
