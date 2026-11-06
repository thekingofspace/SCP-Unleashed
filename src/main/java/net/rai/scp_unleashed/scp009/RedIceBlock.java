package net.rai.scp_unleashed.scp009;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RedIceBlock extends HalfTransparentBlock {
    public static final MapCodec<RedIceBlock> BlockCodec = simpleCodec(RedIceBlock::new);
    public static final int Glow = 3;
    public static final int GlintChance = 90;

    public RedIceBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected MapCodec<RedIceBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState Previous, boolean Pushed) {
        super.onPlace(State, World, Pos, Previous, Pushed);
        if (World instanceof ServerLevel Server && !Previous.is(this)) {
            Scp009Sources.placed(Server, Pos);
            if (RedIceSpread.enabled(Server)) {
                RedIceSpread.schedule(Server, Pos, Server.random);
            }
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return true;
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        RedIceSpread.step(World, Pos, Random);
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        RedIceSpread.step(World, Pos, Random);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replacement, boolean Pushed) {
        if (World instanceof ServerLevel Server && !Replacement.is(this)) {
            Scp009Sources.removed(Server, Pos);
        }
        super.onRemove(State, World, Pos, Replacement, Pushed);
    }

    @Override
    protected void onProjectileHit(Level World, BlockState State, BlockHitResult Hit, Projectile Shot) {
        if (!World.isClientSide) {
            BlockPos Pos = Hit.getBlockPos();
            World.playSound(null, Pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0f, 0.5f + World.random.nextFloat() * 1.2f);
            World.playSound(null, Pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.5f + World.random.nextFloat() * 1.2f);
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(GlintChance) != 0) {
            return;
        }
        Direction Side = Direction.getRandom(Random);
        BlockPos Outside = Pos.relative(Side);
        if (World.getBlockState(Outside).isSolidRender(World, Outside) || World.getBlockState(Outside).is(this)) {
            return;
        }
        double X = Pos.getX() + 0.5 + Side.getStepX() * 0.55 + (Side.getStepX() == 0 ? Random.nextDouble() - 0.5 : 0.0);
        double Y = Pos.getY() + 0.5 + Side.getStepY() * 0.55 + (Side.getStepY() == 0 ? Random.nextDouble() - 0.5 : 0.0);
        double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.55 + (Side.getStepZ() == 0 ? Random.nextDouble() - 0.5 : 0.0);
        World.addParticle(Scp009Registry.Glint.get(), X, Y, Z, 0.0, 0.006, 0.0);
    }
}
