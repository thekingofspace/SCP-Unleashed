package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import org.jetbrains.annotations.Nullable;

public final class CourtKeeper {
    public static final double Skirt = ScarletThrone.PillarWidth + 1.2;
    public static final double Margin = 0.5;
    public static final double PushStrength = 0.06;
    public static final double PushCap = 0.18;
    public static final int CheckEvery = 10;
    public static final int StuckChecks = 4;
    public static final double StuckMove = 0.3;
    public static final int Reach = 9;
    public static final double EdgeMargin = 2.5;

    public static final class Watch {
        double LastX = Double.NaN;
        double LastZ = Double.NaN;
        int Still;
        int Moves;

        public int moves() {
            return Moves;
        }
    }

    private CourtKeeper() {
    }

    public static Vec3 pillarCentre(BlockPos Dais, int Index) {
        return Vec3.atBottomCenterOf(CourtArena.pillarBase(Dais, Index));
    }

    public static double pillarGap(BlockPos Dais, double X, double Z) {
        double Best = Double.MAX_VALUE;
        for (int Index = 0; Index < CourtArena.Pillars; Index++) {
            Vec3 Centre = pillarCentre(Dais, Index);
            double Dx = X - Centre.x;
            double Dz = Z - Centre.z;
            Best = Math.min(Best, Math.sqrt(Dx * Dx + Dz * Dz));
        }
        return Best;
    }

    public static double keep(double Half) {
        return Skirt + Half + Margin;
    }

    public static boolean clearOfPillars(BlockPos Dais, double X, double Z, double Half) {
        return pillarGap(Dais, X, Z) >= keep(Half);
    }

    public static boolean fits(ServerLevel World, Vec3 Feet, double Width, double Height) {
        double Half = Width / 2.0;
        AABB Box = new AABB(Feet.x - Half, Feet.y + 0.01, Feet.z - Half, Feet.x + Half, Feet.y + Height, Feet.z + Half);
        return World.noCollision(Box) && !World.containsAnyLiquid(Box);
    }

    public static boolean free(ServerLevel World, BlockPos Dais, Vec3 Feet, double Width, double Height) {
        return clearOfPillars(Dais, Feet.x, Feet.z, Width / 2.0) && fits(World, Feet, Width, Height);
    }

    @Nullable
    public static Vec3 clear(ServerLevel World, BlockPos Dais, Vec3 Near, double Width, double Height, int Limit) {
        double Half = Width / 2.0;
        Vec3 Centre = Vec3.atBottomCenterOf(Dais);
        double Edge = CourtArena.floorRadius() - EdgeMargin - Half;
        for (int Ring = 0; Ring <= Limit; Ring++) {
            int Steps = Ring == 0 ? 1 : Math.max(8, Ring * 6);
            for (int Step = 0; Step < Steps; Step++) {
                double Angle = Step * Math.PI * 2.0 / Steps + Ring * 0.37;
                double X = Near.x + Math.cos(Angle) * Ring;
                double Z = Near.z + Math.sin(Angle) * Ring;
                double Dx = X - Centre.x;
                double Dz = Z - Centre.z;
                if (Dx * Dx + Dz * Dz > Edge * Edge || !clearOfPillars(Dais, X, Z, Half)) {
                    continue;
                }
                BlockPos Ground = CourtArena.ground(World, BlockPos.containing(X, Near.y, Z), 8);
                if (Ground == null) {
                    continue;
                }
                Vec3 Feet = new Vec3(X, Ground.getY(), Z);
                if (fits(World, Feet, Width, Height)) {
                    return Feet;
                }
            }
        }
        return null;
    }

    public static Vec3 clearOr(ServerLevel World, BlockPos Dais, Vec3 Near, double Width, double Height) {
        Vec3 Found = clear(World, Dais, Near, Width, Height, Reach);
        if (Found != null) {
            return Found;
        }
        Vec3 Inward = Near.add(Vec3.atBottomCenterOf(Dais).subtract(Near).multiply(0.5, 0.0, 0.5));
        Found = clear(World, Dais, Inward, Width, Height, Reach);
        return Found != null ? Found : Near;
    }

    public static BlockPos clearBlock(ServerLevel World, BlockPos Dais, BlockPos Near, double Width, double Height) {
        Vec3 Found = clearOr(World, Dais, Vec3.atBottomCenterOf(Near), Width, Height);
        return BlockPos.containing(Found.x, Found.y + 0.01, Found.z);
    }

