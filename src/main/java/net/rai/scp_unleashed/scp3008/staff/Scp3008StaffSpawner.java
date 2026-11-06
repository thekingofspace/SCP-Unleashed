package net.rai.scp_unleashed.scp3008.staff;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp3008.Scp3008Hours;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp3008StaffSpawner {
    public static final int DayInterval = 100;
    public static final int NightInterval = 40;
    public static final int DayPerPlayer = 3;
    public static final int NightPerPlayer = 7;
    public static final int DayLevelCap = 24;
    public static final int NightLevelCap = 48;
    public static final double CountRadius = 48.0;
    public static final double DayMin = 24.0;
    public static final double DayMax = 44.0;
    public static final double NightMin = 18.0;
    public static final double NightMax = 36.0;
    public static final double DespawnDistance = 96.0;
    public static final double SightRange = 128.0;
    public static final double SightCone = 0.45;
    public static final double TooClose = 0.75;
    public static final int Attempts = 8;
    public static final int Rise = 6;
    public static final int CullInterval = 100;
    public static final int DayLeeway = 1;
    public static final double CullNear = 16.0;

    private Scp3008StaffSpawner() {
    }

    public static void tick(ServerLevel World) {
        if (!Scp3008Hours.inStore(World)) {
            return;
        }
        boolean Night = Scp3008Hours.night(World);
        long Time = World.getGameTime();
        if (Time % CullInterval == 0) {
            cull(World, Night);
        }
        if (!World.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        if (Time % (Night ? NightInterval : DayInterval) == 0) {
            populate(World, Night, World.players(), Night ? NightMin : DayMin, Night ? NightMax : DayMax);
        }
    }

    public static int perPlayer(boolean Night) {
        return Night ? NightPerPlayer : DayPerPlayer;
    }

    public static int levelCap(boolean Night) {
        return Night ? NightLevelCap : DayLevelCap;
    }

    public static int allowedFor(boolean Night, int Near, int Total) {
        return Math.max(0, Math.min(perPlayer(Night) - Near, levelCap(Night) - Total));
    }

    public static int countNear(ServerLevel World, Vec3 Where, double Radius) {
        return World.getEntitiesOfClass(Scp3008Staff.class, new AABB(Where, Where).inflate(Radius),
                Each -> Each.isAlive() && Each.distanceToSqr(Where) <= Radius * Radius).size();
    }

    public static int countAll(ServerLevel World) {
        return World.getEntities(Scp3008StaffRegistry.Staff.get(), Scp3008Staff::isAlive).size();
    }

    public static int populate(ServerLevel World, boolean Night, List<? extends Player> Players, double Min, double Max) {
        int Made = 0;
        for (Player User : new ArrayList<>(Players)) {
            if (User.isSpectator() || !User.isAlive()) {
                continue;
            }
            if (allowedFor(Night, countNear(World, User.position(), CountRadius), countAll(World)) <= 0) {
                continue;
            }
            if (spawnNear(World, User, Min, Max) != null) {
                Made++;
            }
        }
        return Made;
    }

    @Nullable
    public static Scp3008Staff spawnNear(ServerLevel World, Player User, double Min, double Max) {
        RandomSource Random = World.getRandom();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = Min + Random.nextDouble() * (Max - Min);
            int X = Mth.floor(User.getX() + Math.cos(Angle) * Distance);
            int Z = Mth.floor(User.getZ() + Math.sin(Angle) * Distance);
            Scp3008StaffVariant Kind = Scp3008StaffVariant.random(Random);
            BlockPos Ground = ground(World, X, User.getBlockY(), Z, Kind.Height);
            if (Ground == null) {
                continue;
            }
            Vec3 Spot = Vec3.atBottomCenterOf(Ground);
            if (seen(World, Spot, Kind.Height, Min)) {
                continue;
            }
            Scp3008Staff Staff = Scp3008StaffRegistry.Staff.get().create(World);
            if (Staff == null) {
                return null;
            }
            Staff.setVariant(Kind);
            Staff.moveTo(Spot.x, Spot.y, Spot.z, Random.nextFloat() * 360.0f, 0.0f);
            if (!World.noCollision(Staff)) {
                Staff.discard();
                continue;
            }
            Staff.finalizeSpawn(World, World.getCurrentDifficultyAt(Ground), MobSpawnType.EVENT, null);
            Staff.setStoreBorn(true);
            if (World.addFreshEntity(Staff)) {
                return Staff;
            }
        }
        return null;
    }

    @Nullable
    public static BlockPos ground(ServerLevel World, int X, int Y, int Z, float Height) {
        for (int Step = 0; Step <= Rise; Step++) {
            for (int Sign : new int[]{1, -1}) {
                BlockPos Pos = new BlockPos(X, Y + Step * Sign, Z);
                if (World.isLoaded(Pos) && Scp3008StaffGoals.standable(World, Pos, Height)) {
                    return Pos;
                }
                if (Step == 0) {
                    break;
                }
            }
        }
        return null;
    }

    public static boolean seen(ServerLevel World, Vec3 Spot, float Height, double Min) {
        Vec3 Middle = Spot.add(0.0, Height * 0.6, 0.0);
        for (ServerPlayer Viewer : World.players()) {
            if (Viewer.isSpectator()) {
                continue;
            }
            Vec3 Eye = Viewer.getEyePosition();
            double Distance = Eye.distanceTo(Middle);
            if (Distance < Min * TooClose) {
                return true;
            }
            if (Distance > SightRange) {
                continue;
            }
            Vec3 Toward = Middle.subtract(Eye).normalize();
            if (Viewer.getViewVector(1.0f).dot(Toward) < SightCone) {
                continue;
            }
            if (World.clip(new ClipContext(Eye, Middle, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    public static void cull(ServerLevel World, boolean Night) {
        List<? extends Scp3008Staff> All = World.getEntities(Scp3008StaffRegistry.Staff.get(), Each -> Each.isAlive() && Each.storeBorn());
        for (Scp3008Staff Staff : All) {
            Player Nearest = World.getNearestPlayer(Staff, -1.0);
            if (Nearest == null || Nearest.distanceToSqr(Staff) > DespawnDistance * DespawnDistance) {
                Staff.discard();
            }
        }
        if (Night) {
            return;
        }
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator()) {
                continue;
            }
            List<Scp3008Staff> Near = new ArrayList<>(World.getEntitiesOfClass(Scp3008Staff.class, User.getBoundingBox().inflate(CountRadius),
                    Each -> Each.isAlive() && Each.storeBorn() && !Each.engaged() && Each.distanceToSqr(User) <= CountRadius * CountRadius));
            if (Near.size() <= DayPerPlayer + DayLeeway) {
                continue;
            }
            Near.sort(Comparator.comparingDouble(Each -> -Each.distanceToSqr(User)));
            for (Scp3008Staff Staff : Near) {
                if (Staff.distanceToSqr(User) > CullNear * CullNear && !seen(World, Staff.position(), Staff.getBbHeight(), 0.0)) {
                    Staff.discard();
                    break;
                }
            }
        }
    }
}
