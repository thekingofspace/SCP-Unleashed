package net.rai.scp_unleashed.flamethrower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class FlamerStream {
    public static final double Step = 0.5;
    public static final double NozzleReach = 0.9;
    public static final double NozzleDrop = 0.3;
    public static final double NozzleSide = 0.16;
    public static final double BaseRadius = 0.35;
    public static final float HalfAngle = 8.0F;
    public static final float AimedHalfAngle = 6.0F;
    public static final double ArcDrop = 0.5;
    public static final float RainShare = 0.65F;
    public static final float FirstPulse = 0.5F;
    public static final float SecondPulse = 0.8F;
    public static final int StreakGap = 6;
    public static final float VulnerableScale = 2.5F;
    public static final int BurnTicks = 100;
    public static final int VulnerableBurnTicks = 240;
    public static final int MaxTargets = 16;
    public static final float DripChance = 0.3F;
    public static final double DripStart = 3.0;
    private static final Map<UUID, long[]> Streaks = new HashMap<>();

    private FlamerStream() {
    }

    public record Hit(Entity Target, double Distance, float Amount, boolean Vulnerable, boolean Landed) {
    }

    public record Pulse(Vec3 Nozzle, Vec3 End, double Reach, @Nullable BlockHitResult Surface, boolean Doused, boolean Fresh, List<Hit> Hits, int Burned) {
    }

    public static GunStats stats() {
        return GunKind.Flamethrower.getStats();
    }

    public static Vec3 nozzle(LivingEntity User) {
        Vec3 Eye = User.getEyePosition();
        Vec3 Look = User.getLookAngle();
        Vec3 Flat = new Vec3(-Look.z, 0.0, Look.x);
        Flat = Flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : Flat.normalize();
        double Side = User.getMainArm() == HumanoidArm.LEFT ? -NozzleSide : NozzleSide;
        Vec3 Wanted = Eye.add(Look.scale(NozzleReach)).add(Flat.scale(Side)).add(0.0, -NozzleDrop, 0.0);
        BlockHitResult Wall = User.level().clip(new ClipContext(Eye, Wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User));
        return Wall.getType() == HitResult.Type.MISS ? Wanted : Wall.getLocation().subtract(Look.scale(0.05));
    }

    public static boolean submerged(LivingEntity User) {
        Level World = User.level();
        return User.isEyeInFluid(FluidTags.WATER) || World.getFluidState(BlockPos.containing(nozzle(User))).is(FluidTags.WATER);
    }

    public static boolean rained(LivingEntity User) {
        return User.level().isRainingAt(BlockPos.containing(nozzle(User)).above());
    }

    public static float spinUp(int Pulse) {
        return Pulse <= 0 ? FirstPulse : Pulse == 1 ? SecondPulse : 1.0F;
    }

    public static int streak(LivingEntity User, long Now) {
        long[] Found = Streaks.computeIfAbsent(User.getUUID(), Id -> new long[]{Long.MIN_VALUE / 2, 0L});
        Found[1] = Now - Found[0] <= StreakGap ? Found[1] + 1 : 0L;
        Found[0] = Now;
        return (int) Math.min(Found[1], Integer.MAX_VALUE);
    }

    public static void forget(UUID Id) {
        Streaks.remove(Id);
    }

    public static void forgetAll() {
        Streaks.clear();
    }

    public static double reach(LivingEntity User, int Pulse) {
        return stats().getRange() * spinUp(Pulse) * (rained(User) ? RainShare : 1.0F);
    }

    public static Vec3 along(Vec3 Nozzle, Vec3 Look, double Distance) {
        double Share = Distance / stats().getRange();
        return Nozzle.add(Look.scale(Distance)).add(0.0, -ArcDrop * Share * Share, 0.0);
    }

    public static double radius(double Distance, boolean Aimed) {
        return BaseRadius + Distance * Math.tan(Math.toRadians(Aimed ? AimedHalfAngle : HalfAngle));
    }

    public static float damageAt(double Distance, boolean Vulnerable) {
        return stats().getDamage() * stats().falloff(Distance) * (Vulnerable ? VulnerableScale : 1.0F);
    }

    public static DamageSource source(ServerLevel World, LivingEntity User) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FlamerRegistry.StreamDamage), User, User, nozzle(User));
    }

    public static Pulse pulse(ServerLevel World, LivingEntity User, long Now, FlamerConfig.FireRule Rule, boolean Flesh, Predicate<BlockState> Burnable) {
        int Count = streak(User, Now);
        boolean Aimed = User.isUsingItem();
        Vec3 Nozzle = nozzle(User);
        Vec3 Look = User.getLookAngle();
        double Range = reach(User, Count);
        List<Vec3> Path = new ArrayList<>();
        Path.add(Nozzle);
        BlockHitResult Surface = null;
        boolean Doused = false;
        double Reach = Range;
        for (double Distance = Step; Distance <= Range + 1.0E-6; Distance += Step) {
            Vec3 From = Path.get(Path.size() - 1);
            Vec3 To = along(Nozzle, Look, Math.min(Distance, Range));
            BlockHitResult Wall = World.clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, User));
            if (Wall.getType() != HitResult.Type.MISS) {
                Reach = Distance - Step + From.distanceTo(Wall.getLocation());
                Path.add(Wall.getLocation());
                Doused = World.getFluidState(Wall.getBlockPos()).is(FluidTags.WATER);
                Surface = Doused ? null : Wall;
                break;
            }
            Path.add(To);
        }
        Vec3 End = Path.get(Path.size() - 1);
        List<Hit> Hits = strike(World, User, Nozzle, Look, Path, Aimed);
        int Burned = 0;
        if (Surface != null) {
            Burned = FlamerWorld.scorch(World, User, Surface, Now, Rule, Flesh, Burnable);
        }
        int Near = (int) Math.ceil(DripStart / Step);
        if (Path.size() > Near + 1 && User.getRandom().nextFloat() < DripChance) {
            Vec3 Spot = Path.get(Near + User.getRandom().nextInt(Path.size() - Near));
            if (Spot.distanceToSqr(User.position()) > DripStart * DripStart) {
                FlamerWorld.drip(World, User, Spot, Rule);
            }
        }
        return new Pulse(Nozzle, End, Reach, Surface, Doused, Count == 0, Hits, Burned);
    }

    private static List<Hit> strike(ServerLevel World, LivingEntity User, Vec3 Nozzle, Vec3 Look, List<Vec3> Path, boolean Aimed) {
        AABB Area = new AABB(Nozzle, Nozzle).inflate(1.0);
        for (Vec3 Point : Path) {
            Area = Area.minmax(new AABB(Point, Point));
        }
        Area = Area.inflate(radius(stats().getRange(), false) + 0.5);
        Entity Ride = User.getRootVehicle();
        List<Hit> Hits = new ArrayList<>();
        Set<Entity> Bodies = new HashSet<>();
        List<Entity> Candidates = World.getEntities(User, Area, Candidate -> struckBy(User, Candidate, Ride));
        Candidates.sort(Comparator.comparingDouble(Candidate -> Candidate.distanceToSqr(Nozzle)));
        DamageSource Source = source(World, User);
        for (Entity Candidate : Candidates) {
            if (Hits.size() >= MaxTargets) {
                break;
            }
            double Distance = touch(Candidate.getBoundingBox(), Nozzle, Look, Path, Aimed);
            if (Distance < 0.0) {
                continue;
            }
            Entity Body = Candidate instanceof PartEntity<?> Part ? Part.getParent() : Candidate;
            if (!Bodies.add(Body) || !visible(World, User, Nozzle, Candidate.getBoundingBox())) {
                continue;
            }
            boolean Vulnerable = FlamerBurns.vulnerable(Candidate);
            float Amount = damageAt(Distance, Vulnerable);
            Candidate.invulnerableTime = 0;
            boolean Landed = Candidate.hurt(Source, Amount);
            if (Landed && Body instanceof LivingEntity Living && Living.isAlive()) {
                if (Vulnerable) {
                    FlamerBurns.kindle(World, Living, VulnerableBurnTicks, User, 0);
                } else {
                    Living.igniteForTicks(BurnTicks);
                }
            }
            Hits.add(new Hit(Candidate, Distance, Amount, Vulnerable, Landed));
        }
        return Hits;
    }

    public static double touch(AABB Box, Vec3 Nozzle, Vec3 Look, List<Vec3> Path, boolean Aimed) {
        double Walked = 0.0;
        for (int Index = 1; Index < Path.size(); Index++) {
            Vec3 From = Path.get(Index - 1);
            Vec3 To = Path.get(Index);
            double Length = From.distanceTo(To);
            int Samples = Math.max(1, (int) Math.ceil(Length / 0.25));
            for (int Sample = 0; Sample <= Samples; Sample++) {
                double Share = Sample / (double) Samples;
                Vec3 Point = From.add(To.subtract(From).scale(Share));
                double Distance = Walked + Length * Share;
                if (gap(Box, Point) <= radius(Distance, Aimed)) {
                    return Distance;
                }
            }
            Walked += Length;
        }
        return -1.0;
    }

    private static double gap(AABB Box, Vec3 Point) {
        double X = Math.max(0.0, Math.max(Box.minX - Point.x, Point.x - Box.maxX));
        double Y = Math.max(0.0, Math.max(Box.minY - Point.y, Point.y - Box.maxY));
        double Z = Math.max(0.0, Math.max(Box.minZ - Point.z, Point.z - Box.maxZ));
        return Math.sqrt(X * X + Y * Y + Z * Z);
    }

    public static boolean visible(Level World, Entity User, Vec3 Nozzle, AABB Box) {
        Vec3 Centre = Box.getCenter();
        Vec3[] Targets = {Centre, new Vec3(Centre.x, Box.minY + Box.getYsize() * 0.85, Centre.z), new Vec3(Centre.x, Box.minY + Box.getYsize() * 0.15, Centre.z),
                new Vec3(Mth.clamp(Nozzle.x, Box.minX, Box.maxX), Mth.clamp(Nozzle.y, Box.minY, Box.maxY), Mth.clamp(Nozzle.z, Box.minZ, Box.maxZ))};
        for (Vec3 Target : Targets) {
            if (World.clip(new ClipContext(Nozzle, Target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    private static boolean struckBy(LivingEntity User, Entity Candidate, Entity Ride) {
        if (!Candidate.isAlive() || Candidate.isSpectator() || Candidate.getRootVehicle() == Ride) {
            return false;
        }
        if (Candidate instanceof PartEntity<?> Part) {
            return Part.getParent() != User && Part.getParent().isAlive();
        }
        if (!(Candidate instanceof LivingEntity)) {
            return false;
        }
        return !(Candidate instanceof OwnableEntity Pet) || !User.getUUID().equals(Pet.getOwnerUUID());
    }
}
