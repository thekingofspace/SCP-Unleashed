package net.rai.scp_unleashed.microhid;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.GunsRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class MicroHidBeam {
    public static final String MessagePrefix = "message.scp_unleashed.micro_hid.";
    public static final double BeamRadius = 0.6;
    public static final double SplashRadius = 1.25;
    public static final double SplashPull = 0.3;
    public static final int MaxTargets = 24;
    public static final int SlowTicks = 30;
    public static final int SlowLevel = 1;
    public static final double KnockbackLift = 0.04;
    public static final double MuzzleReach = 0.9;
    public static final double MuzzleDrop = 0.3;
    public static final float SpinSpeed = 0.5F;
    public static final float FireSpeed = 0.3F;

    private MicroHidBeam() {
    }

    public record Struck(Entity Target, Vec3 Point, double Distance) {
    }

    public static boolean firing(int Used) {
        return Used >= MicroHidEnergy.SpinUpTicks;
    }

    public static float moveFactor(int Used) {
        return firing(Used) ? FireSpeed : SpinSpeed;
    }

    public static boolean pulsing(int Used) {
        return firing(Used) && (Used - MicroHidEnergy.SpinUpTicks) % GunKind.MicroHid.getStats().getInterval() == 0;
    }

    public static void tick(ServerLevel World, LivingEntity User, ItemStack Stack, int Used) {
        if (!User.isAlive() || User.isSpectator()) {
            User.stopUsingItem();
            return;
        }
        if (!MicroHidEnergy.canFire(Stack)) {
            exhaust(World, User, Stack);
            return;
        }
        boolean Firing = firing(Used);
        if (User.isSprinting()) {
            User.setSprinting(false);
        }
        if (pulsing(Used)) {
            pulse(World, User);
        }
        boolean Free = User instanceof Player Person && Person.getAbilities().instabuild;
        if (!Free && Used % MicroHidEnergy.DrainEvery == MicroHidEnergy.DrainEvery - 1) {
            int Rate = Firing ? MicroHidEnergy.FireDrain : MicroHidEnergy.SpinDrain;
            if (MicroHidEnergy.drain(Stack, Rate * MicroHidEnergy.DrainEvery) <= 0) {
                exhaust(World, User, Stack);
            }
        }
    }

    public static void stop(LivingEntity User, ItemStack Stack, int Used) {
        if (Used > 0 && User instanceof Player Person && !Person.getCooldowns().isOnCooldown(Stack.getItem())) {
            Person.getCooldowns().addCooldown(Stack.getItem(), MicroHidEnergy.WindDownTicks);
        }
    }

    public static void dryFire(Player User, ItemStack Stack) {
        if (!(User.level() instanceof ServerLevel World)) {
            return;
        }
        User.getCooldowns().addCooldown(Stack.getItem(), MicroHidEnergy.EmptyCooldown);
        MicroHidSounds.play(World, User.getEyePosition(), MicroHidSounds.Empty);
        Vec3 Front = front(User);
        World.sendParticles(ParticleTypes.SMOKE, Front.x, Front.y, Front.z, 4, 0.08, 0.05, 0.08, 0.01);
        User.displayClientMessage(Component.translatable(MessagePrefix + "empty").withStyle(ChatFormatting.GRAY), true);
    }

    private static void exhaust(ServerLevel World, LivingEntity User, ItemStack Stack) {
        User.stopUsingItem();
        Vec3 Front = front(User);
        World.sendParticles(ParticleTypes.SMOKE, Front.x, Front.y, Front.z, 8, 0.12, 0.08, 0.12, 0.02);
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Front.x, Front.y, Front.z, 10, 0.15, 0.1, 0.15, 0.2);
        MicroHidSounds.play(World, User.getEyePosition(), MicroHidSounds.Empty);
        if (User instanceof Player Person) {
            Person.getCooldowns().addCooldown(Stack.getItem(), MicroHidEnergy.EmptyCooldown);
            Person.displayClientMessage(Component.translatable(MessagePrefix + "drained").withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static Vec3 front(LivingEntity User) {
        return User.getEyePosition().add(User.getLookAngle().scale(MuzzleReach)).add(0.0, -MuzzleDrop, 0.0);
    }

    public static Vec3 reach(Level World, Entity User, Vec3 Eye, Vec3 Look, double Range) {
        Vec3 Far = Eye.add(Look.scale(Range));
        BlockHitResult Wall = World.clip(new ClipContext(Eye, Far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User));
        return Wall.getType() == HitResult.Type.MISS ? Far : Wall.getLocation();
    }

    public static List<Struck> pulse(ServerLevel World, LivingEntity User) {
        GunStats Stats = GunKind.MicroHid.getStats();
        Vec3 Eye = User.getEyePosition();
        Vec3 Look = User.getLookAngle();
        Vec3 Far = Eye.add(Look.scale(Stats.getRange()));
        BlockHitResult Wall = World.clip(new ClipContext(Eye, Far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User));
        boolean Solid = Wall.getType() != HitResult.Type.MISS;
        Vec3 End = Solid ? Wall.getLocation() : Far;
        List<Struck> Hits = targets(World, User, Eye, End, Look);
        DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MicroHidRegistry.MicroHidDamage), null, User, Eye);
        for (Struck Hit : Hits) {
            Entity Target = Hit.Target();
            Target.invulnerableTime = 0;
            boolean Landed = Target.hurt(Source, Stats.getDamage());
            Entity Body = Target instanceof PartEntity<?> Part ? Part.getParent() : Target;
            if (Landed && Body instanceof LivingEntity Living && Living.isAlive()) {
                Living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SlowTicks, SlowLevel, false, false, true), User);
                push(Living, Look, Stats.getKnockback());
            }
            Vec3 Point = Hit.Point();
            World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Point.x, Point.y, Point.z, 6, 0.25, 0.3, 0.25, 0.35);
            World.sendParticles(GunsRegistry.EnergySpark.get(), Point.x, Point.y, Point.z, 3, 0.2, 0.25, 0.2, 0.08);
        }
        if (Solid) {
            World.sendParticles(ParticleTypes.ELECTRIC_SPARK, End.x, End.y, End.z, 5, 0.2, 0.2, 0.2, 0.3);
            World.gameEvent(GameEvent.PROJECTILE_LAND, End, GameEvent.Context.of(User));
        }
        User.gameEvent(GameEvent.PROJECTILE_SHOOT);
        return Hits;
    }

    public static List<Struck> targets(ServerLevel World, LivingEntity User, Vec3 Eye, Vec3 End, Vec3 Look) {
        Vec3 Splash = End.subtract(Look.scale(SplashPull));
        AABB Area = new AABB(Eye, End).inflate(SplashRadius + 1.0);
        Entity Ride = User.getRootVehicle();
        List<Struck> Hits = new ArrayList<>();
        Set<Entity> Bodies = new HashSet<>();
        for (Entity Candidate : World.getEntities(User, Area, Candidate -> struckBy(User, Candidate, Ride))) {
            AABB Box = Candidate.getBoundingBox();
            AABB Wide = Box.inflate(BeamRadius);
            Optional<Vec3> Clip = Wide.contains(Eye) ? Optional.of(Eye) : Wide.clip(Eye, End);
            Vec3 Point;
            if (Clip.isPresent()) {
                Point = Clip.get();
            } else if (Box.inflate(SplashRadius).contains(Splash) && clear(World, User, Splash, Box.getCenter())) {
                Point = Splash;
            } else {
                continue;
            }
            Entity Body = Candidate instanceof PartEntity<?> Part ? Part.getParent() : Candidate;
            if (!Bodies.add(Body)) {
                continue;
            }
            Hits.add(new Struck(Candidate, Point, Eye.distanceTo(Point)));
        }
        Hits.sort(Comparator.comparingDouble(Struck::Distance));
        return Hits.size() > MaxTargets ? new ArrayList<>(Hits.subList(0, MaxTargets)) : Hits;
    }

    private static boolean clear(Level World, Entity User, Vec3 From, Vec3 To) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User)).getType() == HitResult.Type.MISS;
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

    private static void push(LivingEntity Target, Vec3 Look, float Knockback) {
        double Resist = Target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double Strength = Knockback * Math.max(0.0, 1.0 - Resist);
        if (Strength <= 0.0) {
            return;
        }
        Vec3 Flat = new Vec3(Look.x, 0.0, Look.z);
        Flat = Flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : Flat.normalize();
        Target.push(Flat.x * Strength, Target.onGround() ? KnockbackLift : 0.0, Flat.z * Strength);
        Target.hurtMarked = true;
    }
}
