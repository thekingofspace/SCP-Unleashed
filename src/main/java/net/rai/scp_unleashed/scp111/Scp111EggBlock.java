package net.rai.scp_unleashed.scp111;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

public class Scp111EggBlock extends Block {
    public static final MapCodec<Scp111EggBlock> EggCodec = simpleCodec(Scp111EggBlock::new);
    public static final EnumProperty<Scp111Variant> Variant = EnumProperty.create("variant", Scp111Variant.class);
    public static final IntegerProperty Eggs = BlockStateProperties.EGGS;
    public static final IntegerProperty Hatch = BlockStateProperties.HATCH;
    public static final int MaxEggs = 4;
    public static final int WalkCrushChance = 5;
    public static final int StandCrushChance = 100;
    public static final double MovingSpeedSqr = 1.0E-4;
    public static final double CrushHeight = 3.0 / 16.0;
    private static final VoxelShape OneEgg = box(5.5, 0.0, 5.5, 10.5, 3.0, 10.5);
    private static final VoxelShape SomeEggs = box(4.5, 0.0, 4.5, 11.5, 3.0, 11.5);

    public Scp111EggBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Variant, Scp111Variant.Ember).setValue(Eggs, 1).setValue(Hatch, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return EggCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Variant, Eggs, Hatch);
    }

    public static BlockState clutch(Scp111Variant Kind, int Count, int Stage) {
        return Scp111Registry.EggBlock.get().defaultBlockState().setValue(Variant, Kind).setValue(Eggs, Math.max(1, Math.min(MaxEggs, Count)))
                .setValue(Hatch, Math.max(0, Math.min(Scp111Egg.MaxStage, Stage)));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Eggs) > 1 ? SomeEggs : OneEgg;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return Type != PathComputationType.WATER;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == Direction.DOWN && !canSurvive(State, World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Existing = Context.getLevel().getBlockState(Context.getClickedPos());
        if (Existing.is(this)) {
            return Existing.setValue(Eggs, Math.min(MaxEggs, Existing.getValue(Eggs) + 1));
        }
        Scp111Egg Held = Scp111Egg.of(Context.getItemInHand());
        BlockState Fresh = defaultBlockState().setValue(Variant, Held.Variant()).setValue(Hatch, Held.Stage());
        return Fresh.canSurvive(Context.getLevel(), Context.getClickedPos()) ? Fresh : null;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        ItemStack Held = Context.getItemInHand();
        if (!Context.isSecondaryUseActive() && Held.is(Scp111Registry.EggItem.get()) && State.getValue(Eggs) < MaxEggs
                && Scp111Egg.of(Held).Variant() == State.getValue(Variant)) {
            return true;
        }
        return super.canBeReplaced(State, Context);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean Moving) {
        super.onPlace(State, World, Pos, OldState, Moving);
        if (!World.isClientSide && !OldState.is(this)) {
            schedule(World, Pos);
        }
    }

    public void schedule(Level World, BlockPos Pos) {
        World.scheduleTick(Pos, this, Scp111Egg.StageTicks + World.random.nextInt(Scp111Egg.StageJitter + 1));
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (Scp111Hatching.isChilled(World, Pos)) {
            schedule(World, Pos);
            return;
        }
        advance(State, World, Pos);
    }

    public void advance(BlockState State, ServerLevel World, BlockPos Pos) {
        int Stage = State.getValue(Hatch);
        if (Stage < Scp111Egg.MaxStage) {
            World.setBlock(Pos, State.setValue(Hatch, Stage + 1), Block.UPDATE_CLIENTS);
            World.playSound(null, Pos, Scp111Registry.EggCrack.get(), SoundSource.BLOCKS, 0.6f, 0.95f + World.random.nextFloat() * 0.15f);
            World.gameEvent(GameEvent.BLOCK_CHANGE, Pos, GameEvent.Context.of(State));
            schedule(World, Pos);
            return;
        }
        hatch(State, World, Pos);
    }

    public void hatch(BlockState State, ServerLevel World, BlockPos Pos) {
        World.removeBlock(Pos, false);
        World.gameEvent(GameEvent.BLOCK_DESTROY, Pos, GameEvent.Context.of(State));
        Scp111Hatching.hatchClutch(World, Pos, State.getValue(Variant), State.getValue(Eggs));
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Visitor) {
        if (World.isClientSide || !(Visitor instanceof LivingEntity Walker) || !canCrush(World, Walker)) {
            return;
        }
        AABB Clutch = getShape(State, World, Pos, CollisionContext.empty()).bounds().move(Pos);
        if (!Walker.getBoundingBox().intersects(Clutch.setMaxY(Pos.getY() + CrushHeight))) {
            return;
        }
        boolean Moving = Walker.getDeltaMovement().horizontalDistanceSqr() > MovingSpeedSqr || Walker.fallDistance > 0.5f;
        if (World.random.nextInt(Moving ? WalkCrushChance : StandCrushChance) == 0) {
            crush(World, Pos, State);
        }
    }

    private boolean canCrush(Level World, LivingEntity Walker) {
        if (Walker instanceof Scp111Snail || Walker instanceof Bat || Walker.isSteppingCarefully() || Walker.isSpectator()) {
            return false;
        }
        return Walker instanceof Player || EventHooks.canEntityGrief(World, Walker);
    }

    public static void crush(Level World, BlockPos Pos, BlockState State) {
        World.playSound(null, Pos, Scp111Registry.EggBreak.get(), SoundSource.BLOCKS, 0.7f, 0.9f + World.random.nextFloat() * 0.2f);
        int Count = State.getValue(Eggs);
        if (Count <= 1) {
            World.destroyBlock(Pos, false);
        } else {
            World.setBlock(Pos, State.setValue(Eggs, Count - 1), Block.UPDATE_CLIENTS);
            World.gameEvent(GameEvent.BLOCK_DESTROY, Pos, GameEvent.Context.of(State));
            World.levelEvent(2001, Pos, Block.getId(State));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        return Stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            ItemStack Taken = new Scp111Egg(State.getValue(Variant), State.getValue(Hatch)).stack(1);
            int Count = State.getValue(Eggs);
            if (Count <= 1) {
                World.removeBlock(Pos, false);
            } else {
                World.setBlock(Pos, State.setValue(Eggs, Count - 1), Block.UPDATE_CLIENTS);
            }
            World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
            ItemHandlerHelper.giveItemToPlayer(User, Taken);
            World.playSound(null, Pos, Scp111Registry.EggPick.get(), SoundSource.BLOCKS, 0.6f, 1.1f + World.random.nextFloat() * 0.15f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState State, LootParams.Builder Params) {
        List<ItemStack> Dropped = super.getDrops(State, Params);
        List<ItemStack> Marked = new ArrayList<>(Dropped.size());
        Scp111Egg Kind = new Scp111Egg(State.getValue(Variant), State.getValue(Hatch));
        for (ItemStack Drop : Dropped) {
            if (Drop.is(Scp111Registry.EggItem.get())) {
                Drop.set(Scp111Registry.EggData.get(), Kind);
            }
            Marked.add(Drop);
        }
        return Marked;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader World, BlockPos Pos, BlockState State) {
        return new Scp111Egg(State.getValue(Variant), State.getValue(Hatch)).stack(1);
    }
}
