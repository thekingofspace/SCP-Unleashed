package net.rai.scp_unleashed.scp207;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp207CrateBlock extends BaseEntityBlock {
    public static final MapCodec<Scp207CrateBlock> CrateCodec = simpleCodec(Scp207CrateBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final double WallHeight = 9.0;
    public static final String Key = "block.scp_unleashed.scp207_crate.";
    private static final VoxelShape Body = box(0.0, 0.0, 0.0, 16.0, WallHeight, 16.0);

    public Scp207CrateBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CrateCodec;
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
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Body;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp207CrateBlockEntity(Pos, State);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.isEmpty() && !Scp207BottleItem.is(Stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide && User instanceof ServerPlayer Server) {
            take(Server.serverLevel(), Pos, Server);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && User instanceof ServerPlayer Server) {
            take(Server.serverLevel(), Pos, Server);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static boolean take(ServerLevel World, BlockPos Pos, ServerPlayer User) {
        Vec3 Top = Vec3.atCenterOf(Pos).add(0.0, 0.2, 0.0);
        Scp207CrateBlockEntity Crate = World.getBlockEntity(Pos) instanceof Scp207CrateBlockEntity Found ? Found : null;
        if (Crate == null || !Crate.take()) {
            User.displayClientMessage(Component.translatable(Key + "empty").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            Scp207Registry.playLayers(World, Top, Scp207Registry.EmptyLayers, SoundSource.BLOCKS, 0.6f, 0.9f + World.random.nextFloat() * 0.1f);
            return false;
        }
        boolean One = Scp207Ration.rollOne(World);
        ItemHandlerHelper.giveItemToPlayer(User, Scp207BottleItem.stack(One));
        Scp207Registry.playLayers(World, Top, Scp207Registry.TakeLayers, SoundSource.BLOCKS, 0.7f, 0.95f + World.random.nextFloat() * 0.15f);
        World.gameEvent(User, GameEvent.BLOCK_ACTIVATE, Pos);
        if (Crate.left() <= 0) {
            User.displayClientMessage(Component.translatable(Key + "last").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "take").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "daily").withStyle(ChatFormatting.GOLD));
        }
    }
}
