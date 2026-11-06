package net.rai.scp_unleashed.scp256;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Scp256TypewriterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<Scp256TypewriterBlock> BlockCodec = simpleCodec(Scp256TypewriterBlock::new);
    public static final String Key = "block.scp_unleashed.scp256_typewriter.";
    private static final Map<Direction, VoxelShape> Shapes = PropShapes.horizontal(PropShapes.of(
            PropShapes.box(1, 0, 2, 15, 4, 11),
            PropShapes.box(1, 0, 11, 15, 8, 14.5),
            PropShapes.box(0.5, 8, 12.25, 15.5, 11.5, 15),
            PropShapes.box(2.5, 11.5, 12.75, 13.5, 16, 16)));

    public Scp256TypewriterBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(FACING));
    }

    @Nullable
    public static Scp256TypewriterBlockEntity typewriterAt(BlockGetter World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof Scp256TypewriterBlockEntity Typewriter ? Typewriter : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        Scp256TypewriterBlockEntity Typewriter = typewriterAt(World, Pos);
        if (Typewriter == null) {
            return InteractionResult.PASS;
        }
        if (Typewriter.isTyping(World.getGameTime())) {
            return InteractionResult.CONSUME;
        }
        if (World instanceof ServerLevel ServerWorld) {
            return Typewriter.press(ServerWorld, User) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "patience").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp256TypewriterBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (!World.isClientSide || Type != Scp256Registry.TypewriterEntity.get()) {
            return null;
        }
        return (Where, At, Current, Holder) -> Scp256TypewriterBlockEntity.clientTick(Where, At, Current, (Scp256TypewriterBlockEntity) Holder);
    }
}
