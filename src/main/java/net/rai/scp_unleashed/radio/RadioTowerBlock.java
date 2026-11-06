package net.rai.scp_unleashed.radio;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RadioTowerBlock extends BaseEntityBlock {
    public static final MapCodec<RadioTowerBlock> BlockCodec = simpleCodec(RadioTowerBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Powered = BooleanProperty.create("powered");
    public static final int PoweredLight = 4;
    public static final VoxelShape Body = Shapes.or(Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0), Block.box(1.0, 4.0, 1.0, 15.0, 14.0, 15.0),
            Block.box(3.0, 14.0, 3.0, 13.0, 16.0, 13.0));

    public RadioTowerBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Powered, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Powered);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Body;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld && World.getBlockEntity(Pos) instanceof RadioTowerBlockEntity Tower) {
            Tower.removed(ServerWorld);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!RadioCarry.isRadio(Stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide && User instanceof ServerPlayer Server && World.getBlockEntity(Pos) instanceof RadioTowerBlockEntity Tower) {
            Tower.tune(Server, RadioCarry.channel(Stack));
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && User instanceof ServerPlayer Server && World.getBlockEntity(Pos) instanceof RadioTowerBlockEntity Tower) {
            Server.displayClientMessage(Tower.summary(), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.radio_booster_tower.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            RadioNetwork.Ranges Limits = VoiceConfig.ranges();
            Lines.add(Component.translatable("block.scp_unleashed.radio_booster_tower.masts", RadioNetwork.MaxMasts).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.radio_booster_tower.reach", (int) Limits.coverage(1), (int) Limits.coverage(RadioNetwork.MaxMasts)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.radio_booster_tower.link", (int) Limits.link(1), (int) Limits.link(RadioNetwork.MaxMasts)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.radio_booster_tower.draw", VoiceConfig.demand(1), VoiceConfig.demand(RadioNetwork.MaxMasts)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.radio_booster_tower.carrier").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new RadioTowerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, RadioRegistry.TowerEntity.get(), RadioTowerBlockEntity::serverTick);
    }
}
