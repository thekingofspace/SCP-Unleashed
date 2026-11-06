package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Map;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Scp4666StockingBlock extends BaseEntityBlock {
    public static final MapCodec<Scp4666StockingBlock> BlockCodec = simpleCodec(Scp4666StockingBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{4, 0, 12, 12, 14, 16}});

    public Scp4666StockingBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(Facing));
    }

    public static void hang(ServerLevel World, Scp4666Map Layout, List<ServerPlayer> Players) {
        Direction Out = Layout.Fireplace.getX() < Layout.Centre.getX() ? Direction.EAST : Direction.WEST;
        BlockState Fire = World.getBlockState(Layout.Fireplace);
        if (Fire.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Out = Fire.getValue(HorizontalDirectionalBlock.FACING);
        }
        for (int Index = 0; Index < Math.min(Players.size(), Layout.Stockings.size()); Index++) {
            BlockPos Spot = Layout.Stockings.get(Index);
            if (!World.getBlockState(Spot).isAir()) {
                continue;
            }
            World.setBlock(Spot, Scp4666Registry.Stocking.get().defaultBlockState().setValue(Facing, Out), Block.UPDATE_ALL);
            if (World.getBlockEntity(Spot) instanceof Scp4666OwnedBlockEntity Tag) {
                Tag.setOwner(Players.get(Index).getUUID(), Players.get(Index).getGameProfile().getName());
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        World.playSound(null, Pos, Scp4666Registry.Sack.get(), SoundSource.BLOCKS, 0.4f, 1.4f);
        String Name = World.getBlockEntity(Pos) instanceof Scp4666OwnedBlockEntity Tag ? Tag.name() : "";
        boolean Mine = World.getBlockEntity(Pos) instanceof Scp4666OwnedBlockEntity Tag && Tag.owner() != null && Tag.owner().equals(User.getUUID());
        User.displayClientMessage(Component.translatable(Mine ? "message.scp_unleashed.scp4666.stocking_mine" : "message.scp_unleashed.scp4666.stocking", Name)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp4666OwnedBlockEntity(Pos, State);
    }
}
