package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.network.Scp4666DiaryPayload;

import java.util.List;
import java.util.Map;

public class Scp4666DiaryBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4666DiaryBlock> BlockCodec = simpleCodec(Scp4666DiaryBlock::new);
    public static final int Volumes = 4;
    public static final IntegerProperty Volume = IntegerProperty.create("volume", 0, Volumes - 1);
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{3, 0, 4, 13, 4, 12}});

    public Scp4666DiaryBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Volume, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Volume);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User.isSecondaryUseActive()) {
            World.setBlock(Pos, State.setValue(Volume, (State.getValue(Volume) + 1) % Volumes), Block.UPDATE_ALL);
            return InteractionResult.CONSUME;
        }
        World.playSound(null, Pos, Scp4666Registry.Page.get(), SoundSource.BLOCKS, 0.8f, 0.9f);
        if (User instanceof ServerPlayer Reader) {
            PacketDistributor.sendToPlayer(Reader, new Scp4666DiaryPayload(State.getValue(Volume)));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_diary.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
