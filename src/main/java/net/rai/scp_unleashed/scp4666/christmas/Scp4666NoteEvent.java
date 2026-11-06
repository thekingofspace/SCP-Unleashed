package net.rai.scp_unleashed.scp4666.christmas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp4666NoteEvent {
    public static final long DelayMin = 24000L;
    public static final int DelaySpread = 48000;
    public static final int HomeRadius = 48;
    public static final int HomeHeight = 16;
    public static final int NearRadius = 24;
    public static final double FireRange = 32.0;
    public static final long NightStart = 13000L;
    public static final long NightEnd = 23000L;

    public record Choice(BlockPos Door, Direction Side, boolean Home) {
    }

    private Scp4666NoteEvent() {
    }

    public static void survived(ServerPlayer User) {
        Scp4666Visit Visit = Scp4666Visit.of(User);
        Visit.Survived = true;
        Visit.Read = true;
        if (Visit.NoteDone || Visit.NoteAt > 0L) {
            return;
        }
        Visit.NoteAt = User.server.overworld().getGameTime() + DelayMin + User.getRandom().nextInt(DelaySpread + 1);
    }

    public static boolean night(Level World) {
        long Time = World.getDayTime() % 24000L;
        return Time >= NightStart && Time <= NightEnd;
    }

    public static boolean due(ServerPlayer User, Scp4666Visit Visit) {
        return Visit.pending() && User.server.overworld().getGameTime() >= Visit.NoteAt && User.level().dimension() == Level.OVERWORLD && User.isAlive() && !User.isSpectator()
                && night(User.level());
    }

    public static void tick(ServerPlayer User, Scp4666Visit Visit) {
        if (!due(User, Visit)) {
            return;
        }
        Optional<Choice> Picked = choose(User.serverLevel(), User, Visit);
        if (Picked.isEmpty() || Picked.get().Door().distToCenterSqr(User.position()) > FireRange * FireRange) {
            return;
        }
        place(User, Visit, Picked.get());
    }

    public static Optional<Scp4666DoorNote> place(ServerPlayer User, Scp4666Visit Visit, Choice Picked) {
        Scp4666DoorNote Note = Scp4666DoorNote.attach(User.serverLevel(), Picked.Door(), Picked.Side(), User.getUUID());
        if (Note == null) {
            return Optional.empty();
        }
        Visit.NoteDone = true;
        Scp4666Yuletide.jingle(User.serverLevel(), Note.position());
        return Optional.of(Note);
    }

    public static BlockPos anchor(ServerPlayer User) {
        BlockPos Respawn = User.getRespawnPosition();
        if (Respawn != null && User.getRespawnDimension() == Level.OVERWORLD) {
            return Respawn;
        }
        return User.server.overworld().getSharedSpawnPos();
    }

    public static Optional<Choice> choose(ServerLevel World, ServerPlayer User, Scp4666Visit Visit) {
        return choose(World, anchor(User), User.blockPosition(), Visit);
    }

    public static Optional<Choice> choose(ServerLevel World, BlockPos Home, BlockPos Standing, Scp4666Visit Visit) {
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Long Key : Visit.Doors) {
            BlockPos Pos = BlockPos.of(Key);
            if (!isDoor(World, Pos) || !near(Pos, Home, HomeRadius, HomeHeight)) {
                continue;
            }
            double Distance = Pos.distSqr(Home);
            if (Distance < BestDistance) {
                Best = Pos;
                BestDistance = Distance;
            }
        }
        if (Best == null) {
            Best = nearest(World, Home, HomeRadius, HomeHeight);
        }
        if (Best != null) {
            return Optional.of(new Choice(Best, outside(World, Best, Home), true));
        }
        Best = nearest(World, Standing, NearRadius, HomeHeight);
        if (Best == null) {
            return Optional.empty();
        }
        return Optional.of(new Choice(Best, outside(World, Best, null), false));
    }

    public static boolean lowerDoor(BlockState State) {
        return State.getBlock() instanceof DoorBlock && State.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    public static boolean isDoor(Level World, BlockPos Pos) {
        return World.isLoaded(Pos) && lowerDoor(World.getBlockState(Pos));
    }

    public static boolean near(BlockPos Pos, BlockPos Centre, int Radius, int Height) {
        int Dx = Pos.getX() - Centre.getX();
        int Dz = Pos.getZ() - Centre.getZ();
        return Math.abs(Pos.getY() - Centre.getY()) <= Height && Dx * Dx + Dz * Dz <= Radius * Radius;
    }

    @Nullable
    public static BlockPos nearest(ServerLevel World, BlockPos Centre, int Radius, int Height) {
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        int MinY = Math.max(World.getMinBuildHeight(), Centre.getY() - Height);
        int MaxY = Math.min(World.getMaxBuildHeight() - 1, Centre.getY() + Height);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int ChunkX = (Centre.getX() - Radius) >> 4; ChunkX <= (Centre.getX() + Radius) >> 4; ChunkX++) {
            for (int ChunkZ = (Centre.getZ() - Radius) >> 4; ChunkZ <= (Centre.getZ() + Radius) >> 4; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    continue;
                }
                LevelChunkSection[] Sections = Chunk.getSections();
                for (int SectionY = MinY >> 4; SectionY <= MaxY >> 4; SectionY++) {
                    int Index = World.getSectionIndexFromSectionY(SectionY);
                    if (Index < 0 || Index >= Sections.length) {
                        continue;
                    }
                    LevelChunkSection Section = Sections[Index];
                    if (Section.hasOnlyAir() || !Section.maybeHas(Scp4666NoteEvent::lowerDoor)) {
                        continue;
                    }
                    int Low = Math.max(MinY, SectionY << 4);
                    int High = Math.min(MaxY, (SectionY << 4) + 15);
                    for (int Y = Low; Y <= High; Y++) {
                        for (int X = 0; X < 16; X++) {
                            for (int Z = 0; Z < 16; Z++) {
                                if (!lowerDoor(Section.getBlockState(X, Y & 15, Z))) {
                                    continue;
                                }
                                Cursor.set((ChunkX << 4) + X, Y, (ChunkZ << 4) + Z);
                                if (!near(Cursor, Centre, Radius, Height)) {
                                    continue;
                                }
                                double Distance = Cursor.distSqr(Centre);
                                if (Distance < BestDistance) {
                                    BestDistance = Distance;
                                    Best = Cursor.immutable();
                                }
                            }
                        }
                    }
                }
            }
        }
        return Best;
    }

    public static Direction outside(Level World, BlockPos Door, @Nullable BlockPos Home) {
        Direction Facing = World.getBlockState(Door).getValue(DoorBlock.FACING);
        Direction Front = Facing.getOpposite();
        boolean FrontSky = World.canSeeSky(Door.above().relative(Front));
        boolean BackSky = World.canSeeSky(Door.above().relative(Facing));
        if (FrontSky != BackSky) {
            return FrontSky ? Front : Facing;
        }
        if (Home != null) {
            double FrontDistance = Door.relative(Front).distSqr(Home);
            double BackDistance = Door.relative(Facing).distSqr(Home);
            if (FrontDistance != BackDistance) {
                return FrontDistance > BackDistance ? Front : Facing;
            }
        }
        return Front;
    }
}
