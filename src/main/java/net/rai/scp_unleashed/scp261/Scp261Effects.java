package net.rai.scp_unleashed.scp261;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class Scp261Effects {
    public static final int BuoyantTicks = 900;
    public static final int EchoTicks = 800;
    public static final int ChargeTicks = 1200;
    public static final int TopsyTicks = 240;
    public static final int HollowTicks = 1200;
    public static final int FrostTicks = 900;
    public static final int FortuneTicks = 2400;
    public static final int EmberTicks = 1200;
    public static final int InkTicks = 300;
    public static final int HeavyTicks = 800;
    public static final int BorrowedTicks = 1200;
    public static final int PocketTicks = 900;
    public static final int RewindTicks = 200;
    public static final int HiccupTicks = 600;
    public static final int DreadTicks = 600;
    public static final int InkInterval = 10;
    public static final int InkGlowTicks = 30;
    public static final double InkRadius = 16.0;
    public static final int InkCap = 32;
    public static final int HiccupInterval = 5;
    public static final int HiccupChance = 12;
    public static final double HiccupLift = 0.36;
    public static final double HiccupAirLift = 0.15;
    public static final int FrostRadius = 3;
    public static final int FrostCueGap = 12;
    public static final float HollowSaturation = 10.0f;
    public static final int TomorrowFood = 20;
    public static final float TomorrowSaturation = 8.0f;
    public static final int TomorrowAfter = 1;
    public static final double SinkPull = 0.02;
    public static final float ThudDistance = 2.0f;
    public static final float ThudMultiplier = 1.25f;
    public static final int ChainHops = 3;
    public static final double ChainReach = 6.0;
    public static final float ChainDamage = 4.0f;
    public static final int EmberIgniteSeconds = 3;
    public static final float FortuneChance = 0.4f;
    public static final float MaxSaturation = 20.0f;

    private Scp261Effects() {
    }

    public static void give(LivingEntity Target, Holder<MobEffect> Effect, int Ticks) {
        Target.addEffect(new MobEffectInstance(Effect, Ticks, 0, false, false, true));
    }

    public static void consume(ServerLevel World, ServerPlayer User, Scp261Snack Treat) {
        Vec3 Mouth = User.getEyePosition();
        switch (Treat) {
            case GravityGum -> give(User, Scp261Registry.Buoyant, BuoyantTicks);
            case EchoCrisps -> give(User, Scp261Registry.Echoing, EchoTicks);
            case StaticSoda -> {
                give(User, Scp261Registry.Charged, ChargeTicks);
                Scp261Registry.playCue(World, Mouth, List.of(Scp261Registry.ZapCrackle), SoundSource.PLAYERS, 0.5f, 1.4f);
            }
            case MirrorMints -> {
                give(User, Scp261Registry.Topsy, TopsyTicks);
                Scp261Registry.playCue(World, Mouth, Scp261Registry.FlipCue, SoundSource.PLAYERS, 0.6f, 1.0f);
            }
            case HollowBar -> give(User, Scp261Registry.Hollow, HollowTicks);
            case FrostFizz -> {
                give(User, Scp261Registry.Frostfoot, FrostTicks);
                Scp261Registry.playCue(World, Mouth, Scp261Registry.FreezeCue, SoundSource.PLAYERS, 0.6f, 1.2f);
            }
            case LuckyWafer -> give(User, Scp261Registry.Fortunate, FortuneTicks);
            case EmberJerky -> {
                give(User, MobEffects.FIRE_RESISTANCE, EmberTicks);
                give(User, Scp261Registry.Smoldering, EmberTicks);
            }
            case InkToffee -> {
                give(User, MobEffects.BLINDNESS, InkTicks);
                give(User, Scp261Registry.Inksight, InkTicks);
                World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp261Registry.Ink.get(), SoundSource.PLAYERS, 0.7f, 1.1f);
                inksight(User, 0);
            }
            case WeightGummies -> give(User, Scp261Registry.Heavy, HeavyTicks);
            case TasteOfTomorrow -> {
                FoodData Food = User.getFoodData();
                Food.setFoodLevel(TomorrowFood);
                Food.setSaturation(Math.max(Food.getSaturationLevel(), TomorrowSaturation));
                give(User, Scp261Registry.Borrowed, BorrowedTicks);
            }
            case PocketPretzels -> {
                give(User, Scp261Registry.Pocket, PocketTicks);
                Scp261Registry.playCue(World, Mouth, Scp261Registry.ShrinkCue, SoundSource.PLAYERS, 0.7f, 1.0f);
            }
            case RewindRaisins -> {
                Scp261Rewind.anchor(User);
                give(User, Scp261Registry.Rewinding, RewindTicks);
                Scp261Registry.playCue(World, Mouth, List.of(Scp261Registry.RewindHum), SoundSource.PLAYERS, 0.5f, 1.3f);
            }
            case HiccupPops -> give(User, Scp261Registry.Hiccups, HiccupTicks);
            case MobMorsels -> Scp261Pals.summon(World, User);
            case FrightBites -> {
                give(User, Scp261Registry.Dreadful, DreadTicks);
                Scp261Registry.playCue(World, Mouth, Scp261Registry.DreadCue, SoundSource.PLAYERS, 0.6f, 1.0f);
                Scp261Dread.pulse(User, 0);
            }
        }
    }

    public static void crackle(LivingEntity Target, int Amplifier) {
        if (Target.level() instanceof ServerLevel World && World.getRandom().nextInt(3) == 0) {
            World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Target.getX(), Target.getY() + Target.getBbHeight() * 0.55, Target.getZ(), 2, 0.3, 0.25, 0.3, 0.04);
        }
    }

    public static void embers(LivingEntity Target, int Amplifier) {
        if (Target.level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.SMALL_FLAME, Target.getX(), Target.getY() + 0.08, Target.getZ(), 1, 0.22, 0.02, 0.22, 0.004);
            if (World.getRandom().nextInt(4) == 0) {
                World.sendParticles(ParticleTypes.SMOKE, Target.getX(), Target.getY() + 0.12, Target.getZ(), 1, 0.2, 0.02, 0.2, 0.01);
            }
        }
    }

    public static void inksight(LivingEntity Target, int Amplifier) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return;
        }
        List<LivingEntity> Near = World.getEntitiesOfClass(LivingEntity.class, Target.getBoundingBox().inflate(InkRadius),
                Other -> Other != Target && Other.isAlive() && !(Other instanceof Player) && !Other.isInvisible());
        Near.sort(Comparator.comparingDouble(Other -> Other.distanceToSqr(Target)));
        for (int Index = 0; Index < Near.size() && Index < InkCap; Index++) {
            Near.get(Index).addEffect(new MobEffectInstance(MobEffects.GLOWING, InkGlowTicks, 0, true, false, false));
        }
    }

    public static void frost(LivingEntity Target, int Amplifier) {
        if (!(Target.level() instanceof ServerLevel World) || !Target.onGround() || Target.isSpectator()) {
            return;
        }
        BlockState Ice = Blocks.FROSTED_ICE.defaultBlockState();
        BlockPos Centre = BlockPos.containing(Target.position()).below();
        int Frozen = 0;
        for (BlockPos Spot : BlockPos.betweenClosed(Centre.offset(-FrostRadius, 0, -FrostRadius), Centre.offset(FrostRadius, 0, FrostRadius))) {
            if (Spot.distToCenterSqr(Target.getX(), Spot.getY() + 0.5, Target.getZ()) >= FrostRadius * FrostRadius) {
                continue;
            }
            if (Target instanceof Player User && !World.mayInteract(User, Spot)) {
                continue;
            }
            BlockState At = World.getBlockState(Spot);
            if (!At.is(Blocks.WATER) || !World.getFluidState(Spot).isSourceOfType(Fluids.WATER) || !World.getBlockState(Spot.above()).isAir()) {
                continue;
            }
            if (!World.isUnobstructed(Ice, Spot, CollisionContext.empty())) {
                continue;
            }
            if (World.setBlockAndUpdate(Spot.immutable(), Ice)) {
                Frozen++;
            }
        }
        if (Frozen > 0) {
            World.sendParticles(ParticleTypes.SNOWFLAKE, Target.getX(), Target.getY() + 0.1, Target.getZ(), Math.min(6, Frozen), 0.8, 0.05, 0.8, 0.01);
            if (Target.tickCount % FrostCueGap == 0) {
                Scp261Registry.playCue(World, Target.position(), Scp261Registry.FreezeCue, SoundSource.PLAYERS, 0.25f, 1.4f + World.getRandom().nextFloat() * 0.2f);
            }
        }
    }

    public static void sink(LivingEntity Target, int Amplifier) {
        boolean Local = Target.level().isClientSide ? Target instanceof Player User && User.isLocalPlayer() : !(Target instanceof Player);
        if (Local && Target.isInWater() && !(Target instanceof Player User && User.getAbilities().flying)) {
            Target.setDeltaMovement(Target.getDeltaMovement().add(0.0, -SinkPull, 0.0));
        }
    }

    public static void rewindTrail(LivingEntity Target, int Amplifier) {
        if (Target.level() instanceof ServerLevel World && Target instanceof ServerPlayer User) {
            Scp261Rewind.trail(World, User);
        }
    }

    public static void hiccup(LivingEntity Target, int Amplifier) {
        if (!(Target.level() instanceof ServerLevel World) || Target.isPassenger() || Target.isSpectator()) {
            return;
        }
        if (Target instanceof Player User && User.getAbilities().flying) {
            return;
        }
        if (World.getRandom().nextInt(HiccupChance) != 0) {
            return;
        }
        hop(World, Target);
    }

    public static void hop(ServerLevel World, LivingEntity Target) {
        Vec3 Motion = Target.getDeltaMovement();
        double Lift = Target.onGround() ? HiccupLift : HiccupAirLift;
        Target.setDeltaMovement(Motion.x, Math.max(Motion.y, 0.0) + Lift, Motion.z);
        Target.hurtMarked = true;
        Vec3 Mouth = Target.getEyePosition();
        Scp261Registry.playCue(World, Mouth, Scp261Registry.HiccupCue, SoundSource.PLAYERS, 0.6f, 0.9f + World.getRandom().nextFloat() * 0.25f);
        World.sendParticles(ParticleTypes.BUBBLE_POP, Mouth.x, Mouth.y - 0.1, Mouth.z, 3, 0.15, 0.1, 0.15, 0.02);
    }

    public static void jumped(LivingEntity Target) {
        if (Target.level() instanceof ServerLevel World && Target.hasEffect(Scp261Registry.Buoyant)) {
            Scp261Registry.playCue(World, Target.position(), Scp261Registry.GumCue, SoundSource.PLAYERS, 0.45f, 0.9f + World.getRandom().nextFloat() * 0.2f);
            World.sendParticles(ParticleTypes.CLOUD, Target.getX(), Target.getY() + 0.05, Target.getZ(), 4, 0.25, 0.02, 0.25, 0.01);
        }
    }

    public static float landed(LivingEntity Target, float Distance) {
        if (!(Target.level() instanceof ServerLevel World) || !Target.hasEffect(Scp261Registry.Heavy) || Distance < ThudDistance) {
            return 1.0f;
        }
        BlockState Ground = World.getBlockState(Target.getOnPos());
        Scp261Registry.playCue(World, Target.position(), Scp261Registry.ThudCue, SoundSource.PLAYERS, 0.8f, 0.8f + World.getRandom().nextFloat() * 0.15f);
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Target.getX(), Target.getY() + 0.05, Target.getZ(), 14, 0.4, 0.05, 0.4, 0.12);
        }
        return ThudMultiplier;
    }

    public static void struck(ServerLevel World, Player Attacker, LivingEntity Victim) {
        if (Attacker.hasEffect(Scp261Registry.Smoldering)) {
            Victim.igniteForSeconds(EmberIgniteSeconds);
            Scp261Registry.playCue(World, Victim.position(), List.of(Scp261Registry.Sizzle), SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        if (Attacker.hasEffect(Scp261Registry.Charged)) {
            Attacker.removeEffect(Scp261Registry.Charged);
            chain(World, Attacker, Victim);
        }
    }

    public static int chain(ServerLevel World, Player Attacker, LivingEntity First) {
        List<LivingEntity> Struck = new ArrayList<>();
        Struck.add(First);
        LivingEntity From = First;
        DamageSource Spark = World.damageSources().source(Scp261Registry.StaticDamage, Attacker);
        Scp261Registry.playCue(World, First.position(), Scp261Registry.ZapCue, SoundSource.PLAYERS, 0.8f, 1.0f + World.getRandom().nextFloat() * 0.2f);
        sparkLine(World, Attacker.position().add(0.0, Attacker.getBbHeight() * 0.6, 0.0), centre(First));
        int Hits = 0;
        for (int Hop = 0; Hop < ChainHops; Hop++) {
            LivingEntity Source = From;
            List<LivingEntity> Options = World.getEntitiesOfClass(LivingEntity.class, Source.getBoundingBox().inflate(ChainReach),
                    Other -> Other instanceof Enemy && Other.isAlive() && !Struck.contains(Other) && Other != Attacker && !(Other instanceof Player));
            if (Options.isEmpty()) {
                break;
            }
            Options.sort(Comparator.comparingDouble(Other -> Other.distanceToSqr(Source)));
            LivingEntity Next = Options.getFirst();
            sparkLine(World, centre(Source), centre(Next));
            Next.hurt(Spark, ChainDamage);
            Struck.add(Next);
            From = Next;
            Hits++;
        }
        return Hits;
    }

    private static Vec3 centre(LivingEntity Target) {
        return Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0);
    }

    private static void sparkLine(ServerLevel World, Vec3 From, Vec3 To) {
        Vec3 Step = To.subtract(From);
        int Points = Math.max(3, (int) (Step.length() * 4.0));
        for (int Index = 0; Index <= Points; Index++) {
            Vec3 At = From.add(Step.scale(Index / (double) Points));
            World.sendParticles(ParticleTypes.ELECTRIC_SPARK, At.x, At.y, At.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    public static void fortune(ServerLevel World, Player Killer, Collection<ItemEntity> Drops) {
        if (!Killer.hasEffect(Scp261Registry.Fortunate) || Drops.isEmpty() || World.getRandom().nextFloat() >= FortuneChance) {
            return;
        }
        List<ItemEntity> Doubles = Drops.stream().filter(Drop -> Drop.getItem().getMaxStackSize() > 1).toList();
        if (Doubles.isEmpty()) {
            return;
        }
        ItemEntity Pick = Doubles.get(World.getRandom().nextInt(Doubles.size()));
        ItemEntity Extra = new ItemEntity(World, Pick.getX(), Pick.getY(), Pick.getZ(), Pick.getItem().copyWithCount(1));
        Extra.setDefaultPickUpDelay();
        Drops.add(Extra);
        World.sendParticles(ParticleTypes.HAPPY_VILLAGER, Pick.getX(), Pick.getY() + 0.3, Pick.getZ(), 5, 0.25, 0.2, 0.25, 0.0);
        Scp261Registry.playCue(World, Pick.position(), List.of(Scp261Registry.Lucky), SoundSource.PLAYERS, 0.5f, 1.0f + World.getRandom().nextFloat() * 0.3f);
    }

    public static void hollowEnds(ServerPlayer User) {
        FoodData Food = User.getFoodData();
        Food.setSaturation(Math.min(MaxSaturation, Food.getSaturationLevel() + HollowSaturation));
        Scp261Registry.playCue(User.level(), User.getEyePosition(), List.of(Scp261Registry.Sated), SoundSource.PLAYERS, 0.6f, 0.9f);
    }

    public static void tomorrowArrives(ServerPlayer User) {
        FoodData Food = User.getFoodData();
        Food.setFoodLevel(TomorrowAfter);
        Food.setSaturation(0.0f);
        Scp261Registry.playCue(User.level(), User.getEyePosition(), List.of(Scp261Registry.Growl), SoundSource.PLAYERS, 0.5f, 0.7f);
    }

    public static boolean roomToGrow(LivingEntity Target) {
        float Scale = Target.getScale();
        if (Scale >= 0.999f) {
            return true;
        }
        EntityDimensions Full = Target.getDimensions(Target.getPose()).scale(1.0f / Math.max(0.05f, Scale));
        AABB Box = Full.makeBoundingBox(Target.position()).deflate(1.0e-3);
        return Target.level().noCollision(Target, Box);
    }

    public static void grew(LivingEntity Target) {
        Level World = Target.level();
        Scp261Registry.playCue(World, Target.getEyePosition(), Scp261Registry.ShrinkCue, SoundSource.PLAYERS, 0.6f, 0.6f);
    }
}
