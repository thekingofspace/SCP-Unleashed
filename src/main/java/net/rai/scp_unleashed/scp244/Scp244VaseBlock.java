package net.rai.scp_unleashed.scp244;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
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

import java.util.List;

public class Scp244VaseBlock extends BaseEntityBlock {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final float BoundResistance = 3600000.0f;

    private final Scp244Style Style;
    private final MapCodec<Scp244VaseBlock> BlockCodec;

    public Scp244VaseBlock(Scp244Style Style, Properties Settings) {
        super(Settings);
        this.Style = Style;
        this.BlockCodec = simpleCodec(Next -> new Scp244VaseBlock(Style, Next));
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false));
    }

    public Scp244Style style() {
        return Style;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Style.shape(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof Scp244VaseBlockEntity Vase) {
            Vase.toggle(User);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static void setOpen(Level World, BlockPos Pos, boolean Value) {
        BlockState State = World.getBlockState(Pos);
        if (State.getBlock() instanceof Scp244VaseBlock) {
            World.setBlock(Pos, State.setValue(Open, Value), Block.UPDATE_ALL);
        }
    }

    public static boolean bound(BlockGetter World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof Scp244VaseBlockEntity Vase && Vase.bound();
    }

    @Override
    protected float getDestroyProgress(BlockState State, Player User, BlockGetter World, BlockPos Pos) {
        if (bound(World, Pos)) {
            return 0.0f;
        }
        return super.getDestroyProgress(State, User, World, Pos);
    }

    @Override
    public float getExplosionResistance(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return bound(World, Pos) ? BoundResistance : super.getExplosionResistance(State, World, Pos, Blast);
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
        Lines.add(Component.translatable("block.scp_unleashed." + Style.id() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp244_vase.lid").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp244_vase.fog").withStyle(ChatFormatting.AQUA));
            Lines.add(Component.translatable("block.scp_unleashed.scp244_vase.bound").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp244_vase.set").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp244VaseBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide) {
            return createTickerHelper(Type, Scp244Registry.VaseEntity.get(), Scp244VaseBlockEntity::clientTick);
        }
        return createTickerHelper(Type, Scp244Registry.VaseEntity.get(), Scp244VaseBlockEntity::serverTick);
    }
}
