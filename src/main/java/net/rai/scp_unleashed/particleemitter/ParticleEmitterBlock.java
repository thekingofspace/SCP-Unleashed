package net.rai.scp_unleashed.particleemitter;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ParticleEmitterBlock extends BaseEntityBlock {
    public static final MapCodec<ParticleEmitterBlock> BlockCodec = simpleCodec(ParticleEmitterBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.FACING;
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;
    public static final double Depth = 3.0;
    public static final double Inset = 1.0;
    private static final Map<Direction, VoxelShape> ShapeByFacing = buildShapes();

    public ParticleEmitterBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.UP).setValue(Powered, false));
    }

    private static Map<Direction, VoxelShape> buildShapes() {
        Map<Direction, VoxelShape> ByFacing = new EnumMap<>(Direction.class);
        double Far = 16.0 - Inset;
        ByFacing.put(Direction.UP, Block.box(Inset, 0.0, Inset, Far, Depth, Far));
        ByFacing.put(Direction.DOWN, Block.box(Inset, 16.0 - Depth, Inset, Far, 16.0, Far));
        ByFacing.put(Direction.NORTH, Block.box(Inset, Inset, 16.0 - Depth, Far, Far, 16.0));
        ByFacing.put(Direction.SOUTH, Block.box(Inset, Inset, 0.0, Far, Far, Depth));
        ByFacing.put(Direction.WEST, Block.box(16.0 - Depth, Inset, Inset, 16.0, Far, Far));
        ByFacing.put(Direction.EAST, Block.box(0.0, Inset, Inset, Depth, Far, Far));
        return ByFacing;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Powered);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getClickedFace()).setValue(Powered, Context.getLevel().hasNeighborSignal(Context.getClickedPos()));
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByFacing.get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (World.isClientSide) {
            return;
        }
        boolean Signal = World.hasNeighborSignal(Pos);
        if (Signal != State.getValue(Powered)) {
            World.setBlock(Pos, State.setValue(Powered, Signal), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        return Stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof ParticleEmitterBlockEntity Emitter) {
            EmitterMode Next = Emitter.cycleMode();
            World.playSound(null, Pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3f, 1.6f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.particle_emitter.mode",
                    Component.translatable(Next.translationKey()).withStyle(ChatFormatting.AQUA)), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Key = getDescriptionId();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".mount").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + ".modes").withStyle(ChatFormatting.GOLD));
            for (EmitterMode Each : EmitterMode.values()) {
                Lines.add(Component.translatable(Key + ".mode_line", Component.translatable(Each.translationKey()).withStyle(ChatFormatting.AQUA),
                        Component.translatable(Each.translationKey() + ".detail")).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ParticleEmitterBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, ParticleEmitterRegistry.EmitterEntity.get(), ParticleEmitterBlockEntity::clientTick) : null;
    }
}
