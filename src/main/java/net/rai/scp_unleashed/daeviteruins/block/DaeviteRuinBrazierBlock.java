package net.rai.scp_unleashed.daeviteruins.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;

public class DaeviteRuinBrazierBlock extends Block {
    public static final MapCodec<DaeviteRuinBrazierBlock> BlockCodec = simpleCodec(DaeviteRuinBrazierBlock::new);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    private static final VoxelShape Shape = Block.box(1, 0, 1, 15, 13, 15);

    public DaeviteRuinBrazierBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Lit);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        boolean Burning = State.getValue(Lit);
        if (!Burning && (Stack.is(Items.FLINT_AND_STEEL) || Stack.is(Items.FIRE_CHARGE))) {
            if (!World.isClientSide) {
                World.setBlock(Pos, State.setValue(Lit, true), Block.UPDATE_ALL);
                World.playSound(null, Pos, Stack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f,
                        0.9f + World.getRandom().nextFloat() * 0.2f);
                World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
                if (Stack.is(Items.FIRE_CHARGE)) {
                    Stack.consume(1, User);
                } else {
                    Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
                }
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (Burning && Stack.canPerformAction(ItemAbilities.SHOVEL_DOUSE)) {
            if (!World.isClientSide) {
                World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_ALL);
                World.playSound(null, Pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7f, 1.0f);
                World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
                Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
            } else {
                for (int Puff = 0; Puff < 10; Puff++) {
                    World.addParticle(ParticleTypes.LARGE_SMOKE, Pos.getX() + 0.3 + World.getRandom().nextDouble() * 0.4, Pos.getY() + 0.9,
                            Pos.getZ() + 0.3 + World.getRandom().nextDouble() * 0.4, 0.0, 0.05, 0.0);
                }
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Lit)) {
            if (Random.nextInt(14) == 0) {
                World.addParticle(ParticleTypes.SMOKE, Pos.getX() + 0.35 + Random.nextDouble() * 0.3, Pos.getY() + 0.85, Pos.getZ() + 0.35 + Random.nextDouble() * 0.3,
                        0.0, 0.01, 0.0);
            }
            return;
        }
        if (Random.nextInt(8) == 0) {
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.7, Pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f + Random.nextFloat() * 0.4f,
                    0.7f + Random.nextFloat() * 0.4f, false);
        }
        for (int Spark = 0; Spark < 2; Spark++) {
            World.addParticle(ParticleTypes.FLAME, Pos.getX() + 0.25 + Random.nextDouble() * 0.5, Pos.getY() + 0.85 + Random.nextDouble() * 0.3,
                    Pos.getZ() + 0.25 + Random.nextDouble() * 0.5, 0.0, 0.02, 0.0);
        }
        if (Random.nextInt(3) == 0) {
            World.addParticle(ParticleTypes.SMOKE, Pos.getX() + 0.5, Pos.getY() + 1.4, Pos.getZ() + 0.5, 0.0, 0.04, 0.0);
        }
        if (Random.nextInt(10) == 0) {
            World.addParticle(ParticleTypes.LAVA, Pos.getX() + 0.5, Pos.getY() + 0.9, Pos.getZ() + 0.5, 0.0, 0.0, 0.0);
        }
    }
}
