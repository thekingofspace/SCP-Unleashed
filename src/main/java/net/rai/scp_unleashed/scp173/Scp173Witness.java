package net.rai.scp_unleashed.scp173;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class Scp173Witness {
    public static final double Range = 32.0;
    public static final double DarkRange = 15.0;
    public static final double HalfWidth = 1.19;
    public static final double HalfHeight = 0.7;
    public static final double NearDepth = 0.05;
    public static final double HeadShare = 0.9;
    public static final double ChestShare = 0.62;
    public static final double WaistShare = 0.38;
    public static final double FeetLift = 0.15;
    public static final double Shoulder = 0.3;
    public static final int RayTicks = 4;
    public static final int PruneTicks = 100;

    private Scp173Witness() {
    }

    public static final class Memo {
        private Vec3 Eye = Vec3.ZERO;
        private AABB Box = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        private long Tick = Long.MIN_VALUE;
        private int Known;
        private int Clear;

        private boolean fresh(Vec3 From, AABB Seen, long Now) {
            return Now - Tick < RayTicks && Now >= Tick && Eye.equals(From) && Box.equals(Seen);
        }

        private void reset(Vec3 From, AABB Seen, long Now) {
            Eye = From;
            Box = Seen;
            Tick = Now;
            Known = 0;
            Clear = 0;
        }
    }

    public static double reach(LivingEntity Viewer) {
        return Viewer.hasEffect(MobEffects.DARKNESS) ? DarkRange : Range;
    }

    public static boolean blinded(LivingEntity Viewer) {
        return Senses.isBlindfolded(Viewer) || Viewer.hasEffect(MobEffects.BLINDNESS);
    }

    public static boolean canSee(LivingEntity Viewer, AABB Box) {
        return canSee(Viewer, Box, null);
    }

    public static boolean canSee(LivingEntity Viewer, Scp173Statue Statue) {
        return canSee(Viewer, Statue, Statue.sightBox());
    }

    public static boolean canSee(LivingEntity Viewer, Scp173Statue Statue, AABB Box) {
        return Viewer.level() == Statue.level() && canSee(Viewer, Box, Statue.glances());
    }

    private static boolean canSee(LivingEntity Viewer, AABB Box, @Nullable Map<Integer, Memo> Cache) {
        if (blinded(Viewer)) {
            return false;
        }
        double Limit = reach(Viewer);
        Vec3 Eye = Viewer.getEyePosition();
        double Loose = Limit + Box.getSize();
        if (Eye.distanceToSqr(Box.getCenter()) > Loose * Loose) {
            return false;
        }
        Vec3 Forward = Viewer.getViewVector(1.0f);
        Vec3 Up = Viewer.getUpVector(1.0f);
        Vec3 Right = Forward.cross(Up);
        List<Vec3> Points = points(Eye, Box);
        Memo Entry = null;
        for (int Index = 0; Index < Points.size(); Index++) {
            Vec3 Point = Points.get(Index);
            Vec3 Toward = Point.subtract(Eye);
            if (Toward.lengthSqr() > Limit * Limit || !framed(Toward, Forward, Up, Right)) {
                continue;
            }
            if (Cache != null && Entry == null) {
                Entry = memo(Cache, Viewer, Eye, Box);
            }
            if (open(Entry, Index, Viewer.level(), Eye, Point)) {
                return true;
            }
        }
        return false;
    }

    public static boolean framed(Vec3 Toward, Vec3 Forward, Vec3 Up, Vec3 Right) {
        double Depth = Toward.dot(Forward);
        if (Depth <= NearDepth) {
            return false;
        }
        return Math.abs(Toward.dot(Right)) <= HalfWidth * Depth && Math.abs(Toward.dot(Up)) <= HalfHeight * Depth;
    }

    public static List<Vec3> points(Vec3 Eye, AABB Box) {
        Vec3 Centre = Box.getCenter();
        double Height = Box.maxY - Box.minY;
        double Dx = Centre.x - Eye.x;
        double Dz = Centre.z - Eye.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        double Sx = Flat < 1.0e-4 ? 1.0 : -Dz / Flat;
        double Sz = Flat < 1.0e-4 ? 0.0 : Dx / Flat;
        double Spread = Math.min(Shoulder, Math.min(Box.maxX - Box.minX, Box.maxZ - Box.minZ) * 0.5);
        double Chest = Box.minY + Height * ChestShare;
        return List.of(new Vec3(Centre.x, Box.minY + Height * HeadShare, Centre.z), new Vec3(Centre.x, Chest, Centre.z),
                new Vec3(Centre.x, Box.minY + Height * WaistShare, Centre.z), new Vec3(Centre.x, Box.minY + FeetLift, Centre.z),
                new Vec3(Centre.x + Sx * Spread, Chest, Centre.z + Sz * Spread), new Vec3(Centre.x - Sx * Spread, Chest, Centre.z - Sz * Spread));
    }

    private static Memo memo(Map<Integer, Memo> Cache, LivingEntity Viewer, Vec3 Eye, AABB Box) {
        long Now = Viewer.level().getGameTime();
        Memo Entry = Cache.computeIfAbsent(Viewer.getId(), Key -> new Memo());
        if (!Entry.fresh(Eye, Box, Now)) {
            Entry.reset(Eye, Box, Now);
        }
        return Entry;
    }

    private static boolean open(@Nullable Memo Entry, int Index, Level World, Vec3 Eye, Vec3 Point) {
        if (Entry == null) {
            return Scp173Sight.clear(World, Eye, Point);
        }
        int Bit = 1 << Index;
        if ((Entry.Known & Bit) == 0) {
            Entry.Known |= Bit;
            if (Scp173Sight.clear(World, Eye, Point)) {
                Entry.Clear |= Bit;
            }
        }
        return (Entry.Clear & Bit) != 0;
    }

    public static void prune(Map<Integer, Memo> Cache, long Now) {
        Cache.values().removeIf(Entry -> Now - Entry.Tick >= RayTicks || Now < Entry.Tick);
    }
}
