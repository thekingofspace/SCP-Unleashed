package net.rai.scp_unleashed.scp457;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp457AshPileBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<Scp457AshPileBlock> BlockCodec = simpleCodec(Scp457AshPileBlock::new);
    public static final BooleanProperty Waterlogged = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty Frosted = BooleanProperty.create("frosted");
    public static final float Hardness = 0.6f;
    public static final float Resistance = 3600000.0f;
    public static final int Glow = 3;
    public static final float SmokeChance = 0.14f;
    public static final float EmberChance = 0.05f;
    private static final VoxelShape Outline = Block.box(1.0, 0.0, 1.0, 15.0, 5.0, 15.0);

    public Scp457AshPileBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Waterlogged, false).setValue(Frosted, false));
    }

    public static int glow(BlockState State) {
        return State.getValue(Waterlogged) || State.getValue(Frosted) ? 0 : Glow;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState State, LootParams.Builder Params) {
        ItemStack Heap = new ItemStack(this);
        if (Params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Scp457AshPileBlockEntity Pile && Pile.holdsRemains()) {
            Pile.saveToItem(Heap, Params.getLevel().registryAccess());
        }
        return List.of(Heap);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Waterlogged, Frosted);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Waterlogged, Context.getLevel().getFluidState(Context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return State.getValue(Waterlogged) ? Fluids.WATER.getSource(false) : super.getFluidState(State);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbor, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (State.getValue(Waterlogged)) {
            World.scheduleTick(Pos, Fluids.WATER, Fluids.WATER.getTickDelay(World));
        }
        return super.updateShape(State, Side, Neighbor, World, Pos, NeighborPos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp457AshPileBlockEntity(Pos, State);
    }

    public static boolean isLighter(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!isLighter(Stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server) {
            float Pitch = World.getRandom().nextFloat() * 0.4f + 0.8f;
            World.playSound(null, Pos, Stack.isDamageableItem() ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0f, Pitch);
            if (State.getValue(Waterlogged)) {
                World.playSound(null, Pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.4f);
                return ItemInteractionResult.CONSUME;
            }
            CompoundTag Remains = World.getBlockEntity(Pos) instanceof Scp457AshPileBlockEntity Pile ? Pile.remains() : null;
            if (Scp457Prime.relight(Server, Pos, State, Remains) == null) {
                Scp457Effects.stifled(Server, Pos);
                return ItemInteractionResult.CONSUME;
            }
            User.awardStat(Stats.ITEM_USED.get(Stack.getItem()));
            spend(Stack, User, Hand);
            World.gameEvent(User, GameEvent.BLOCK_DESTROY, Pos);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    private static void spend(ItemStack Stack, Player User, InteractionHand Hand) {
        if (Stack.isDamageableItem()) {
            Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
        } else if (!User.hasInfiniteMaterials()) {
            Stack.shrink(1);
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Frosted)) {
            Scp457Cold.frostTick(World, Pos, Random);
            return;
        }
        if (State.getValue(Waterlogged)) {
            return;
        }
        double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.5;
        double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.5;
        if (Random.nextFloat() < SmokeChance) {
            World.addParticle(ParticleTypes.SMOKE, X, Pos.getY() + 0.3, Z, 0.0, 0.01, 0.0);
        }
        if (Random.nextFloat() < EmberChance) {
            World.addParticle(Scp457Registry.Ember.get(), X, Pos.getY() + 0.25, Z, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return State.getValue(Frosted);
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        Scp457Cold.thaw(World, Pos, State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp457_ash_pile.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
