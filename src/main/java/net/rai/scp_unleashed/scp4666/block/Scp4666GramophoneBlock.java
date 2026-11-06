package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Chores;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666State;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Scp4666GramophoneBlock extends BaseEntityBlock {
    public static final MapCodec<Scp4666GramophoneBlock> BlockCodec = simpleCodec(Scp4666GramophoneBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Playing = BooleanProperty.create("playing");
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{2, 0, 2, 14, 15, 14}});

    public Scp4666GramophoneBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Playing, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Playing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(Facing));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (World.getBlockEntity(Pos) instanceof Scp4666GramophoneBlockEntity Player) {
            if (Player.playing()) {
                Player.stop();
            } else {
                Player.start(false);
                if (World instanceof ServerLevel Server && User instanceof ServerPlayer Visitor && Scp4666Chores.active(Server, Visitor)) {
                    Scp4666State Data = Scp4666State.get(Server.getServer());
                    String Key = "music:" + Visitor.getUUID();
                    if (!Data.daily(Key)) {
                        Data.markDaily(Key);
                        Scp4666Chores.perform(Server, Visitor, Scp4666Chores.Chore.Music);
                    }
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp4666GramophoneBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp4666Registry.GramophoneEntity.get(), Scp4666GramophoneBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_gramophone.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
