package net.rai.scp_unleashed.scp073;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Scp073Blight {
    public static final int LeafRadius = 3;
    public static final double LeafReach = 3.5;
    public static final int ScanTicks = 8;
    public static final int MaxWithering = 4;
    public static final int WitherMin = 24;
    public static final int WitherSpread = 16;
    public static final int DriftTicks = 6;
    public static final int FallFlakes = 6;
    public static final int GrassFlakes = 3;
    public static final int FadeSoundGap = 6;
    public static final float WitherVolume = 0.6f;
    public static final float FadeVolume = 0.45f;
    public static final int Removal = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;

    private final List<Withering> Active = new ArrayList<>();
    private long QuietUntil;

    private static final class Withering {
        private final BlockPos Pos;
        private final int Duration;
        private final int Slot;
        private int Age;
        private int Stage = -1;

        private Withering(BlockPos Pos, int Duration, int Slot) {
            this.Pos = Pos;
            this.Duration = Duration;
            this.Slot = Slot;
        }
    }

    public int activeCount() {
        return Active.size();
    }

    public void tick(ServerLevel World, Scp073Cain Cain) {
        touch(World, Cain);
        if (Cain.tickCount % ScanTicks == 0 && Active.size() < MaxWithering) {
            pick(World, Cain);
        }
        progress(World, Cain);
    }

    public void clear(ServerLevel World, Scp073Cain Cain) {
        for (Withering Entry : Active) {
            World.destroyBlockProgress(breaker(Cain, Entry.Slot), Entry.Pos, -1);
        }
        Active.clear();
    }

    private static int breaker(Scp073Cain Cain, int Slot) {
        return -1 - (Cain.getId() * MaxWithering + Slot);
    }

    private int freeSlot() {
        for (int Slot = 0; Slot < MaxWithering; Slot++) {
            boolean Used = false;
            for (Withering Entry : Active) {
                if (Entry.Slot == Slot) {
                    Used = true;
                    break;
                }
            }
            if (!Used) {
                return Slot;
            }
        }
        return -1;
    }

    private boolean withering(BlockPos Pos) {
        for (Withering Entry : Active) {
            if (Entry.Pos.equals(Pos)) {
                return true;
            }
        }
        return false;
    }

    private void pick(ServerLevel World, Scp073Cain Cain) {
        Vec3 Centre = Cain.position().add(0.0, Cain.getBbHeight() * 0.5, 0.0);
        BlockPos Middle = BlockPos.containing(Centre);
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Each : BlockPos.betweenClosed(Middle.offset(-LeafRadius, -LeafRadius, -LeafRadius), Middle.offset(LeafRadius, LeafRadius + 1, LeafRadius))) {
            if (Vec3.atCenterOf(Each).distanceToSqr(Centre) > LeafReach * LeafReach || !World.isLoaded(Each)) {
                continue;
            }
            if (World.getBlockState(Each).is(BlockTags.LEAVES) && !withering(Each)) {
                Found.add(Each.immutable());
            }
        }
        int Slot = freeSlot();
        if (Found.isEmpty() || Slot < 0) {
            return;
        }
        RandomSource Random = Cain.getRandom();
        BlockPos Chosen = Found.get(Random.nextInt(Found.size()));
        Active.add(new Withering(Chosen, WitherMin + Random.nextInt(WitherSpread + 1), Slot));
    }

    private void progress(ServerLevel World, Scp073Cain Cain) {
        Iterator<Withering> Each = Active.iterator();
        while (Each.hasNext()) {
            Withering Entry = Each.next();
            int Breaker = breaker(Cain, Entry.Slot);
            if (!World.isLoaded(Entry.Pos) || !World.getBlockState(Entry.Pos).is(BlockTags.LEAVES)) {
                World.destroyBlockProgress(Breaker, Entry.Pos, -1);
                Each.remove();
                continue;
            }
            Entry.Age++;
            int Stage = Mth.clamp(Entry.Age * 10 / Entry.Duration, 0, 9);
            if (Stage != Entry.Stage) {
                Entry.Stage = Stage;
                World.destroyBlockProgress(Breaker, Entry.Pos, Stage);
            }
            if (Entry.Age % DriftTicks == 0) {
                flakes(World, Entry.Pos, 1, Cain.getRandom());
            }
            if (Entry.Age >= Entry.Duration) {
                World.destroyBlockProgress(Breaker, Entry.Pos, -1);
                vanish(World, Cain, Entry.Pos);
                Each.remove();
            }
        }
    }

    private void vanish(ServerLevel World, Scp073Cain Cain, BlockPos Pos) {
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Removal);
        World.gameEvent(Cain, GameEvent.BLOCK_DESTROY, Pos);
        flakes(World, Pos, FallFlakes, Cain.getRandom());
        Vec3 Point = Vec3.atCenterOf(Pos);
        float Pitch = 0.9f + Cain.getRandom().nextFloat() * 0.25f;
        play(World, Point, WitherVolume, Pitch);
    }

    private void touch(ServerLevel World, Scp073Cain Cain) {
        AABB Box = Cain.getBoundingBox();
        int MinX = Mth.floor(Box.minX + 0.001);
        int MaxX = Mth.floor(Box.maxX - 0.001);
        int MinZ = Mth.floor(Box.minZ + 0.001);
        int MaxZ = Mth.floor(Box.maxZ - 0.001);
        if (Cain.onGround()) {
            int Below = Mth.floor(Box.minY - 0.05);
            for (int X = MinX; X <= MaxX; X++) {
                for (int Z = MinZ; Z <= MaxZ; Z++) {
                    BlockPos Pos = new BlockPos(X, Below, Z);
                    if (World.getBlockState(Pos).is(Blocks.GRASS_BLOCK)) {
                        World.setBlock(Pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                        World.gameEvent(Cain, GameEvent.BLOCK_CHANGE, Pos);
                        faded(World, Cain, Pos.above());
                    }
                }
            }
        }
        int MinY = Mth.floor(Box.minY + 0.001);
        int MaxY = Mth.floor(Box.maxY - 0.001);
        for (int X = MinX; X <= MaxX; X++) {
            for (int Y = MinY; Y <= MaxY; Y++) {
                for (int Z = MinZ; Z <= MaxZ; Z++) {
                    BlockPos Pos = new BlockPos(X, Y, Z);
                    BlockState State = World.getBlockState(Pos);
                    if (State.is(Blocks.SHORT_GRASS) || State.is(Blocks.FERN)) {
                        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Removal);
                        World.gameEvent(Cain, GameEvent.BLOCK_DESTROY, Pos);
                        faded(World, Cain, Pos);
                    } else if (State.is(Blocks.TALL_GRASS) || State.is(Blocks.LARGE_FERN)) {
                        BlockPos Other = State.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? Pos.above() : Pos.below();
                        if (World.getBlockState(Other).is(State.getBlock())) {
                            World.setBlock(Other, Blocks.AIR.defaultBlockState(), Removal);
                        }
                        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Removal);
                        World.gameEvent(Cain, GameEvent.BLOCK_DESTROY, Pos);
                        faded(World, Cain, Pos);
                    }
                }
            }
        }
    }

    private void faded(ServerLevel World, Scp073Cain Cain, BlockPos Pos) {
        flakes(World, Pos, GrassFlakes, Cain.getRandom());
        if (World.getGameTime() >= QuietUntil) {
            QuietUntil = World.getGameTime() + FadeSoundGap;
            World.playSound(null, Pos.getX() + 0.5, Pos.getY() + 0.2, Pos.getZ() + 0.5, Scp073Registry.Fade.get(), SoundSource.BLOCKS, FadeVolume,
                    0.9f + Cain.getRandom().nextFloat() * 0.2f);
        }
    }

    private static void flakes(ServerLevel World, BlockPos Pos, int Count, RandomSource Random) {
        for (int Index = 0; Index < Count; Index++) {
            World.sendParticles(Scp073Registry.Leaf.get(), Pos.getX() + 0.1 + Random.nextDouble() * 0.8, Pos.getY() + 0.1 + Random.nextDouble() * 0.8,
                    Pos.getZ() + 0.1 + Random.nextDouble() * 0.8, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void play(ServerLevel World, Vec3 Point, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp073Registry.WitherLayers) {
            World.playSound(null, Point.x, Point.y, Point.z, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }
}
