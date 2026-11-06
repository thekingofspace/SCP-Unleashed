package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Chores;
import net.rai.scp_unleashed.scp4666.Scp4666Clock;
import net.rai.scp_unleashed.scp4666.Scp4666Cues;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666State;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.List;
import java.util.Map;

public class Scp4666StoveBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4666StoveBlock> BlockCodec = simpleCodec(Scp4666StoveBlock::new);
    public static final IntegerProperty Kettle = IntegerProperty.create("kettle", 0, 2);
    public static final IntegerProperty Oven = IntegerProperty.create("oven", 0, 2);
    public static final int HeatTicks = 240;
    public static final int BakeTicks = 200;
    public static final int Loaves = 2;
    public static final int WarmthTicks = 200;
    public static final float AnswerChance = 0.35f;
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{1, 0, 2, 15, 13, 16}, {4, 13, 5, 11, 20, 12}});

    public Scp4666StoveBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Kettle, 0).setValue(Oven, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Kettle, Oven);
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
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Scp4666Registry.Oats.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (State.getValue(Oven) != 0 || State.getValue(Kettle) == 1) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.stove_busy").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        World.setBlock(Pos, State.setValue(Oven, 1), Block.UPDATE_ALL);
        World.scheduleTick(Pos, this, BakeTicks);
        World.playSound(null, Pos, Scp4666Registry.OvenDoor.get(), SoundSource.BLOCKS, 0.8f, 0.9f);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.bake_start").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        if (World instanceof ServerLevel Server && User instanceof ServerPlayer Visitor && Scp4666Chores.active(Server, Visitor)) {
            Scp4666Chores.perform(Server, Visitor, Scp4666Chores.Chore.Bake);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (State.getValue(Oven) == 2) {
            World.setBlock(Pos, State.setValue(Oven, 0), Block.UPDATE_ALL);
            World.playSound(null, Pos, Scp4666Registry.OvenDoor.get(), SoundSource.BLOCKS, 0.7f, 1.1f);
            ItemStack Bread = new ItemStack(Scp4666Registry.OatBread.get(), Loaves);
            if (!User.getInventory().add(Bread)) {
                User.drop(Bread, false);
            }
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.bake_done").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        if (State.getValue(Oven) == 1) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.bake_wait").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        int Stage = State.getValue(Kettle);
        if (Stage == 0) {
            World.setBlock(Pos, State.setValue(Kettle, 1), Block.UPDATE_ALL);
            World.scheduleTick(Pos, this, HeatTicks);
            if (World instanceof ServerLevel Server && User instanceof ServerPlayer Visitor && Scp4666Chores.active(Server, Visitor)) {
                Scp4666Chores.perform(Server, Visitor, Scp4666Chores.Chore.Kettle);
            }
            World.playSound(null, Pos, Scp4666Registry.KettleBoil.get(), SoundSource.BLOCKS, 0.6f, 0.9f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.kettle_on").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        } else if (Stage == 2) {
            World.setBlock(Pos, State.setValue(Kettle, 0), Block.UPDATE_ALL);
            World.playSound(null, Pos, Scp4666Registry.Drink.get(), SoundSource.BLOCKS, 0.5f, 1.3f);
            User.addEffect(new MobEffectInstance(MobEffects.REGENERATION, WarmthTicks, 0, false, true));
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.kettle_pour").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        } else {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.kettle_wait").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Oven) == 1) {
            World.setBlock(Pos, State.setValue(Oven, 2), Block.UPDATE_ALL);
            World.playSound(null, Pos, Scp4666Registry.OvenDoor.get(), SoundSource.BLOCKS, 0.5f, 1.4f);
            return;
        }
        if (State.getValue(Kettle) != 1) {
            return;
        }
        World.setBlock(Pos, State.setValue(Kettle, 2), Block.UPDATE_ALL);
        World.playSound(null, Pos, Scp4666Registry.KettleWhistle.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        if (Scp4666World.inside(World) && Scp4666Clock.night(Scp4666State.get(World.getServer()).clock()) && Random.nextFloat() < AnswerChance) {
            Vec3 Far = Vec3.atCenterOf(Pos).add((Random.nextDouble() - 0.5) * 60.0, 2.0, (Random.nextDouble() - 0.5) * 60.0);
            Scp4666Cues.after(World, 50, Level -> Level.playSound(null, Far.x, Far.y, Far.z, Scp4666Registry.WhistleKettleReply.get(), SoundSource.AMBIENT, 2.6f, 1.0f));
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Stage = State.getValue(Kettle);
        if (Random.nextInt(6) == 0) {
            Direction Front = State.getValue(FACING);
            World.addParticle(ParticleTypes.SMALL_FLAME, Pos.getX() + 0.5 + Front.getStepX() * 0.45, Pos.getY() + 0.25, Pos.getZ() + 0.5 + Front.getStepZ() * 0.45, 0.0, 0.0, 0.0);
        }
        if (State.getValue(Oven) > 0 && Random.nextInt(3) == 0) {
            Direction Front = State.getValue(FACING);
            World.addParticle(ParticleTypes.WHITE_SMOKE, Pos.getX() + 0.5 + Front.getStepX() * 0.5, Pos.getY() + 0.45, Pos.getZ() + 0.5 + Front.getStepZ() * 0.5, Front.getStepX() * 0.01, 0.02, Front.getStepZ() * 0.01);
        }
        if (Stage > 0 && Random.nextInt(Stage == 2 ? 1 : 4) == 0) {
            World.addParticle(ParticleTypes.CLOUD, Pos.getX() + 0.5, Pos.getY() + 1.3, Pos.getZ() + 0.5, 0.0, 0.03, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_stove.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