    public static Vec3 away(BlockPos Dais, Vec3 From, double Half) {
        int Nearest = 0;
        double Best = Double.MAX_VALUE;
        for (int Index = 0; Index < CourtArena.Pillars; Index++) {
            double Distance = pillarCentre(Dais, Index).distanceToSqr(From.x, pillarCentre(Dais, Index).y, From.z);
            if (Distance < Best) {
                Best = Distance;
                Nearest = Index;
            }
        }
        Vec3 Centre = pillarCentre(Dais, Nearest);
        Vec3 Way = new Vec3(From.x - Centre.x, 0.0, From.z - Centre.z);
        if (Way.lengthSqr() < 1.0E-4) {
            Way = Vec3.atBottomCenterOf(Dais).subtract(Centre).multiply(1.0, 0.0, 1.0);
        }
        Way = Way.normalize();
        double Need = keep(Half) + 0.5;
        return Math.sqrt(Best) < Need ? new Vec3(Centre.x + Way.x * Need, From.y, Centre.z + Way.z * Need) : From;
    }

    public static boolean inside(ServerLevel World, Mob Body) {
        return !World.noCollision(Body, Body.getBoundingBox().deflate(0.05));
    }

    public static void push(BlockPos Dais, Mob Body) {
        double Half = Body.getBbWidth() / 2.0;
        double Keep = keep(Half);
        double Px = 0.0;
        double Pz = 0.0;
        for (int Index = 0; Index < CourtArena.Pillars; Index++) {
            Vec3 Centre = pillarCentre(Dais, Index);
            double Dx = Body.getX() - Centre.x;
            double Dz = Body.getZ() - Centre.z;
            double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
            if (Distance < Keep && Distance > 1.0E-3) {
                double Strength = (Keep - Distance) * PushStrength;
                Px += Dx / Distance * Strength;
                Pz += Dz / Distance * Strength;
            }
        }
        double Length = Math.sqrt(Px * Px + Pz * Pz);
        if (Length > PushCap) {
            Px *= PushCap / Length;
            Pz *= PushCap / Length;
        }
        if (Length > 0.0) {
            Body.setDeltaMovement(Body.getDeltaMovement().add(Px, 0.0, Pz));
        }
    }

    public static boolean keep(ServerLevel World, Mob Body, BlockPos Dais, Watch State, boolean WantsMove) {
        push(Dais, Body);
        if (Body.tickCount % CheckEvery != 0) {
            return false;
        }
        double Moved = Double.isNaN(State.LastX) ? 1.0 : Math.sqrt(Math.pow(Body.getX() - State.LastX, 2) + Math.pow(Body.getZ() - State.LastZ, 2));
        State.LastX = Body.getX();
        State.LastZ = Body.getZ();
        double Half = Body.getBbWidth() / 2.0;
        boolean Close = pillarGap(Dais, Body.getX(), Body.getZ()) < keep(Half);
        if (inside(World, Body)) {
            State.Still = StuckChecks;
        } else if (WantsMove && Moved < StuckMove && (Close || Body.horizontalCollision)) {
            State.Still++;
        } else if (Close && Moved < StuckMove) {
            State.Still = Math.max(State.Still, StuckChecks - 2) + 1;
        } else {
            State.Still = 0;
        }
        if (State.Still < StuckChecks) {
            return false;
        }
        State.Still = 0;
        return relocate(World, Body, Dais, State);
    }

    public static boolean relocate(ServerLevel World, Mob Body, BlockPos Dais, Watch State) {
        double Half = Body.getBbWidth() / 2.0;
        Vec3 Start = away(Dais, Body.position(), Half);
        Vec3 Spot = clear(World, Dais, Start, Body.getBbWidth(), Body.getBbHeight(), Reach);
        if (Spot == null) {
            Spot = clear(World, Dais, Vec3.atBottomCenterOf(Dais), Body.getBbWidth(), Body.getBbHeight(), Reach * 2);
        }
        if (Spot == null) {
            return false;
        }
        Vec3 From = Body.position();
        World.sendParticles(ParticleTypes.LARGE_SMOKE, From.x, From.y + Body.getBbHeight() * 0.5, From.z, 16, Half, Body.getBbHeight() * 0.3, Half, 0.02);
        World.sendParticles(CourtFx.DeepCrimson, Spot.x, Spot.y + 0.5, Spot.z, 20, Half, 0.4, Half, 0.02);
        Body.getNavigation().stop();
        Body.setDeltaMovement(Vec3.ZERO);
        Body.teleportTo(Spot.x, Spot.y, Spot.z);
        State.Moves++;
        State.LastX = Double.NaN;
        return true;
    }
}
