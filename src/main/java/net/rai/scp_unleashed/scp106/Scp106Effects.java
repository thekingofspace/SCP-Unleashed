package net.rai.scp_unleashed.scp106;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.block.DecoratableBlock;
import net.rai.scp_unleashed.decal.DecalItem;
import net.rai.scp_unleashed.decal.DecalKind;

public final class Scp106Effects {
    public static final float DecalChance = 0.05f;

    private Scp106Effects() {
    }

    public static void pool(ServerLevel World, Vec3 At) {
        World.sendParticles(Scp106Registry.Pool.get(), At.x, floorY(World, At) + 0.02, At.z, 1, 0.0, 0.0, 0.0, 0.0);
        World.sendParticles(Scp106Registry.Goo.get(), At.x, At.y + 0.4, At.z, 14, 0.5, 0.3, 0.5, 0.02);
    }

    public static void splash(ServerLevel World, Vec3 At, int Count, double Spread) {
        World.sendParticles(Scp106Registry.Goo.get(), At.x, At.y, At.z, Count, Spread, Spread * 0.6, Spread, 0.03);
    }

    public static double floorY(ServerLevel World, Vec3 At) {
        BlockPos Feet = BlockPos.containing(At.x, At.y + 0.5, At.z);
        for (int Step = 0; Step < 4; Step++) {
            BlockPos Below = Feet.below(Step + 1);
            BlockState Floor = World.getBlockState(Below);
            if (!Floor.getCollisionShape(World, Below).isEmpty()) {
                return Below.getY() + Floor.getCollisionShape(World, Below).max(Direction.Axis.Y);
            }
        }
        return At.y;
    }

    public static void layered(ServerLevel World, Entity Source, DeferredHolder<SoundEvent, SoundEvent> Main, DeferredHolder<SoundEvent, SoundEvent> Layer,
                               float Volume, float Pitch) {
        RandomSource Random = World.getRandom();
        float Varied = Pitch * (0.94f + Random.nextFloat() * 0.12f);
        World.playSound(null, Source.getX(), Source.getEyeY(), Source.getZ(), Main.get(), SoundSource.HOSTILE, Volume, Varied);
        World.playSound(null, Source.getX(), Source.getEyeY(), Source.getZ(), Layer.get(), SoundSource.HOSTILE, Volume * 0.8f, Varied * (0.97f + Random.nextFloat() * 0.06f));
    }

    public static void voice(ServerLevel World, Entity Source, Holder<SoundEvent> Sound, float Volume, float Pitch, long Seed) {
        World.playSeededSound(null, Source, Sound, SoundSource.HOSTILE, Volume, Pitch, Seed);
    }

    public static void decal(ServerLevel World, BlockPos Floor, RandomSource Random) {
        BlockState State = World.getBlockState(Floor);
        if (!(State.getBlock() instanceof DecoratableBlock) || Random.nextFloat() >= DecalChance) {
            return;
        }
        if (DecalItem.layerCount(World, Floor, Direction.UP) >= DecalItem.MaxLayersPerFace) {
            return;
        }
        int Variant = 1 + Random.nextInt(DecalKind.Corrosion.getVariants());
        DecoratableBlock.addDecal(World, Floor, Direction.UP, DecalKind.Corrosion.layer(Variant, Direction.UP, Direction.NORTH, Random));
    }
}
