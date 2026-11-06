package net.rai.scp_unleashed.ultralaser;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.guns.GunGlass;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.network.GunImpact;
import net.rai.scp_unleashed.ultralaser.network.UltralaserBlastPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class UltralaserBlast {
    public static final double BeamRadius = 0.75;
    public static final int MaxTargets = 60;
    public static final float KnockbackScale = 0.6F;
    public static final double KnockbackLift = 0.45;
    public static final double RecoilPush = 0.9;
    public static final double RecoilLift = 0.12;
    public static final double ShooterRange = 160.0;
    public static final double EndRange = 96.0;

    private UltralaserBlast() {
    }

    private record Hit(Entity Target, Vec3 Point, double Distance) {
    }

    public static boolean fire(ServerPlayer User, ItemStack Stack) {
        ServerLevel World = User.serverLevel();
        Stack.remove(GunsRegistry.Charging.get());
        int Loaded = GunItem.getAmmo(Stack);
        if (Loaded <= 0) {
            return false;
        }
        if (!User.getAbilities().instabuild) {
            GunItem.setAmmo(Stack, Loaded - 1);
        }
        User.getCooldowns().addCooldown(Stack.getItem(), UltralaserPriming.CooldownTicks);
        GunStats Stats = GunKind.Ultralaser.getStats();
        Vec3 Eye = User.getEyePosition();
        Vec3 Look = User.getLookAngle();
        Vec3 Far = Eye.add(Look.scale(Stats.getRange()));
        GunGlass.Route Path = GunGlass.trace(World, User, Eye, Far).settle(World, User, Double.MAX_VALUE, new HashMap<>());
        BlockHitResult Wall = Path.surface();
        boolean Solid = Path.solid();
        Vec3 End = Path.limit();
        List<GunImpact> Impacts = new ArrayList<>();
        DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(UltralaserRegistry.UltralaserDamage), null, User, Eye);
        Direction Facing = Direction.getNearest(-Look.x, -Look.y, -Look.z);
        for (Hit Struck : targets(World, User, Eye, End)) {
            Entity Target = Struck.Target();
            Target.invulnerableTime = 0;
            boolean Landed = Target.hurt(Source, Stats.getDamage() * Path.carryBefore(Struck.Distance()));
            if (!(Target instanceof PartEntity<?>)) {
                push(Target, Look, Stats.getKnockback());
            }
            Entity Body = Target instanceof PartEntity<?> Part ? Part.getParent() : Target;
            boolean Killed = !Body.isAlive() || Body instanceof LivingEntity Living && Living.isDeadOrDying();
            Impacts.add(GunImpact.at(Eye, Struck.Point(), Landed && Killed ? GunImpact.Kill : GunImpact.Wound, Facing));
            if (Impacts.size() >= MaxTargets) {
                break;
            }
        }
        Impacts.add(GunImpact.at(Eye, End, Solid ? GunImpact.Surface : GunImpact.Miss, Solid ? Wall.getDirection() : Direction.UP));
        broadcast(World, User, new UltralaserBlastPayload(User.getId(), Eye, End, Impacts));
        UltralaserSounds.play(World, Eye, UltralaserSounds.Blast);
        if (Solid) {
            UltralaserSounds.play(World, End, UltralaserSounds.Impact);
            World.gameEvent(GameEvent.PROJECTILE_LAND, End, GameEvent.Context.of(User));
        }
        User.gameEvent(GameEvent.PROJECTILE_SHOOT);
        GunGlass.shatter(World, User, Path.positions());
        if (Solid) {
            GunGlass.strike(World, User, Wall, Stats.getDamage() * Path.carry());
        }
        recoil(User, Look);
        return true;
    }

    private static List<Hit> targets(ServerLevel World, ServerPlayer User, Vec3 Start, Vec3 End) {
        AABB Area = new AABB(Start, End).inflate(BeamRadius + 1.0);
        Entity Ride = User.getRootVehicle();
        List<Hit> Hits = new ArrayList<>();
        Set<Entity> Bodies = new HashSet<>();
        for (Entity Candidate : World.getEntities(User, Area, Candidate -> struckBy(User, Candidate, Ride))) {
            AABB Box = Candidate.getBoundingBox().inflate(BeamRadius);
            Optional<Vec3> Clip = Box.contains(Start) ? Optional.of(Start) : Box.clip(Start, End);
            if (Clip.isEmpty()) {
                continue;
            }
            Entity Body = Candidate instanceof PartEntity<?> Part ? Part.getParent() : Candidate;
            if (!Bodies.add(Body)) {
                continue;
            }
            Vec3 Point = Clip.get();
            Hits.add(new Hit(Candidate, Point, Start.distanceTo(Point)));
        }
        Hits.sort(Comparator.comparingDouble(Hit::Distance));
        return Hits;
    }

    private static boolean struckBy(ServerPlayer User, Entity Candidate, Entity Ride) {
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

    private static void push(Entity Target, Vec3 Look, float Knockback) {
        double Resist = Target instanceof LivingEntity Living ? Living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
        double Strength = Knockback * KnockbackScale * Math.max(0.0, 1.0 - Resist);
        if (Strength <= 0.0) {
            return;
        }
        Vec3 Flat = new Vec3(Look.x, 0.0, Look.z);
        Flat = Flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : Flat.normalize();
        Target.push(Flat.x * Strength, KnockbackLift * Math.max(0.0, 1.0 - Resist), Flat.z * Strength);
        Target.hurtMarked = true;
    }

    private static void recoil(ServerPlayer User, Vec3 Look) {
        Vec3 Flat = new Vec3(Look.x, 0.0, Look.z);
        if (Flat.lengthSqr() < 1.0E-6) {
            return;
        }
        double Strength = RecoilPush * (User.isCrouching() ? 0.5 : 1.0);
        Flat = Flat.normalize().scale(-Strength);
        User.push(Flat.x, User.onGround() ? RecoilLift : 0.0, Flat.z);
        User.hurtMarked = true;
    }

    private static void broadcast(ServerLevel World, ServerPlayer User, UltralaserBlastPayload Payload) {
        Vec3 End = Payload.End();
        for (ServerPlayer Viewer : World.players()) {
            if (Viewer == User || Viewer.distanceToSqr(User) <= ShooterRange * ShooterRange || Viewer.distanceToSqr(End) <= EndRange * EndRange) {
                PacketDistributor.sendToPlayer(Viewer, Payload);
            }
        }
    }
}
