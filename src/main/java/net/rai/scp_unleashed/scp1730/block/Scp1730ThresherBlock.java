package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.scp1730.Scp1730Collapse;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import org.jetbrains.annotations.Nullable;

public class Scp1730ThresherBlock extends BaseEntityBlock {
    public static final MapCodec<Scp1730ThresherBlock> ThresherCodec = simpleCodec(Scp1730ThresherBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;

    public Scp1730ThresherBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return ThresherCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp1730ThresherBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp1730Registry.ThresherEntity.get(), Scp1730ThresherBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(World.getBlockEntity(Pos) instanceof Scp1730ThresherBlockEntity Thresher) || Thresher.pulled()) {
            return InteractionResult.CONSUME;
        }
        if (!Scp1730Collapse.canPull((ServerLevel) World, Pos)) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.thresher_guarded").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        Scp1730Collapse.pull((ServerLevel) World, Pos, Thresher, (ServerPlayer) User);
        return InteractionResult.CONSUME;
    }
}
