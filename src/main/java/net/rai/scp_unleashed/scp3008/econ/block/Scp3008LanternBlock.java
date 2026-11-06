package net.rai.scp_unleashed.scp3008.econ.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;

public class Scp3008LanternBlock extends Block {
    public static final MapCodec<Scp3008LanternBlock> BlockCodec = simpleCodec(Scp3008LanternBlock::new);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final int MaxCharge = 8;
    public static final IntegerProperty Charge = IntegerProperty.create("charge", 0, MaxCharge);
    public static final int DrainTicks = 1250;
    public static final int LowCharge = 2;
    public static final int BrightLight = 14;
    public static final int DimLight = 9;
    private static final VoxelShape Outline = Shapes.or(Block.box(5.0, 0.0, 5.0, 11.0, 9.0, 11.0), Block.box(6.0, 9.0, 7.0, 10.0, 12.0, 9.0));

    public Scp3008LanternBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Lit, false).setValue(Charge, MaxCharge));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    public static int light(BlockState State) {
        if (!State.getValue(Lit) || State.getValue(Charge) <= 0) {
            return 0;
        }
        return State.getValue(Charge) > LowCharge ? BrightLight : DimLight;
    }

    public static boolean glowing(BlockState State) {
        return light(State) > 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Lit, Charge);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Lit, true);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean Moving) {
        if (World.isClientSide) {
            return;
        }
        if (State.getValue(Lit) && State.getValue(Charge) <= 0) {
            World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_ALL);
            return;
        }
        if (State.getValue(Lit)) {
            schedule(World, Pos);
        }
    }

    private void schedule(Level World, BlockPos Pos) {
        if (!World.getBlockTicks().hasScheduledTick(Pos, this)) {
            World.scheduleTick(Pos, this, DrainTicks);
        }
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        drain(State, World, Pos);
    }

    public BlockState drain(BlockState State, Level World, BlockPos Pos) {
        if (!State.getValue(Lit) || State.getValue(Charge) <= 0) {
            return State;
        }
        int Left = State.getValue(Charge) - 1;
        BlockState Next = State.setValue(Charge, Left).setValue(Lit, Left > 0);
        World.setBlock(Pos, Next, Block.UPDATE_ALL);
        if (Left <= 0) {
            play(World, Pos, Scp3008EconRegistry.LanternFade.get(), 0.6f, 1.0f);
        } else {
            schedule(World, Pos);
        }
        return Next;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Scp3008EconRegistry.Battery.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (State.getValue(Charge) >= MaxCharge) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp3008_econ_lantern_full").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!World.isClientSide) {
            recharge(State, World, Pos);
            Stack.consume(1, User);
            report(User, MaxCharge);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    public BlockState recharge(BlockState State, Level World, BlockPos Pos) {
        BlockState Next = State.setValue(Charge, MaxCharge).setValue(Lit, true);
        World.setBlock(Pos, Next, Block.UPDATE_ALL);
        play(World, Pos, Scp3008EconRegistry.LanternBattery.get(), 0.8f, 1.0f);
        play(World, Pos, Scp3008EconRegistry.LanternSnap.get(), 0.5f, 1.0f);
        return Next;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            toggle(State, World, Pos);
            report(User, State.getValue(Charge));
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public BlockState toggle(BlockState State, Level World, BlockPos Pos) {
        if (State.getValue(Charge) <= 0) {
            play(World, Pos, Scp3008EconRegistry.LanternClick.get(), 0.6f, 0.8f);
            return State;
        }
        boolean On = !State.getValue(Lit);
        BlockState Next = State.setValue(Lit, On);
        World.setBlock(Pos, Next, Block.UPDATE_ALL);
        play(World, Pos, Scp3008EconRegistry.LanternClick.get(), 0.6f, On ? 1.1f : 0.9f);
        play(World, Pos, Scp3008EconRegistry.LanternHum.get(), 0.45f, On ? 1.15f : 0.8f);
        return Next;
    }

    private static void report(Player User, int Left) {
        String Key = Left <= 0 ? "message.scp_unleashed.scp3008_econ_lantern_flat" : "message.scp_unleashed.scp3008_econ_lantern_level";
        User.displayClientMessage(Component.translatable(Key, Left * 100 / MaxCharge).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    private static void play(Level World, BlockPos Pos, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, Pos, Sound, SoundSource.BLOCKS, Volume, Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f));
    }
}
