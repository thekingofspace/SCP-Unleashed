package net.rai.scp_unleashed.scp082;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class Scp082Rage {
    public static final float HardnessCap = 25.0f;
    public static final float SmashPower = 2.5f;
    public static final int RageProofHits = 40;
    public static final double LookAhead = 0.95;
    public static final int Forgotten = 64;

    private Scp082Rage() {
    }

    public static int required(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.is(Scp082Registry.RageProof)) {
            return RageProofHits;
        }
        float Hardness = State.getDestroySpeed(World, Pos);
        if (Hardness < 0.0f || Hardness > HardnessCap || State.is(BlockTags.WITHER_IMMUNE)) {
            return -1;
        }
        return Math.max(1, Mth.ceil(Hardness / SmashPower));
    }

    public static boolean solid(ServerLevel World, BlockPos Pos, BlockState State) {
        return !State.isAir() && State.getFluidState().isEmpty() && !State.getCollisionShape(World, Pos).isEmpty();
    }

    @Nullable
    public static BlockPos pick(ServerLevel World, Scp082Fernand Fernand, Vec3 Heading) {
        Vec3 Flat = new Vec3(Heading.x, 0.0, Heading.z);
        if (Flat.lengthSqr() < 1.0e-4) {
            Flat = Vec3.directionFromRotation(0.0f, Fernand.getYRot());
        }
        Flat = Flat.normalize();
        BlockPos Found = pickAhead(World, Fernand, Flat);
        if (Found != null || !Fernand.isBlocked()) {
            return Found;
        }
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Found = pickAhead(World, Fernand, Vec3.atLowerCornerOf(Side.getNormal()));
            if (Found != null) {
                return Found;
            }
        }
        BlockPos Above = BlockPos.containing(Fernand.getX(), Fernand.getBoundingBox().maxY + 0.2, Fernand.getZ());
        BlockState Ceiling = World.getBlockState(Above);
        return solid(World, Above, Ceiling) ? Above : null;
    }

    @Nullable
    private static BlockPos pickAhead(ServerLevel World, Scp082Fernand Fernand, Vec3 Flat) {
        AABB Body = Fernand.getBoundingBox();
        AABB Ahead = Body.move(Flat.scale(LookAhead)).deflate(0.05, 0.02, 0.05);
        BlockPos Best = null;
        double BestScore = Double.MAX_VALUE;
        Vec3 Centre = Fernand.position();
        for (BlockPos Each : BlockPos.betweenClosed(BlockPos.containing(Ahead.minX, Ahead.minY, Ahead.minZ), BlockPos.containing(Ahead.maxX, Ahead.maxY, Ahead.maxZ))) {
            BlockState State = World.getBlockState(Each);
            if (!solid(World, Each, State)) {
                continue;
            }
            int Need = required(World, Each, State);
            double Score = Vec3.atBottomCenterOf(Each).subtract(Centre).multiply(1.0, 0.0, 1.0).length() + (Each.getY() - Fernand.getBlockY()) * 0.15;
            if (Need < 0) {
                Score += 100.0;
            }
            if (Score < BestScore) {
                BestScore = Score;
                Best = Each.immutable();
            }
        }
        return Best;
    }

    public static boolean strike(ServerLevel World, Scp082Fernand Fernand, BlockPos Pos, Map<Long, Integer> Memory) {
        BlockState State = World.getBlockState(Pos);
        if (!solid(World, Pos, State)) {
            return false;
        }
        Vec3 Centre = Vec3.atCenterOf(Pos);
        SoundType Sound = State.getSoundType(World, Pos, Fernand);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Centre.x, Centre.y, Centre.z, 14, 0.35, 0.35, 0.35, 0.12);
        World.playSound(null, Pos, Sound.getHitSound(), SoundSource.BLOCKS, Sound.getVolume() * 1.2f, Sound.getPitch() * 0.7f);
        int Need = required(World, Pos, State);
        if (Need < 0 || !EventHooks.canEntityGrief(World, Fernand)) {
            World.playSound(null, Pos, Scp082Registry.Thud.get(), SoundSource.HOSTILE, 1.0f, 0.9f + World.getRandom().nextFloat() * 0.15f);
            return false;
        }
        World.playSound(null, Pos, State.is(Scp082Registry.RageProof) ? Scp082Registry.Thud.get() : Scp082Registry.Smash.get(), SoundSource.HOSTILE, 1.0f,
                0.9f + World.getRandom().nextFloat() * 0.15f);
        int Hits = Memory.merge(Pos.asLong(), 1, Integer::sum);
        if (Hits >= Need) {
            Memory.remove(Pos.asLong());
            World.destroyBlockProgress(Fernand.getId(), Pos, -1);
            World.destroyBlock(Pos, true, Fernand);
            return true;
        }
        World.destroyBlockProgress(Fernand.getId(), Pos, Mth.clamp(Hits * 10 / Need, 0, 9));
        if (Memory.size() > Forgotten) {
            Memory.keySet().removeIf(Key -> World.getBlockState(BlockPos.of(Key)).isAir());
        }
        return false;
    }
}
