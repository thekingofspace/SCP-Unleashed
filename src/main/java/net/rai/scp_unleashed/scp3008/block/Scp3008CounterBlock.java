package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Scp3008CounterBlock extends BaseEntityBlock {
    public static final MapCodec<Scp3008CounterBlock> CounterCodec = simpleCodec(Scp3008CounterBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final float SideChance = 0.45f;
    public static final Map<Direction, VoxelShape> Shapes = Scp3008Shapes.facings(new double[][]{{0, 0, 2, 16, 14, 16}, {0, 14, 0, 16, 16, 16}, {0, 16, 9, 16, 24, 11}});

    public Scp3008CounterBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Scp3008CounterBlock> codec() {
        return CounterCodec;
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
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(Facing));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp3008CounterBlockEntity(Pos, State);
    }

    public static List<ItemStack> plate(RandomSource Random) {
        List<ItemStack> Found = new ArrayList<>();
        Found.add(new ItemStack(Scp3008Registry.Meatballs.get()));
        if (Random.nextFloat() < SideChance) {
            Item[] Sides = {Scp3008Registry.Fries.get(), Scp3008Registry.SoftDrink.get(), Scp3008Registry.Jam.get()};
            Found.add(new ItemStack(Sides[Random.nextInt(Sides.length)]));
        }
        return Found;
    }

    public static boolean serve(Level World, BlockPos Pos, Player User) {
        if (!(World.getBlockEntity(Pos) instanceof Scp3008CounterBlockEntity Counter)) {
            return false;
        }
        long Now = World.getGameTime();
        if (!Counter.ready(User.getUUID(), Now)) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp3008.counter.empty").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return false;
        }
        Counter.serve(User.getUUID(), Now);
        for (ItemStack Stack : plate(World.getRandom())) {
            if (!User.getInventory().add(Stack)) {
                User.drop(Stack, false);
            }
        }
        World.playSound(null, Pos, Scp3008Registry.CounterServe.get(), SoundSource.BLOCKS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp3008.counter.served").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        serve(World, Pos, User);
        return InteractionResult.CONSUME;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }
}
