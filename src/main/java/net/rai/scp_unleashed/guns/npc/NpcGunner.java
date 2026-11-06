package net.rai.scp_unleashed.guns.npc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunFiring;
import net.rai.scp_unleashed.guns.GunGlass;
import net.rai.scp_unleashed.guns.GunHeat;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunReloading;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.scp127.Scp127Item;
import net.rai.scp_unleashed.scp127.Scp127Levels;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class NpcGunner {
    public static final float MissSlop = 4.0F;
    public static final float MovingShare = 0.5F;
    public static final double LeadTicks = 1.0;
    public static final float HeadPreference = 2.0F;
    public static final float HeadAccuracy = 0.75F;
    public static final double FriendlyMargin = 0.3;
    public static final int DryRetry = 40;
    public static final int ChargeGrace = 20;
    public static final float DryVolume = 0.8F;
    private static final Map<UUID, Gunner> Gunners = new HashMap<>();

    private NpcGunner() {
    }

    @Nullable
    public static GunKind kindOf(ItemStack Stack) {
        GunKind Kind = GunItem.kindOf(Stack);
        return Kind == null || Kind == GunKind.Ultralaser || Kind == GunKind.MicroHid || Kind == GunKind.Flamethrower || Kind == GunKind.ScarletRifle ? null : Kind;
    }

    public static boolean isGun(ItemStack Stack) {
        return kindOf(Stack) != null;
    }

    public static double range(ItemStack Stack) {
        GunKind Kind = kindOf(Stack);
        return Kind == null ? 0.0 : Kind.getStats().getRange();
    }

    public static boolean ready(LivingEntity Shooter, ItemStack Stack) {
        GunKind Kind = kindOf(Stack);
        if (Kind == null || !Shooter.isAlive()) {
            return false;
        }
        Gunner State = Gunners.get(Shooter.getUUID());
        long Now = Shooter.level().getGameTime();
        return (State == null || State.ready(Kind, Now)) && !GunHeat.venting(Stack, Now);
    }

    public static boolean reloading(LivingEntity Shooter, ItemStack Stack) {
        GunKind Kind = kindOf(Stack);
        Gunner State = Gunners.get(Shooter.getUUID());
        return Kind != null && State != null && State.ReloadKind == Kind && Shooter.level().getGameTime() < State.ReloadEnd;
    }

    public static boolean dry(LivingEntity Shooter, ItemStack Stack) {
        GunKind Kind = kindOf(Stack);
        if (Kind == null) {
            return true;
        }
        if (Kind.getStats().usesCharges()) {
            return GunItem.getCharges(Stack) <= 0;
        }
        Gunner State = Gunners.get(Shooter.getUUID());
        if (State == null) {
            return false;
        }
        return State.DryKind == Kind && State.rounds(Kind, Stack) <= 0;
    }

    public static int rounds(LivingEntity Shooter, ItemStack Stack) {
        GunKind Kind = kindOf(Stack);
        if (Kind == null) {
            return 0;
        }
        if (Kind.getStats().usesCharges()) {
            return GunItem.getCharges(Stack);
        }
        return Gunners.computeIfAbsent(Shooter.getUUID(), Id -> new Gunner()).rounds(Kind, Stack);
    }

    public static void forget(UUID Id) {
        Gunners.remove(Id);
    }

    public static void forgetAll() {
        Gunners.clear();
    }

    public static boolean fireAt(LivingEntity Shooter, ItemStack Stack, LivingEntity Target, @Nullable NpcAmmoSource Ammo, float Accuracy) {
        GunKind Kind = kindOf(Stack);
        if (Kind == null || !(Shooter.level() instanceof ServerLevel World) || !Shooter.isAlive() || !Target.isAlive() || Target == Shooter || Target.level() != World) {
            return false;
        }
        long Now = World.getGameTime();
        GunStats Stats = Kind.getStats();
        Gunner State = Gunners.computeIfAbsent(Shooter.getUUID(), Id -> new Gunner());
        State.settle(Shooter, Kind, Stack, Now);
        if (GunHeat.cooled(Stack, Now)) {
            GunSounds.playNear(Shooter, GunSounds.CueCooled, Kind, 1.0F);
        }
        if (State.ReloadKind == Kind || Now < State.NextShot[Kind.ordinal()] || GunHeat.venting(Stack, Now)) {
            return false;
        }
        float Steadiness = Mth.clamp(Accuracy, 0.0F, 1.0F);
        if (Stats.usesCharges()) {
            return discharge(World, Shooter, Stack, Kind, Target, Steadiness, State, Now);
        }
        int Rounds = State.rounds(Kind, Stack);
        if (Rounds <= 0) {
            if (Kind.usesAmmo() && Now >= State.DryUntil) {
                State.reload(Shooter, Kind, Stack, Ammo, Now);
            }
            return false;
        }
        shoot(World, Shooter, Stack, Kind, Target, Steadiness, State, Now);
        State.store(Kind, Stack, Rounds - 1);
        State.NextShot[Kind.ordinal()] = Now + Stats.getInterval();
        if (GunHeat.addShot(Stack, Stats, Now)) {
            GunSounds.playNear(Shooter, GunSounds.CueOverheat, Kind, 1.0F);
        }
        if (Rounds - 1 <= 0 && Kind.usesAmmo()) {
            State.reload(Shooter, Kind, Stack, Ammo, Now);
        }
        return true;
    }

    public static boolean clearShot(LivingEntity Shooter, LivingEntity Target, Predicate<Entity> Friendly) {
        if (!(Shooter.level() instanceof ServerLevel World) || Target.level() != World || !Target.isAlive()) {
            return false;
        }
        Vec3 Eye = Shooter.getEyePosition();
        Vec3 Lead = lead(Target);
        boolean Open = false;
        for (Vec3 Point : List.of(centre(Target).add(Lead), head(Target).add(Lead))) {
            if (!sees(World, Shooter, Eye, Point)) {
                continue;
            }
            Open = true;
            if (!unobstructed(World, Shooter, Target, Eye, Point, Friendly)) {
                return false;
            }
        }
        return Open;
    }

    private static boolean discharge(ServerLevel World, LivingEntity Shooter, ItemStack Stack, GunKind Kind, LivingEntity Target, float Steadiness, Gunner State, long Now) {
        GunStats Stats = Kind.getStats();
        int Left = GunItem.getCharges(Stack);
        if (Left <= 0) {
            return false;
        }
        if (State.ChargeKind != Kind || Now > State.ChargeEnd + ChargeGrace) {
            State.ChargeKind = Kind;
            State.ChargeEnd = Now + Stats.getChargeTicks();
            GunSounds.playNear(Shooter, GunSounds.CueCharge, Kind, 1.0F);
            return false;
        }
        if (Now < State.ChargeEnd) {
            return false;
        }
        State.ChargeKind = null;
        shoot(World, Shooter, Stack, Kind, Target, Steadiness, State, Now);
        State.NextShot[Kind.ordinal()] = Now + Stats.getInterval();
        GunItem.setCharges(Stack, Left - 1);
        if (Left - 1 <= 0) {
            GunFiring.burnout(World, Shooter, Stack);
        }
        return true;
    }

    private static void shoot(ServerLevel World, LivingEntity Shooter, ItemStack Stack, GunKind Kind, LivingEntity Target, float Steadiness, Gunner State, long Now) {
        GunStats Stats = Kind.getStats();
        Vec3 Eye = Shooter.getEyePosition();
        Vec3 Heading = aimPoint(World, Shooter, Target, Stats, Steadiness).subtract(Eye);
        Heading = Heading.lengthSqr() < 1.0E-6 ? Shooter.getLookAngle() : Heading.normalize();
        face(Shooter, Heading);
        RandomSource Random = Shooter.getRandom();
        Vec3 Centre = GunFiring.scatter(Heading, spread(Shooter, Stats, State.Cadence, Now, Steadiness), Random);
        GunFiring.volley(World, Shooter, Kind, State.Cadence, Now, Eye, Centre, GunItem.damageOf(Stack, Kind), Random);
    }

    private static float spread(LivingEntity Shooter, GunStats Stats, GunFiring.Shooter Cadence, long Now, float Steadiness) {
        float Base = Stats.getAimSpread() + (1.0F - Steadiness) * (Math.max(0.0F, Stats.getHipSpread() - Stats.getAimSpread()) + MissSlop);
        Vec3 Movement = Shooter.getDeltaMovement();
        float Moving = (float) Mth.clamp(Math.sqrt(Movement.x * Movement.x + Movement.z * Movement.z) / GunFiring.WalkSpeed, 0.0, 1.0) * Stats.getMovePenalty() * MovingShare;
        float Airborne = Shooter.onGround() || Shooter.isPassenger() ? 0.0F : Stats.getAirPenalty();
        return Base + Moving + Airborne + Cadence.bloom(Stats, Now);
    }

    private static Vec3 aimPoint(ServerLevel World, LivingEntity Shooter, LivingEntity Target, GunStats Stats, float Steadiness) {
        Vec3 Lead = lead(Target);
        Vec3 Eye = Shooter.getEyePosition();
        boolean Head = Stats.getHeadshot() >= HeadPreference && Steadiness >= HeadAccuracy && Target.getBbHeight() >= GunFiring.HeadshotHeight;
        Vec3 First = (Head ? head(Target) : centre(Target)).add(Lead);
        Vec3 Second = (Head ? centre(Target) : head(Target)).add(Lead);
        if (sees(World, Shooter, Eye, First) || !sees(World, Shooter, Eye, Second)) {
            return First;
        }
        return Second;
    }

    private static Vec3 centre(LivingEntity Target) {
        return Target.getBoundingBox().getCenter();
    }

    private static Vec3 head(LivingEntity Target) {
        AABB Box = Target.getBoundingBox();
        return new Vec3((Box.minX + Box.maxX) * 0.5, Math.min(Box.maxY - 0.05, Target.getEyeY()), (Box.minZ + Box.maxZ) * 0.5);
    }

    private static Vec3 lead(LivingEntity Target) {
        Vec3 Motion = Target.getKnownMovement();
        if (Target.onGround()) {
            Motion = new Vec3(Motion.x, 0.0, Motion.z);
        }
        return Motion.scale(LeadTicks);
    }

    private static boolean sees(ServerLevel World, LivingEntity Shooter, Vec3 Eye, Vec3 Point) {
        GunGlass.Route Path = GunGlass.trace(World, Shooter, Eye, Point);
        return !Path.lodged() && !Path.solid();
    }

    private static boolean unobstructed(ServerLevel World, LivingEntity Shooter, LivingEntity Target, Vec3 Eye, Vec3 Point, Predicate<Entity> Friendly) {
        double Reach = Eye.distanceTo(Point);
        Entity Ride = Shooter.getRootVehicle();
        AABB Area = new AABB(Eye, Point).inflate(1.0 + FriendlyMargin);
        for (Entity Other : World.getEntities(Shooter, Area, Candidate -> Candidate != Target && Candidate.isAlive() && !Candidate.isSpectator()
                && Candidate.isPickable() && Candidate.getRootVehicle() != Ride && Friendly.test(Candidate))) {
            AABB Box = Other.getBoundingBox().inflate(Other.getPickRadius() + FriendlyMargin);
            if (Box.contains(Eye)) {
                return false;
            }
            Optional<Vec3> Clip = Box.clip(Eye, Point);
            if (Clip.isPresent() && Eye.distanceTo(Clip.get()) < Reach) {
                return false;
            }
        }
        return true;
    }

    private static void face(LivingEntity Shooter, Vec3 Heading) {
        if (Shooter instanceof Player) {
            return;
        }
        float Yaw = (float) (Mth.atan2(Heading.z, Heading.x) * Mth.RAD_TO_DEG) - 90.0F;
        float Pitch = (float) -(Mth.atan2(Heading.y, Math.sqrt(Heading.x * Heading.x + Heading.z * Heading.z)) * Mth.RAD_TO_DEG);
        Shooter.setYHeadRot(Yaw);
        Shooter.setXRot(Mth.clamp(Pitch, -90.0F, 90.0F));
        Shooter.setYBodyRot(Yaw);
        if (Shooter instanceof Mob Body) {
            Body.getLookControl().setLookAt(Shooter.getEyePosition().add(Heading.scale(8.0)));
        }
    }

    private static final class Gunner {
        private final GunFiring.Shooter Cadence = new GunFiring.Shooter();
        private final long[] NextShot = new long[GunKind.values().length];
        private final int[] Rounds = new int[GunKind.values().length];
        private final boolean[] Known = new boolean[GunKind.values().length];
        private final ItemStack[] Held = new ItemStack[GunKind.values().length];
        @Nullable
        private GunKind ReloadKind;
        private long ReloadEnd;
        private int Pending;
        @Nullable
        private GunKind DryKind;
        private long DryUntil = Long.MIN_VALUE;
        @Nullable
        private GunKind ChargeKind;
        private long ChargeEnd;

        private boolean ready(GunKind Kind, long Now) {
            if (ReloadKind == Kind && Now < ReloadEnd) {
                return false;
            }
            if (ChargeKind == Kind && Now < ChargeEnd) {
                return false;
            }
            if (DryKind == Kind && Now < DryUntil) {
                return false;
            }
            return Now >= NextShot[Kind.ordinal()];
        }

        private int capacity(GunKind Kind) {
            return Kind == GunKind.Scp127 ? Scp127Levels.capacity() : Kind.getStats().getCapacity();
        }

        private int rounds(GunKind Kind, ItemStack Stack) {
            if (!Kind.usesAmmo()) {
                return GunItem.getAmmo(Stack);
            }
            int Index = Kind.ordinal();
            if (Held[Index] != Stack) {
                if (!Known[Index] || Stack.getComponentsPatch().get(GunsRegistry.LoadedAmmo.get()) != null) {
                    Rounds[Index] = Mth.clamp(GunItem.getAmmo(Stack), 0, capacity(Kind));
                }
                Known[Index] = true;
                Held[Index] = Stack;
            }
            return Rounds[Index];
        }

        private void store(GunKind Kind, ItemStack Stack, int Value) {
            if (Kind.usesAmmo()) {
                Rounds[Kind.ordinal()] = Value;
                Known[Kind.ordinal()] = true;
                Held[Kind.ordinal()] = Stack;
            }
            GunItem.setAmmo(Stack, Value);
        }

        private void settle(LivingEntity Shooter, GunKind Kind, ItemStack Stack, long Now) {
            if (Scp127Item.is(Stack)) {
                Scp127Levels.regrow(Stack, Now);
            }
            if (ChargeKind != null && ChargeKind != Kind) {
                ChargeKind = null;
            }
            if (ReloadKind == null || Now < ReloadEnd) {
                return;
            }
            GunKind Finished = ReloadKind;
            ReloadKind = null;
            int Index = Finished.ordinal();
            int Filled = Math.min(capacity(Finished), Rounds[Index] + Pending);
            Pending = 0;
            if (Finished == Kind) {
                store(Kind, Stack, Filled);
            } else {
                Rounds[Index] = Filled;
            }
            GunSounds.playNear(Shooter, GunSounds.CueReloadFinish, Finished, GunReloading.Volume);
        }

        private void reload(LivingEntity Shooter, GunKind Kind, ItemStack Stack, @Nullable NpcAmmoSource Ammo, long Now) {
            int Missing = capacity(Kind) - rounds(Kind, Stack);
            if (Missing <= 0 || ReloadKind != null) {
                return;
            }
            int Taken = Ammo == null ? Missing : Math.max(0, Math.min(Missing, Ammo.take(Kind.getAmmo(), Missing)));
            if (Taken <= 0) {
                if (DryKind != Kind) {
                    GunSounds.play(Shooter.level(), null, Shooter.getX(), Shooter.getEyeY(), Shooter.getZ(), GunSounds.dryFire(), DryVolume);
                }
                DryKind = Kind;
                DryUntil = Now + DryRetry;
                return;
            }
            DryKind = null;
            DryUntil = Long.MIN_VALUE;
            ReloadKind = Kind;
            ReloadEnd = Now + Kind.getStats().getReloadTicks();
            Pending = Taken;
            GunSounds.playNear(Shooter, GunSounds.CueReloadStart, Kind, GunReloading.Volume);
        }
    }
}
