package net.rai.scp_unleashed.scp173;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.decal.DecalRegistry;
import net.rai.scp_unleashed.decal.DecalScraperItem;
import org.jetbrains.annotations.Nullable;

public class Scp173ResidueBlock extends Block {
    public static final MapCodec<Scp173ResidueBlock> BlockCodec = simpleCodec(Scp173ResidueBlock::new);
    public static final float Hardness = 1.0f;
    public static final int ScrapeWear = 1;
    private static final VoxelShape Outline = Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0);

    public Scp173ResidueBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState State = defaultBlockState();
        return State.canSurvive(Context.getLevel(), Context.getClickedPos()) ? State : null;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbor, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == Direction.DOWN && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbor, World, Pos, NeighborPos);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState Old, boolean Moved) {
        super.onPlace(State, World, Pos, Old, Moved);
        if (World instanceof ServerLevel Server && !Old.is(this)) {
            Scp173ResidueIndex.get(Server).add(Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        if (World instanceof ServerLevel Server && !Replaced.is(this)) {
            Scp173ResidueIndex.get(Server).remove(Pos);
        }
        super.onRemove(State, World, Pos, Replaced, Moved);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(Stack.getItem() instanceof DecalScraperItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server) {
            scrape(Server, Pos, State, User);
            Stack.hurtAndBreak(ScrapeWear, User, LivingEntity.getSlotForHand(Hand));
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    public static void scrape(ServerLevel World, BlockPos Pos, BlockState State, @Nullable Player User) {
        World.removeBlock(Pos, false);
        World.playSound(null, Pos, DecalRegistry.DecalScrape.get(), SoundSource.BLOCKS, 0.9f, 0.8f + World.random.nextFloat() * 0.2f);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Pos.getX() + 0.5, Pos.getY() + 0.1, Pos.getZ() + 0.5, 12, 0.3, 0.05, 0.3, 0.05);
        World.gameEvent(GameEvent.BLOCK_DESTROY, Pos, GameEvent.Context.of(User, State));
    }
}
