package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PylonGrid {
    private static final int[] Signs = {1, -1};

    private PylonGrid() {
    }

    public enum Problem {
        None("none"),
        NoPartnerX("no_partner_x"),
        NoPartnerZ("no_partner_z"),
        TooClose("too_close"),
        MissingCorner("missing_corner"),
        CornerMissing("corner_missing"),
        CornerUnpowered("corner_unpowered"),
        Conflict("conflict"),
        Unloaded("unloaded");

        private static final Problem[] Values = values();
        private final String Name;

        Problem(String Name) {
            this.Name = Name;
        }

        public static Problem byIndex(int Index) {
            return Index >= 0 && Index < Values.length ? Values[Index] : None;
        }

        public boolean hasSpot() {
            return this == TooClose || this == MissingCorner || this == CornerMissing || this == CornerUnpowered || this == Conflict;
        }

        public String getTranslationKey() {
            return "gui.scp_unleashed.instability_pylon.problem." + Name;
        }
    }

    public record Layout(BlockPos Anchor, int SizeX, int SizeZ, int Rise) {
        public boolean isBox() {
            return Rise > 0;
        }

        public int levels() {
            return isBox() ? 2 : 1;
        }

        public List<BlockPos> corners() {
            List<BlockPos> Found = new ArrayList<>(8);
            for (int Floor = 0; Floor < levels(); Floor++) {
                int Lift = Floor * Rise;
                Found.add(Anchor.offset(0, Lift, 0));
                Found.add(Anchor.offset(SizeX, Lift, 0));
                Found.add(Anchor.offset(0, Lift, SizeZ));
                Found.add(Anchor.offset(SizeX, Lift, SizeZ));
            }
            return Found;
        }

        public int cornerIndex(BlockPos Pos) {
            return corners().indexOf(Pos);
        }

        public AABB volume() {
            return new AABB(Anchor.getX() + 1, Anchor.getY(), Anchor.getZ() + 1, Anchor.getX() + SizeX, Anchor.getY() + Rise + GridTuning.PylonHeight, Anchor.getZ() + SizeZ);
        }

        public Vec3 centre() {
            return volume().getCenter();
        }

        public AABB bounds() {
            return new AABB(Anchor.getX(), Anchor.getY(), Anchor.getZ(), Anchor.getX() + SizeX + 1, Anchor.getY() + Rise + GridTuning.PylonHeight, Anchor.getZ() + SizeZ + 1);
        }

        public int cap() {
            return GridTuning.capFor(isBox());
        }

        public CompoundTag save() {
            CompoundTag Tag = new CompoundTag();
            Tag.putLong("Anchor", Anchor.asLong());
            Tag.putInt("SizeX", SizeX);
            Tag.putInt("SizeZ", SizeZ);
            Tag.putInt("Rise", Rise);
            return Tag;
        }

        @Nullable
        public static Layout load(CompoundTag Tag) {
            if (!Tag.contains("Anchor")) {
                return null;
            }
            return new Layout(BlockPos.of(Tag.getLong("Anchor")), Tag.getInt("SizeX"), Tag.getInt("SizeZ"), Tag.getInt("Rise"));
        }
    }

    public record Scan(@Nullable Layout Found, Problem Issue, @Nullable BlockPos Where) {
        public static Scan formed(Layout Found) {
            return new Scan(Found, Problem.None, null);
        }

        public static Scan failed(Problem Issue, @Nullable BlockPos Where) {
            return new Scan(null, Issue, Where);
        }
    }

    private record Partner(@Nullable BlockPos Pos, int Distance, @Nullable BlockPos Crowded) {
    }

    public static boolean isBase(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return false;
        }
        BlockState State = World.getBlockState(Pos);
        return State.getBlock() instanceof InstabilityPylonBlock && State.getValue(InstabilityPylonBlock.Part) == PylonPart.Base;
    }

    private static Partner partner(Level World, BlockPos Base, Direction Side) {
        for (int Step = 1; Step <= GridTuning.MaxSide; Step++) {
            BlockPos Probe = Base.relative(Side, Step);
            if (!World.isLoaded(Probe)) {
                return new Partner(null, 0, null);
            }
            if (isBase(World, Probe)) {
                return Step < GridTuning.MinSide ? new Partner(null, 0, Probe) : new Partner(Probe, Step, null);
            }
        }
        return new Partner(null, 0, null);
    }

    public static Scan scan(Level World, BlockPos Base) {
        Partner[] AlongX = {partner(World, Base, Direction.EAST), partner(World, Base, Direction.WEST)};
        Partner[] AlongZ = {partner(World, Base, Direction.SOUTH), partner(World, Base, Direction.NORTH)};
        Layout Best = null;
        BlockPos Missing = null;
        for (int IndexX = 0; IndexX < 2; IndexX++) {
            for (int IndexZ = 0; IndexZ < 2; IndexZ++) {
                Partner OnX = AlongX[IndexX];
                Partner OnZ = AlongZ[IndexZ];
                if (OnX.Pos() == null || OnZ.Pos() == null) {
                    continue;
                }
                BlockPos Opposite = new BlockPos(OnX.Pos().getX(), Base.getY(), OnZ.Pos().getZ());
                if (!isBase(World, Opposite)) {
                    if (Missing == null) {
                        Missing = Opposite;
                    }
                    continue;
                }
                int SignX = Signs[IndexX];
                int SignZ = Signs[IndexZ];
                BlockPos Anchor = new BlockPos(SignX > 0 ? Base.getX() : OnX.Pos().getX(), Base.getY(), SignZ > 0 ? Base.getZ() : OnZ.Pos().getZ());
                Layout Candidate = new Layout(Anchor, OnX.Distance(), OnZ.Distance(), 0);
                if (Best == null || better(Candidate, Best)) {
                    Best = Candidate;
                }
            }
        }
        if (Best == null) {
            BlockPos Crowded = firstCrowded(AlongX, AlongZ);
            boolean HasX = AlongX[0].Pos() != null || AlongX[1].Pos() != null;
            boolean HasZ = AlongZ[0].Pos() != null || AlongZ[1].Pos() != null;
            if (Missing != null) {
                return Scan.failed(Problem.MissingCorner, Missing);
            }
            if (Crowded != null) {
                return Scan.failed(Problem.TooClose, Crowded);
            }
            return Scan.failed(HasX ? Problem.NoPartnerZ : Problem.NoPartnerX, null);
        }
        return Scan.formed(stacked(World, Best));
    }

    @Nullable
    private static BlockPos firstCrowded(Partner[] AlongX, Partner[] AlongZ) {
        for (Partner Entry : AlongX) {
            if (Entry.Crowded() != null) {
                return Entry.Crowded();
            }
        }
        for (Partner Entry : AlongZ) {
            if (Entry.Crowded() != null) {
                return Entry.Crowded();
            }
        }
        return null;
    }

    private static boolean better(Layout Candidate, Layout Best) {
        long CandidateArea = (long) Candidate.SizeX() * Candidate.SizeZ();
        long BestArea = (long) Best.SizeX() * Best.SizeZ();
        if (CandidateArea != BestArea) {
            return CandidateArea < BestArea;
        }
        if (Candidate.Anchor().getX() != Best.Anchor().getX()) {
            return Candidate.Anchor().getX() < Best.Anchor().getX();
        }
        return Candidate.Anchor().getZ() < Best.Anchor().getZ();
    }

    private static Layout stacked(Level World, Layout Flat) {
        Layout Below = matching(World, Flat, -1);
        if (Below != null) {
            return Below;
        }
        Layout Above = matching(World, Flat, 1);
        return Above != null ? Above : Flat;
    }

    @Nullable
    private static Layout matching(Level World, Layout Flat, int Sign) {
        BlockPos Anchor = Flat.Anchor();
        for (int Rise = GridTuning.MinRise; Rise <= GridTuning.MaxRise; Rise++) {
            BlockPos Probe = Anchor.above(Rise * Sign);
            if (World.isOutsideBuildHeight(Probe)) {
                return null;
            }
            if (!isBase(World, Probe)) {
                continue;
            }
            Layout Shifted = new Layout(Probe, Flat.SizeX(), Flat.SizeZ(), 0);
            for (BlockPos Corner : Shifted.corners()) {
                if (!isBase(World, Corner)) {
                    return null;
                }
            }
            return Sign < 0 ? new Layout(Probe, Flat.SizeX(), Flat.SizeZ(), Rise) : new Layout(Anchor, Flat.SizeX(), Flat.SizeZ(), Rise);
        }
        return null;
    }
}
