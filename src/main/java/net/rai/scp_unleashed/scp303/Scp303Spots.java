package net.rai.scp_unleashed.scp303;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp303Spots {
    public static final int Attempts = 36;
    public static final int Vertical = 6;
    public static final double BehindSpread = 110.0;
    public static final double CoverMin = 2.0;
    public static final double CoverMax = 8.0;
    public static final double StalkKeep = 9.0;

    private Scp303Spots() {
    }

    public static boolean standable(ServerLevel World, Vec3 Spot) {
        BlockPos Feet = BlockPos.containing(Spot);
        if (!World.isLoaded(Feet) || !World.getWorldBorder().isWithinBounds(Feet) || World.isOutsideBuildHeight(Feet.above(3))) {
            return false;
        }
        return World.noCollision(Scp303Doors.standBox(Spot)) && Scp303Doors.grounded(World, Spot) && !World.containsAnyLiquid(Scp303Doors.standBox(Spot).inflate(0.5, 0.0, 0.5));
    }

    public static Optional<Vec3> floorNear(ServerLevel World, double X, double Z, int BaseY) {
        for (int Step = 0; Step <= Vertical * 2; Step++) {
            int Dy = Step % 2 == 0 ? Step / 2 : -(Step + 1) / 2;
            Vec3 Spot = new Vec3(Mth.floor(X) + 0.5, BaseY + Dy, Mth.floor(Z) + 0.5);
            if (standable(World, Spot)) {
                return Optional.of(Spot);
            }
        }
        return Optional.empty();
    }

    public static boolean hiddenFrom(ServerLevel World, Vec3 Spot, @Nullable LivingEntity Target, boolean Lenient) {
        boolean Open = Scp303Sight.exposed(World, Spot, Scp303Doorman.Height) || Target != null && !(Target instanceof Player) && Scp303Sight.inLine(Target, Spot, Scp303Doorman.Height);
        if (!Open) {
            return true;
        }
        if (!Lenient || Scp303Sight.watched(World, Spot, Scp303Doorman.Height)) {
            return false;
        }
        return Target == null || Scp303Sight.shaded(World, Spot, Target);
    }

    public static Optional<Vec3> cover(ServerLevel World, Scp303Doorman Doorman, LivingEntity Target, RandomSource Random) {
        Vec3 Best = null;
        double BestScore = -Double.MAX_VALUE;
        for (int Attempt = 0; Attempt < Attempts / 2; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = CoverMin + Random.nextDouble() * (CoverMax - CoverMin);
            Optional<Vec3> Found = floorNear(World, Doorman.getX() + Math.cos(Angle) * Radius, Doorman.getZ() + Math.sin(Angle) * Radius, Doorman.getBlockY());
            if (Found.isEmpty() || Scp303Sight.inLine(Target, Found.get(), Scp303Doorman.Height)) {
                continue;
            }
            double Score = -Found.get().distanceTo(Doorman.position()) - Math.max(0.0, StalkKeep - Found.get().distanceTo(Target.position())) * 2.0;
            if (Score > BestScore) {
                BestScore = Score;
                Best = Found.get();
            }
        }
        return Optional.ofNullable(Best);
    }

    public static Optional<Vec3> hidden(ServerLevel World, LivingEntity Target, double Min, double Max, RandomSource Random, @Nullable Vec3 Near, boolean Behind, boolean Lenient) {
        Vec3 Look = Target.getViewVector(1.0f);
        double Facing = Math.atan2(Look.z, Look.x);
        Vec3 Best = null;
        double BestScore = -Double.MAX_VALUE;
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Behind ? Facing + Math.PI + Math.toRadians((Random.nextDouble() - 0.5) * 2.0 * BehindSpread) : Random.nextDouble() * Math.PI * 2.0;
            double Radius = Min + Random.nextDouble() * Math.max(0.0, Max - Min);
            Optional<Vec3> Found = floorNear(World, Target.getX() + Math.cos(Angle) * Radius, Target.getZ() + Math.sin(Angle) * Radius, Target.getBlockY());
            if (Found.isEmpty()) {
                continue;
            }
            Vec3 Spot = Found.get();
            if (!hiddenFrom(World, Spot, Target, Lenient)) {
                continue;
            }
            double Score = -Math.abs(Spot.y - Target.getY()) * 0.5;
            if (Near != null) {
                Score -= Spot.distanceTo(Near) * 0.6;
            }
            if (Scp303Sight.shaded(World, Spot, Target)) {
                Score += 2.0;
            }
            if (Score > BestScore) {
                BestScore = Score;
                Best = Spot;
            }
        }
        return Optional.ofNullable(Best);
    }
}
