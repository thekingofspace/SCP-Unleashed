package net.rai.scp_unleashed.daevite.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class DaeviteBrazierBlock extends DaeviteDecorBlock {
    public static final MapCodec<DaeviteBrazierBlock> BrazierCodec = simpleCodec(DaeviteBrazierBlock::new);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;

    public DaeviteBrazierBlock(Properties Settings) {
        super(Settings, DaeviteDecor.Brazier);
        registerDefaultState(defaultBlockState().setValue(Lit, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BrazierCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Lit);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return super.getStateForPlacement(Context).setValue(Lit, true);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        boolean Burning = State.getValue(Lit);
        if (Burning && (Stack.is(ItemTags.SHOVELS) || Stack.is(Items.WATER_BUCKET))) {
            if (!World.isClientSide) {
                World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_ALL);
                World.playSound(null, Pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.9f);
                World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
                if (Stack.is(ItemTags.SHOVELS)) {
                    Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
                }
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!Burning && (Stack.is(Items.FLINT_AND_STEEL) || Stack.is(Items.FIRE_CHARGE))) {
            if (!World.isClientSide) {
                World.setBlock(Pos, State.setValue(Lit, true), Block.UPDATE_ALL);
                World.playSound(null, Pos, Stack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 0.9f);
                World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
                if (Stack.is(Items.FIRE_CHARGE)) {
                    Stack.consume(1, User);
                } else {
                    Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
                }
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        return super.useItemOn(Stack, State, World, Pos, User, Hand, Hit);
    }

    @Override
    public void stepOn(Level World, BlockPos Pos, BlockState State, Entity Body) {
        if (State.getValue(Lit) && !Body.fireImmune() && Body instanceof LivingEntity) {
            Body.igniteForSeconds(2.0f);
        }
        super.stepOn(World, Pos, State, Body);
    }

    @Override
    public void ambient(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Lit)) {
            return;
        }
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + 0.82;
        double Z = Pos.getZ() + 0.5;
        if (Random.nextInt(3) == 0) {
            World.addParticle(ParticleTypes.FLAME, X + (Random.nextDouble() - 0.5) * 0.4, Y, Z + (Random.nextDouble() - 0.5) * 0.4, 0.0, 0.02, 0.0);
        }
        if (Random.nextInt(4) == 0) {
            World.addParticle(ParticleTypes.SMOKE, X + (Random.nextDouble() - 0.5) * 0.3, Y + 0.35, Z + (Random.nextDouble() - 0.5) * 0.3, 0.0, 0.04, 0.0);
        }
        if (Random.nextInt(12) == 0) {
            World.addParticle(ParticleTypes.LAVA, X, Y, Z, 0.0, 0.0, 0.0);
        }
        if (Random.nextInt(10) == 0) {
            World.playLocalSound(X, Y, Z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f + Random.nextFloat() * 0.3f, 0.7f + Random.nextFloat() * 0.3f, false);
        }
    }
}
