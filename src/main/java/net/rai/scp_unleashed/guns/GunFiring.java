package net.rai.scp_unleashed.guns;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.guns.network.GunAckPayload;
import net.rai.scp_unleashed.guns.network.GunImpact;
import net.rai.scp_unleashed.guns.network.GunShotPayload;
import net.rai.scp_unleashed.marksman.CoinRicochet;
import net.rai.scp_unleashed.space.SpaceWeapons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GunFiring {
    public static final double PickPadding = 0.05;
    public static final double HeadReach = 0.3;
    public static final float HeadshotHeight = 1.2F;
    public static final int MaxImpactSounds = 2;
    public static final int MaxPierced = 40;
    public static final double MaxSweep = 128.0;
    public static final double WalkSpeed = 0.2;
    public static final float CrouchSteadiness = 0.75F;
    private static final Map<UUID, Shooter> Shooters = new HashMap<>();

    private GunFiring() {
    }

    public static void requestFire(ServerPlayer User, int KindIndex, int Sequence) {
        attemptFire(User, KindIndex);
        ItemStack Held = User.getMainHandItem();
        GunKind Kind = GunItem.kindOf(Held);
        int Ammo = Kind == null || Kind.getStats().usesCharges() ? -1 : GunItem.getAmmo(Held);
        PacketDistributor.sendToPlayer(User, new GunAckPayload(Sequence, Ammo));
    }

    private static void attemptFire(ServerPlayer User, int KindIndex) {
        ItemStack Stack = User.getMainHandItem();
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind == null || Kind == GunKind.Ultralaser || Kind.ordinal() != KindIndex || User.isSpectator() || !User.isAlive() || GunItem.isBusy(Stack)) {
            return;
        }
        if (SpaceWeapons.jammed(User, Stack)) {
            return;
        }
        ServerLevel World = User.serverLevel();
        long Now = World.getGameTime();
        Shooter State = Shooters.computeIfAbsent(User.getUUID(), Id -> new Shooter());
        GunStats Stats = Kind.getStats();
        if (Stats.usesCharges()) {
            if (GunItem.getCharges(Stack) <= 0 || !State.admit(Kind, Now)) {
                return;
            }
            Stack.set(GunsRegistry.Charging.get(), new GunTimer(Now, Now + Stats.getChargeTicks()));
            GunSounds.playAround(User, GunSounds.CueCharge, Kind, 1.0F);
            return;
        }
        int Loaded = GunItem.getAmmo(Stack);
        if (Loaded <= 0 || GunHeat.venting(Stack, Now) || !State.admit(Kind, Now)) {
            return;
        }
        if (Stack.getItem() instanceof GunItem Custom && Custom.shoot(World, User, Stack, Now)) {
            return;
        }
        if (!User.getAbilities().instabuild) {
            GunItem.setAmmo(Stack, Loaded - 1);
        }
        fire(World, User, Kind, State, Now);
        if (GunHeat.addShot(Stack, Stats, Now)) {
            GunSounds.playAround(User, GunSounds.CueOverheat, Kind, 1.0F);
        }
        if (Stack.getItem() instanceof GunItem Gun) {
            Gun.onFired(User, Stack);
        }
    }

    public static void discharge(ServerPlayer User, ItemStack Stack, GunKind Kind) {
        Stack.remove(GunsRegistry.Charging.get());
        int Left = GunItem.getCharges(Stack);
        if (Left <= 0) {
            return;
        }
        ServerLevel World = User.serverLevel();
        Shooter State = Shooters.computeIfAbsent(User.getUUID(), Id -> new Shooter());
        fire(World, User, Kind, State, World.getGameTime());
        if (User.getAbilities().instabuild) {
            return;
        }
        GunItem.setCharges(Stack, Left - 1);
        if (Left - 1 <= 0) {
            burnout(World, User, Stack);
        }
    }

    public static void abortCharge(ServerPlayer User, ItemStack Stack) {
        Stack.remove(GunsRegistry.Charging.get());
        GunKind Kind = GunItem.kindOf(Stack);
        GunSounds.playAround(User, GunSounds.CueAbort, Kind == null ? GunKind.ParticleDisruptor : Kind, 1.0F);
    }

    public static void forget(UUID Id) {
        Shooters.remove(Id);
    }

    public static void forgetAll() {
        Shooters.clear();
    }

    public static void burnout(ServerLevel World, LivingEntity User, ItemStack Stack) {
        Vec3 Eye = User.getEyePosition();
        Vec3 Front = Eye.add(User.getLookAngle().scale(0.6)).add(0.0, -0.25, 0.0);
        GunSounds.playNear(User, GunSounds.CueBurnout, GunKind.ParticleDisruptor, 1.0F);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Front.x, Front.y, Front.z, 12, 0.2, 0.15, 0.2, 0.02);
        World.sendParticles(GunsRegistry.EnergySpark.get(), Front.x, Front.y, Front.z, 24, 0.25, 0.2, 0.25, 0.15);
        User.onEquippedItemBroken(Stack.getItem(), EquipmentSlot.MAINHAND);
        Stack.shrink(1);
    }

    private static void fire(ServerLevel World, ServerPlayer User, GunKind Kind, Shooter State, long Now) {
        GunStats Stats = Kind.getStats();
        Vec3 Eye = User.getEyePosition();
        Vec3 Look = User.getLookAngle();
        boolean Aiming = User.isUsingItem() && User.getUseItem().getItem() instanceof GunItem;
        RandomSource Random = User.getRandom();
        Vec3 Centre = scatter(Look, spread(User, Stats, State, Now, Aiming), Random);
        volley(World, User, Kind, State, Now, Eye, Centre, GunItem.damageOf(User.getMainHandItem(), Kind), Random);
    }

    public static void volley(ServerLevel World, LivingEntity User, GunKind Kind, Shooter State, long Now, Vec3 Eye, Vec3 Centre, float BaseDamage, RandomSource Random) {
        GunStats Stats = Kind.getStats();
        List<GunImpact> Impacts = new ArrayList<>();
        Map<Entity, Float> Wounds = new LinkedHashMap<>();
        Map<Entity, List<Integer>> Marks = new HashMap<>();
        List<BlockHitResult> Surfaces = new ArrayList<>();
        Set<BlockPos> Shattered = new LinkedHashSet<>();
        Map<BlockPos, Boolean> Verdicts = new HashMap<>();
        Map<BlockPos, Float> Blows = new LinkedHashMap<>();
        Map<BlockPos, BlockHitResult> BlowSurfaces = new HashMap<>();
        for (int Pellet = 0; Pellet < Stats.getPellets(); Pellet++) {
            Vec3 Heading = Stats.getPellets() > 1 ? scatter(Centre, Stats.getPelletSpread(), Random) : Centre;
            Vec3 End = Eye.add(Heading.scale(Stats.getRange()));
            GunGlass.Route Path = GunGlass.trace(World, User, Eye, End);
            Vec3 Limit = Path.limit();
            List<Hit> Hits = entitiesAlong(World, User, Eye, Limit);
            Vec3 Deflected = User instanceof ServerPlayer Gunner ? CoinRicochet.deflect(World, Gunner, Kind, Eye, Heading, Hits.isEmpty() ? Limit : Hits.get(0).Point()) : null;
            if (Deflected != null) {
                Impacts.add(GunImpact.at(Eye, Deflected, GunImpact.Miss, Direction.UP));
                continue;
            }
            Path = Path.settle(World, User, !Stats.isPiercing() && !Hits.isEmpty() ? Hits.get(0).Distance() : Double.MAX_VALUE, Verdicts);
            Limit = Path.limit();
            double Reach = Eye.distanceTo(Limit) + PickPadding;
            Hits.removeIf(Candidate -> Candidate.Distance() > Reach);
            Shattered.addAll(Path.positions());
            BlockHitResult Surface = Path.surface();
            boolean Solid = Path.solid();
            int Reached = Stats.isPiercing() ? Math.min(MaxPierced, Hits.size()) : Math.min(1, Hits.size());
            for (int Index = 0; Index < Reached; Index++) {
                Hit Struck = Hits.get(Index);
                float Amount = BaseDamage * Stats.falloff(Struck.Distance()) * Path.carryBefore(Struck.Distance()) * (isHeadshot(Struck) ? Stats.getHeadshot() : 1.0F);
                Wounds.merge(Struck.Target(), Amount, Float::sum);
                Marks.computeIfAbsent(Struck.Target(), Target -> new ArrayList<>()).add(Impacts.size());
                Impacts.add(GunImpact.at(Eye, Struck.Point(), GunImpact.Wound, Direction.getNearest(-Heading.x, -Heading.y, -Heading.z)));
            }
            if (Reached == 0 || Stats.isPiercing()) {
                if (Solid) {
                    Impacts.add(GunImpact.at(Eye, Limit, GunImpact.Surface, Surface.getDirection()));
                    Surfaces.add(Surface);
                    BlockPos Pos = Surface.getBlockPos().immutable();
                    Blows.merge(Pos, BaseDamage * Stats.falloff(Eye.distanceTo(Limit)) * Path.carry(), Float::sum);
                    BlowSurfaces.putIfAbsent(Pos, Surface);
                } else {
                    Impacts.add(GunImpact.at(Eye, Limit, GunImpact.Miss, Direction.UP));
                }
            }
        }
        for (Map.Entry<Entity, Float> Entry : Wounds.entrySet()) {
            Entity Target = Entry.getKey();
            boolean Landed = GunDamage.hurt(World, User, Target, Kind, Entry.getValue(), Centre);
            boolean Killed = !Target.isAlive() || Target instanceof LivingEntity Living && Living.isDeadOrDying();
            if (Landed && Killed) {
                for (int Index : Marks.get(Target)) {
                    Impacts.set(Index, Impacts.get(Index).withType(GunImpact.Kill));
                }
            }
        }
        State.addBloom(Stats, Now);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new GunShotPayload(User.getId(), Kind.ordinal(), Eye, Impacts));
        if (Stats.usesCharges()) {
            GunSounds.playNear(User, GunSounds.CueShot, Kind, 1.0F);
        } else {
            int Streak = State.streak(Stats, Now);
            GunSounds.play(World, User instanceof ServerPlayer Gunner ? Gunner : null, Eye.x, Eye.y, Eye.z, GunSounds.burst(Kind, Streak), 1.0F, GunSounds.jitter(Streak));
        }
        User.gameEvent(GameEvent.PROJECTILE_SHOOT);
        GunGlass.shatter(World, User, Shattered);
        Blows.forEach((Pos, Amount) -> GunGlass.strike(World, User, BlowSurfaces.get(Pos), Amount));
        for (int Index = 0; Index < Surfaces.size(); Index++) {
            BlockHitResult Surface = Surfaces.get(Index);
            BlockPos Pos = Surface.getBlockPos();
            World.gameEvent(GameEvent.PROJECTILE_LAND, Surface.getLocation(), GameEvent.Context.of(User));
            if (Index < MaxImpactSounds) {
                BlockState Struck = World.getBlockState(Pos);
                SoundType Sounds = Struck.getSoundType(World, Pos, User);
                GunSoundMixer.emitAt(World, null, Pos, Sounds.getHitSound(), SoundSource.BLOCKS, Sounds.getVolume() * 0.7F, Sounds.getPitch() * 1.25F);
            }
        }
    }

    private static boolean isHeadshot(Hit Struck) {
        Entity Target = Struck.Target();
        return Target instanceof LivingEntity && Target.getBbHeight() >= HeadshotHeight && Struck.Point().y >= Target.getEyeY() - HeadReach;
    }

    private static float spread(ServerPlayer User, GunStats Stats, Shooter State, long Now, boolean Aiming) {
        float Base = Aiming ? Stats.getAimSpread() : Stats.getHipSpread();
        float Bloom = State.bloom(Stats, Now);
        if (User.isCrouching()) {
            Base *= Stats.getCrouchSpread();
            Bloom *= Stats.getCrouchBloom();
        }
        Vec3 Movement = User.getKnownMovement();
        float Moving = (float) Mth.clamp(Math.sqrt(Movement.x * Movement.x + Movement.z * Movement.z) / WalkSpeed, 0.0, 1.0) * Stats.getMovePenalty() * (Aiming ? 0.5F : 1.0F);
        float Airborne = User.onGround() || User.isPassenger() ? 0.0F : Stats.getAirPenalty();
        return Base + Moving + Airborne + Bloom;
    }

    public static Vec3 scatter(Vec3 Heading, float Degrees, RandomSource Random) {
        if (Degrees <= 0.0F) {
            return Heading;
        }
        double Angle = Math.toRadians(Degrees) * Math.sqrt(Random.nextDouble());
        double Turn = Random.nextDouble() * Math.PI * 2.0;
        Vec3 Helper = Math.abs(Heading.y) < 0.99 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 Right = Heading.cross(Helper).normalize();
        Vec3 Up = Right.cross(Heading).normalize();
        double Offset = Math.tan(Angle);
        return Heading.add(Right.scale(Math.cos(Turn) * Offset)).add(Up.scale(Math.sin(Turn) * Offset)).normalize();
    }

    private static List<Hit> entitiesAlong(ServerLevel World, Entity User, Vec3 Start, Vec3 Requested) {
        if (!Double.isFinite(Requested.x) || !Double.isFinite(Requested.y) || !Double.isFinite(Requested.z)) {
            return new ArrayList<>();
        }
        Vec3 End = Start.distanceToSqr(Requested) > MaxSweep * MaxSweep ? Start.add(Requested.subtract(Start).normalize().scale(MaxSweep)) : Requested;
        AABB Area = new AABB(Start, End).inflate(1.0);
        Entity Ride = User.getRootVehicle();
        List<Hit> Hits = new ArrayList<>();
        for (Entity Target : World.getEntities(User, Area, Candidate -> Candidate.isPickable() && !Candidate.isSpectator() && Candidate.isAlive()
                && Candidate.getRootVehicle() != Ride)) {
            AABB Box = Target.getBoundingBox().inflate(Target.getPickRadius() + PickPadding);
            if (Box.contains(Start)) {
                Hits.add(new Hit(Target, Start, 0.0));
                continue;
            }
            Optional<Vec3> Clip = Box.clip(Start, End);
            Clip.ifPresent(Point -> Hits.add(new Hit(Target, Point, Start.distanceTo(Point))));
        }
        Hits.sort(Comparator.comparingDouble(Hit::Distance));
        return Hits;
    }

    private record Hit(Entity Target, Vec3 Point, double Distance) {
    }

    public static final class Shooter {
        private final double[] Ready = new double[GunKind.values().length];
        private float Bloom;
        private long BloomTick;
        private long LastShot = Long.MIN_VALUE / 2;
        private int Streak;

        private int streak(GunStats Stats, long Now) {
            Streak = Stats.isAutomatic() && Now - LastShot <= Stats.getInterval() * 2L ? Streak + 1 : 0;
            LastShot = Now;
            return Streak;
        }

        private boolean admit(GunKind Kind, long Now) {
            GunStats Stats = Kind.getStats();
            double Tolerance = Stats.isAutomatic() ? Stats.getInterval() : 1.0;
            double Due = Math.max(Ready[Kind.ordinal()], Now);
            if (Due - Now > Tolerance) {
                return false;
            }
            Ready[Kind.ordinal()] = Due + Stats.getInterval();
            return true;
        }

        public float bloom(GunStats Stats, long Now) {
            Bloom = Math.max(0.0F, Bloom - Stats.getBloomRecovery() * Math.max(0L, Now - BloomTick));
            BloomTick = Now;
            return Bloom;
        }

        private void addBloom(GunStats Stats, long Now) {
            bloom(Stats, Now);
            Bloom = Math.min(Stats.getBloomLimit(), Bloom + Stats.getBloomPerShot());
        }
    }
}
