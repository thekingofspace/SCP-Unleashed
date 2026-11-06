package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class Scp2845Powers {
    public static final int GlobalGap = 30;
    public static final int DefyGap = 20;
    public static final double GoreReach = 4.2;
    public static final float GoreDamage = 14.0f;
    public static final int GoreHit = 8;
    public static final double StompReach = 6.0;
    public static final float StompDamage = 8.0f;
    public static final int StompHit = 12;
    public static final int RootTicks = 50;
    public static final double BlastRange = 28.0;
    public static final int BlastFuse = 30;
    public static final float BlastPower = 2.6f;
    public static final double BlastRing = 2.5;
    public static final double PetrifyRange = 24.0;
    public static final double PetrifyBreak = 28.0;
    public static final int PetrifyBlind = 8;
    public static final double UpliftReach = 12.0;
    public static final int UpliftRise = 15;
    public static final int UpliftTicks = 50;
    public static final int UpliftLevel = 2;
    public static final double WarpFar = 22.0;
    public static final double StampedeMin = 8.0;
    public static final double StampedeMax = 30.0;
    public static final double StampedeBack = 16.0;
    public static final double[] StampedeLanes = {-3.0, -1.0, 1.0, 3.0};

    public enum Kind {
        Gore(30, 18),
        Stomp(160, 24),
        Blast(120, 34),
        Petrify(400, 50),
        Uplift(300, 26),
        Warp(160, 1),
        Stampede(600, 30);

        public final int Cooldown;
        public final int Duration;

        Kind(int Cooldown, int Duration) {
            this.Cooldown = Cooldown;
            this.Duration = Duration;
        }
    }

    private final Scp2845 Owner;
    private final int[] Cooldowns = new int[Kind.values().length];
    @Nullable
    private Kind Active;
    private int Elapsed;
    private Vec3 Spot = Vec3.ZERO;
    @Nullable
    private UUID Victim;
    private int Global;
    private int Blind;

    public Scp2845Powers(Scp2845 Owner) {
        this.Owner = Owner;
    }

    public void cool() {
        for (int Index = 0; Index < Cooldowns.length; Index++) {
            if (Cooldowns[Index] > 0) {
                Cooldowns[Index]--;
            }
        }
        if (Global > 0) {
            Global--;
        }
    }

    public boolean busy() {
        return Active != null;
    }

    @Nullable
    public Kind active() {
        return Active;
    }

    public boolean ready(Kind Which) {
        return Cooldowns[Which.ordinal()] <= 0;
    }

    public int cooldown(Kind Which) {
        return Cooldowns[Which.ordinal()];
    }

    public void cancel() {
        Active = null;
        Elapsed = 0;
        Victim = null;
        Blind = 0;
    }

    public void defy() {
        Global = DefyGap;
    }

    public void clearCooldowns() {
        Arrays.fill(Cooldowns, 0);
        Global = 0;
    }

    private List<LivingEntity> around(ServerLevel World, double Reach) {
        return World.getEntitiesOfClass(LivingEntity.class, Owner.getBoundingBox().inflate(Reach, 4.0, Reach),
                Candidate -> Candidate != Owner && !(Candidate instanceof Scp2845) && Candidate.isAlive() && Owner.distanceTo(Candidate) <= Reach && exposed(Candidate));
    }

    private static boolean exposed(LivingEntity Candidate) {
        return !(Candidate instanceof Player User) || !User.isCreative() && !User.isSpectator();
    }

    public boolean choose(ServerLevel World, LivingEntity Target) {
        if (Global > 0 || Active != null) {
            return false;
        }
        double Gap = Owner.distanceTo(Target);
        boolean Sight = Owner.hasLineOfSight(Target);
        if (Gap <= GoreReach - 0.6 && ready(Kind.Gore)) {
            return start(World, Kind.Gore, Target);
        }
        int Crowd = around(World, StompReach - 0.5).size();
        if (ready(Kind.Stomp) && (Crowd >= 2 || Gap <= 4.5 && Owner.getRandom().nextInt(3) == 0)) {
            return start(World, Kind.Stomp, Target);
        }
        if (Owner.getRandom().nextInt(6) != 0) {
            return false;
        }
        Kind[] Options = new Kind[5];
        int[] Weights = new int[5];
        int Count = 0;
        int Total = 0;
        if (ready(Kind.Warp) && (Gap > WarpFar || !Sight && Gap > 10.0)) {
            Options[Count] = Kind.Warp;
            Weights[Count] = 6;
            Total += Weights[Count++];
        }
        if (ready(Kind.Blast) && Gap <= BlastRange && Gap >= 5.0) {
            Options[Count] = Kind.Blast;
            Weights[Count] = 4;
            Total += Weights[Count++];
        }
        if (ready(Kind.Petrify) && Sight && Gap <= PetrifyRange && Gap >= 4.0) {
            Options[Count] = Kind.Petrify;
            Weights[Count] = 3;
            Total += Weights[Count++];
        }
        if (ready(Kind.Uplift) && Gap <= UpliftReach) {
            Options[Count] = Kind.Uplift;
            Weights[Count] = 2;
            Total += Weights[Count++];
        }
        if (ready(Kind.Stampede) && Gap >= StampedeMin && Gap <= StampedeMax) {
            Options[Count] = Kind.Stampede;
            Weights[Count] = 2;
            Total += Weights[Count++];
        }
        if (Count == 0) {
            return false;
        }
        int Roll = Owner.getRandom().nextInt(Total);
        for (int Index = 0; Index < Count; Index++) {
            Roll -= Weights[Index];
            if (Roll < 0) {
                return start(World, Options[Index], Target);
            }
        }
        return false;
    }

    public boolean start(ServerLevel World, Kind Which, LivingEntity Target) {
        Active = Which;
        Elapsed = 0;
        Victim = Target.getUUID();
        Spot = Target.position();
        Blind = 0;
        Cooldowns[Which.ordinal()] = Which.Cooldown;
        Owner.getNavigation().stop();
        switch (Which) {
            case Gore -> {
                Owner.playAction("attack");
                Scp2845Sounds.play(World, Owner.position(), Scp2845Registry.Gore, 1.0f, 0.8f + Owner.getRandom().nextFloat() * 0.2f);
            }
            case Stomp -> {
                Owner.playAction("stomp");
                Owner.call(World, 0.9f);
            }
            case Blast -> {
                Owner.playAction("cast");
                Spot = groundUnder(World, Target.position());
                Scp2845Sounds.play(World, Spot, Scp2845Registry.Mark, 1.2f, 0.9f);
                Owner.chime(World, 1.0f);
            }
            case Petrify -> {
                Owner.playAction("cast");
                Owner.chime(World, 1.1f);
                Scp2845Sounds.play(World, Target.getEyePosition(), Scp2845Registry.GrindLayers, 0.9f, 0.6f);
            }
            case Uplift -> {
                Owner.playAction("cast");
                Owner.chime(World, 1.2f);
            }
            case Warp -> warpNear(World, Target);
            case Stampede -> {
                Owner.playAction("cast");
                Owner.call(World, 1.6f);
                Scp2845Sounds.play(World, Target.position(), Scp2845Registry.Gallop, 1.4f, 0.7f);
            }
        }
        return true;
    }

    public void tick(ServerLevel World, @Nullable LivingEntity Target) {
        if (Active == null) {
            return;
        }
        Kind Which = Active;
        LivingEntity Mark = victim(World);
        Owner.getNavigation().stop();
        if (Mark != null) {
            Owner.getLookControl().setLookAt(Mark, 40.0f, 40.0f);
        }
        switch (Which) {
            case Gore -> {
                if (Elapsed == GoreHit && Mark != null) {
                    gore(Mark);
                }
            }
            case Stomp -> {
                if (Elapsed == StompHit) {
                    stomp(World);
                }
            }
            case Blast -> {
                if (Elapsed < BlastFuse && Elapsed % 3 == 0) {
                    markRing(World, Spot, Elapsed);
                }
                if (Elapsed == BlastFuse) {
                    World.explode(Owner, null, null, Spot.x, Spot.y + 0.5, Spot.z, BlastPower, false, Level.ExplosionInteraction.MOB);
                    Owner.chime(World, 0.8f);
                }
            }
            case Petrify -> {
                if (Mark == null || Owner.distanceTo(Mark) > PetrifyBreak) {
                    fizzle();
                    return;
                }
                Blind = Owner.hasLineOfSight(Mark) ? 0 : Blind + 1;
                if (Blind > PetrifyBlind) {
                    fizzle();
                    return;
                }
                if (Elapsed % 5 == 0) {
                    dust(World, Mark);
                    Mark.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false), Owner);
                }
                if (Elapsed % 15 == 0) {
                    Scp2845Sounds.play(World, Mark.getEyePosition(), Scp2845Registry.Grind, 0.9f, 0.55f + Elapsed / 100.0f);
                }
                if (Elapsed == Which.Duration - 1) {
                    Scp2845Petrify.petrify(World, Mark, Owner);
                }
            }
            case Uplift -> {
                List<LivingEntity> Caught = around(World, UpliftReach);
                if (Elapsed < UpliftRise) {
                    for (LivingEntity Each : Caught) {
                        World.sendParticles(ParticleTypes.END_ROD, Each.getX(), Each.getY() + 0.1, Each.getZ(), 2, 0.4, 0.05, 0.4, 0.02);
                    }
                } else if (Elapsed == UpliftRise) {
                    for (LivingEntity Each : Caught) {
                        Each.addEffect(new MobEffectInstance(MobEffects.LEVITATION, UpliftTicks, UpliftLevel), Owner);
                    }
                    Scp2845Sounds.play(World, Owner.position(), Scp2845Registry.Uplift, 1.3f, 0.9f);
                }
            }
            case Stampede -> {
                if (Elapsed == 8 && Mark != null) {
                    stampede(World, Mark);
                }
            }
            default -> {
            }
        }
        Elapsed++;
        if (Elapsed >= Which.Duration) {
            finish();
        }
    }

    private void finish() {
        Active = null;
        Elapsed = 0;
        Victim = null;
        Global = GlobalGap;
    }

    private void fizzle() {
        Cooldowns[Kind.Petrify.ordinal()] = Kind.Petrify.Cooldown / 2;
        finish();
    }

    @Nullable
    private LivingEntity victim(ServerLevel World) {
        if (Victim == null) {
            return null;
        }
        Entity Found = World.getEntity(Victim);
        return Found instanceof LivingEntity Living && Living.isAlive() && exposed(Living) ? Living : null;
    }

    public boolean gore(Entity Target) {
        if (!(Owner.level() instanceof ServerLevel World) || Owner.distanceTo(Target) > GoreReach) {
            return false;
        }
        boolean Hit = Target.hurt(World.damageSources().mobAttack(Owner), GoreDamage);
        if (Hit) {
            Vec3 Push = Target.position().subtract(Owner.position()).multiply(1.0, 0.0, 1.0);
            Push = Push.lengthSqr() < 1.0e-4 ? Owner.getLookAngle() : Push.normalize();
            Target.push(Push.x * 1.2, 0.45, Push.z * 1.2);
            Target.hurtMarked = true;
            Scp2845Sounds.play(World, Target.position(), Scp2845Registry.Gore, 1.0f, 0.6f);
        }
        return Hit;
    }

    private void stomp(ServerLevel World) {
        Vec3 Centre = Owner.position();
        Scp2845Sounds.play(World, Centre, Scp2845Registry.StompLayers, 1.4f, 0.7f);
        BlockState Ground = World.getBlockState(Owner.blockPosition().below());
        for (int Index = 0; Index < 24; Index++) {
            double Angle = Index * Math.PI * 2.0 / 24.0;
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground.isAir() ? Blocks.DIRT.defaultBlockState() : Ground), Centre.x + Math.cos(Angle) * 3.0,
                    Centre.y + 0.2, Centre.z + Math.sin(Angle) * 3.0, 3, 0.4, 0.1, 0.4, 0.15);
        }
        World.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, Centre.x, Centre.y + 0.4, Centre.z, 40, StompReach * 0.5, 0.2, StompReach * 0.5, 0.0);
        for (LivingEntity Each : around(World, StompReach)) {
            double Distance = Owner.distanceTo(Each);
            float Strength = (float) Mth.clamp(1.0 - Distance / StompReach, 0.25, 1.0);
            Each.hurt(World.damageSources().mobAttack(Owner), StompDamage * Strength);
            Vec3 Away = Each.position().subtract(Centre).multiply(1.0, 0.0, 1.0);
            Away = Away.lengthSqr() < 1.0e-4 ? new Vec3(1.0, 0.0, 0.0) : Away.normalize();
            Each.push(Away.x * Strength, 0.45 * Strength, Away.z * Strength);
            Each.hurtMarked = true;
            Each.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RootTicks, 3), Owner);
            World.sendParticles(ParticleTypes.HAPPY_VILLAGER, Each.getX(), Each.getY() + 0.2, Each.getZ(), 6, 0.3, 0.1, 0.3, 0.0);
        }
    }

    private static Vec3 groundUnder(ServerLevel World, Vec3 At) {
        BlockPos Cursor = BlockPos.containing(At);
        for (int Step = 0; Step < 8; Step++) {
            if (!World.getBlockState(Cursor.below()).getCollisionShape(World, Cursor.below()).isEmpty()) {
                break;
            }
            Cursor = Cursor.below();
        }
        return new Vec3(At.x, Cursor.getY(), At.z);
    }

    private static void markRing(ServerLevel World, Vec3 Spot, int Elapsed) {
        int Points = 16;
        double Turn = Elapsed * 0.08;
        for (int Index = 0; Index < Points; Index++) {
            double Angle = Turn + Index * Math.PI * 2.0 / Points;
            World.sendParticles(ParticleTypes.END_ROD, Spot.x + Math.cos(Angle) * BlastRing, Spot.y + 0.15, Spot.z + Math.sin(Angle) * BlastRing, 1, 0.0, 0.0, 0.0, 0.0);
        }
        World.sendParticles(ParticleTypes.ENCHANT, Spot.x, Spot.y + 0.4, Spot.z, 10, 1.0, 0.1, 1.0, 0.6);
    }

    private static void dust(ServerLevel World, LivingEntity Mark) {
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), Mark.getX(), Mark.getY() + Mark.getBbHeight() * 0.3, Mark.getZ(),
                8, Mark.getBbWidth() * 0.4, Mark.getBbHeight() * 0.3, Mark.getBbWidth() * 0.4, 0.02);
        World.sendParticles(ParticleTypes.ASH, Mark.getX(), Mark.getY() + Mark.getBbHeight() * 0.5, Mark.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
    }

    private void stampede(ServerLevel World, LivingEntity Mark) {
        Vec3 Way = Mark.position().subtract(Owner.position()).multiply(1.0, 0.0, 1.0);
        Way = Way.lengthSqr() < 1.0e-4 ? Owner.getLookAngle().multiply(1.0, 0.0, 1.0).normalize() : Way.normalize();
        Vec3 Across = new Vec3(-Way.z, 0.0, Way.x);
        Vec3 Start = Mark.position().subtract(Way.scale(StampedeBack));
        for (double Lane : StampedeLanes) {
            Vec3 At = Start.add(Across.scale(Lane));
            Scp2845Spirit.charge(World, groundUnder(World, At.add(0.0, 3.0, 0.0)), Way, Owner);
        }
    }

    private void warpNear(ServerLevel World, LivingEntity Target) {
        for (int Attempt = 0; Attempt < 12; Attempt++) {
            double Angle = Owner.getRandom().nextDouble() * Math.PI * 2.0;
            double Reach = 5.0 + Owner.getRandom().nextDouble() * 3.0;
            Vec3 At = Target.position().add(Math.cos(Angle) * Reach, 0.0, Math.sin(Angle) * Reach);
            if (warpTo(World, At)) {
                return;
            }
        }
    }

    public boolean warpTo(ServerLevel World, Vec3 At) {
        Vec3 From = Owner.position();
        for (int Lift = 4; Lift >= -4; Lift--) {
            Vec3 Candidate = new Vec3(At.x, Math.floor(At.y) + Lift, At.z);
            AABB Room = Owner.getType().getDimensions().makeBoundingBox(Candidate);
            BlockPos Below = BlockPos.containing(Candidate).below();
            if (!World.noCollision(Owner, Room) || World.getBlockState(Below).getCollisionShape(World, Below).isEmpty() || !World.getFluidState(Below.above()).isEmpty()) {
                continue;
            }
            World.sendParticles(ParticleTypes.CHERRY_LEAVES, From.x, From.y + 1.5, From.z, 30, 0.8, 1.2, 0.8, 0.02);
            World.sendParticles(ParticleTypes.END_ROD, From.x, From.y + 1.5, From.z, 20, 0.6, 1.0, 0.6, 0.03);
            Owner.teleportTo(Candidate.x, Candidate.y, Candidate.z);
            Owner.getNavigation().stop();
            World.sendParticles(ParticleTypes.CHERRY_LEAVES, Candidate.x, Candidate.y + 1.5, Candidate.z, 30, 0.8, 1.2, 0.8, 0.02);
            World.sendParticles(ParticleTypes.END_ROD, Candidate.x, Candidate.y + 1.5, Candidate.z, 20, 0.6, 1.0, 0.6, 0.03);
            Scp2845Sounds.play(World, From, Scp2845Registry.Warp, 1.0f, 0.9f);
            Scp2845Sounds.play(World, Candidate, Scp2845Registry.Warp, 1.0f, 1.1f);
            return true;
        }
        return false;
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        Tag.putIntArray("Cooldowns", Cooldowns);
        Tag.putInt("Global", Global);
        return Tag;
    }

    public void load(CompoundTag Tag) {
        int[] Saved = Tag.getIntArray("Cooldowns");
        for (int Index = 0; Index < Math.min(Saved.length, Cooldowns.length); Index++) {
            Cooldowns[Index] = Saved[Index];
        }
        Global = Tag.getInt("Global");
    }
}
