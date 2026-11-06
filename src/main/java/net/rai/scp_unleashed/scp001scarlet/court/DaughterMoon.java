package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DaughterMoon extends CourtDaughter {
    public static final int Pillars = MoveA;
    public static final int Tide = MoveB;
    public static final int Summon = MoveC;
    public static final int Fullness = MoveD;
    public static final int Waning = MoveE;
    public static final int Waxing = 0;
    public static final int Full = 1;
    public static final int Wane = 2;
    public static final int NewMoon = 3;
    public static final int[] Cycle = {90, 140, 90, 120};
    public static final int[] EclipseCycle = {60, 100, 60, 80};
    public static final int PillarTelegraph = 30;
    public static final double PillarRadius = 1.5;
    public static final float PillarDamage = 9.0f;
    public static final int PillarEvery = 100;
    public static final int BeamEvery = 40;
    public static final int BeamTelegraph = 20;
    public static final float BeamDamage = 8.0f;
    public static final double BeamReach = 40.0;
    public static final int TideTelegraph = 40;
    public static final int TidePull = 30;
    public static final double TideRange = 18.0;
    public static final double TideBurst = 4.0;
    public static final float TideDamage = 8.0f;
    public static final int ZoneEvery = 160;
    public static final int ZoneLife = 200;
    public static final double ZoneRadius = 3.0;
    public static final double Hover = 0.5;
    public static final float NewMoonAlpha = 0.15f;
    private static final EntityDataAccessor<Integer> MoonData = SynchedEntityData.defineId(DaughterMoon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FadeData = SynchedEntityData.defineId(DaughterMoon.class, EntityDataSerializers.INT);
    private final List<Fall> Falls = new ArrayList<>();
    private final List<Ray> Rays = new ArrayList<>();
    private final List<Zone> Zones = new ArrayList<>();
    private int CycleTick;
    private int PillarTimer = PillarEvery;
    private int ZoneTimer = ZoneEvery / 2;
    private int Rotation;
    private boolean Eclipsed;
    private boolean Pushing;

    public record Fall(Vec3 Spot, int At) {
    }

    public record Ray(Vec3 To, int At) {
    }

    public record Zone(Vec3 Centre, int Until) {
    }

    public DaughterMoon(EntityType<? extends Monster> Type, Level World) {
        super(Type, World);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(MoonData, Waxing);
        Builder.define(FadeData, 100);
    }

    @Override
    public int index() {
        return 3;
    }

    @Override
    public String key() {
        return "moon";
    }

    @Override
    protected BossEvent.BossBarColor barColour() {
        return BossEvent.BossBarColor.PURPLE;
    }

    public int moon() {
        return entityData.get(MoonData);
    }

    public boolean fullMoon() {
        return moon() == Full && !dying();
    }

    public boolean newMoon() {
        return moon() == NewMoon && !dying();
    }

    public boolean translucent() {
        return newMoon() || dying();
    }

    public float alpha() {
        if (dying()) {
            return Math.max(0.0f, entityData.get(FadeData) / 100.0f);
        }
        return newMoon() ? NewMoonAlpha : 1.0f;
    }

    public boolean eclipsed() {
        return Eclipsed;
    }

    public List<Fall> falls() {
        return List.copyOf(Falls);
    }

    public List<Zone> zones() {
        return List.copyOf(Zones);
    }

    private int[] lengths() {
        return Eclipsed ? EclipseCycle : Cycle;
    }

    public void moon(int Phase) {
        entityData.set(MoonData, Phase);
        CycleTick = 0;
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Phase == Full) {
            CourtFx.sound(World, eyes(), "moon_full", 6.0f, 1.0f);
            CourtFx.burst(World, CourtFx.Moonlight, eyes(), 30, 1.6, 0.02);
            if (action() == None) {
                startAction(Fullness, 30);
            }
        } else if (Phase == NewMoon) {
            CourtFx.sound(World, eyes(), "moon_wane", 6.0f, 1.0f);
            CourtFx.burst(World, CourtFx.Indigo, eyes(), 30, 1.6, 0.02);
            if (action() == None) {
                startAction(Waning, 30);
            }
        }
    }

    @Override
    protected double nearRange() {
        return 7.0;
    }

    @Override
    protected double farRange() {
        return 14.0;
    }

    @Override
    protected int moveLength(int Move) {
        return switch (Move) {
            case Pillars -> 30;
            case Tide -> TideTelegraph + TidePull + 10;
            case Summon -> 30;
            case Fullness, Waning -> 30;
            default -> 20;
        };
    }

    @Override
    protected int shiftLength() {
        return 60;
    }

    @Override
    protected String clip(int Action) {
        return switch (Action) {
            case Entrance -> "entrance";
            case Death -> "death";
            case Shift -> "eclipse";
            case Pillars -> "pillars";
            case Tide -> "tide";
            case Summon -> "summon";
            case Fullness -> "full";
            case Waning -> "wane";
            default -> null;
        };
    }

    @Override
    protected void chooseMove(LivingEntity Target) {
        Rotation++;
        int[] Order = {Pillars, Tide, Pillars, Summon, Pillars, Tide};
        int Move = Order[Rotation % Order.length];
        if (Move == Summon && level().getEntitiesOfClass(StarMoth.class, getBoundingBox().inflate(48.0)).size() >= 6) {
            Move = Pillars;
        }
        startMove(Move, Target);
        Cooldown = Eclipsed ? 36 : 50;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !dying()) {
            BlockPos Ground = CourtArena.ground((ServerLevel) level(), blockPosition(), 8);
            double Want = (Ground != null ? Ground.getY() : getY()) + Hover + Math.sin(tickCount * 0.04) * 0.2;
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(Motion.x, Motion.y * 0.6 + (Want - getY()) * 0.08, Motion.z);
        }
    }

    @Override
    protected void steer(@Nullable LivingEntity Target) {
        if (Target == null) {
            setDeltaMovement(getDeltaMovement().multiply(0.8, 1.0, 0.8));
            return;
        }
        face(Target);
        Vec3 Centre = Vec3.atBottomCenterOf(Dais);
        Vec3 Way = CourtFx.flat(Target.position().subtract(position()));
        double Distance = distanceTo(Target);
        Vec3 Push;
        if (Distance > farRange()) {
            Push = Way.scale(0.03);
        } else if (Distance < nearRange()) {
            Push = Way.scale(-0.025);
        } else {
            Push = new Vec3(Way.z, 0.0, -Way.x).scale(0.012);
        }
        if (position().distanceTo(new Vec3(Centre.x, getY(), Centre.z)) > CourtArena.floorRadius() - 4) {
            Push = CourtFx.flat(Centre.subtract(position())).scale(0.04);
        }
        Vec3 Motion = getDeltaMovement();
        setDeltaMovement(Motion.x * 0.85 + Push.x, Motion.y, Motion.z * 0.85 + Push.z);
    }

    @Override
    protected void moveTick(int Action, int Elapsed, @Nullable LivingEntity Target) {
        ServerLevel World = (ServerLevel) level();
        setDeltaMovement(getDeltaMovement().multiply(0.7, 1.0, 0.7));
        face(Target);
        switch (Action) {
            case Pillars -> {
                if (Elapsed == 1) {
                    CourtFx.sound(World, eyes(), "moon_idle", 3.0f, 1.2f);
                }
                if (Elapsed < 10 && Elapsed % 3 == 0) {
                    CourtFx.burst(World, CourtFx.Moonlight, eyes().add(0.0, 1.5, 0.0), 8, 0.8, 0.02);
                }
                if (Elapsed == 10) {
                    volley(World, Target, Eclipsed ? 3 : 1);
                }
            }
            case Tide -> tide(World, Elapsed);
            case Summon -> summon(World, Elapsed);
            default -> {
            }
        }
    }

    public void volley(ServerLevel World, @Nullable LivingEntity Target, int Count) {
        List<LivingEntity> Marks = new ArrayList<>();
        if (Target != null) {
            Marks.add(Target);
        }
        for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(Dais).inflate(CourtArena.playerRange()), CourtFx::validTarget)) {
            if (!Marks.contains(Each)) {
                Marks.add(Each);
            }
        }
        for (LivingEntity Mark : Marks) {
            for (int Index = 0; Index < Count; Index++) {
                Vec3 Offset = Index == 0 ? Vec3.ZERO : CourtFx.turn(new Vec3(2.6, 0.0, 0.0), Index * 360.0 / Math.max(1, Count - 1) + random.nextDouble() * 40.0);
                drop(World, Mark.position().add(Offset));
            }
        }
    }

    public void drop(ServerLevel World, Vec3 Near) {
        BlockPos Ground = CourtArena.ground(World, BlockPos.containing(Near), 6);
        Vec3 Spot = Ground != null ? Vec3.atBottomCenterOf(Ground) : Near;
        Falls.add(new Fall(Spot, tickCount + PillarTelegraph));
        CourtFx.sound(World, Spot, "moon_idle", 1.5f, 1.6f);
    }

    public static boolean covered(ServerLevel World, LivingEntity Victim) {
        BlockPos Start = BlockPos.containing(Victim.getX(), Victim.getBoundingBox().maxY + 0.05, Victim.getZ());
        for (int Lift = 0; Lift < 24; Lift++) {
            BlockPos At = Start.above(Lift);
            BlockState Above = World.getBlockState(At);
            if (!Above.is(Blocks.BARRIER) && !Above.getCollisionShape(World, At).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void tickFalls(ServerLevel World) {
        Iterator<Fall> Each = Falls.iterator();
        while (Each.hasNext()) {
            Fall Pillar = Each.next();
            int Left = Pillar.At() - tickCount;
            if (Left > 0) {
                if (Left % 3 == 0) {
                    CourtFx.ring(World, CourtFx.Moonlight, Pillar.Spot(), PillarRadius, 18, 0.08);
                    CourtFx.ring(World, ParticleTypes.END_ROD, Pillar.Spot(), PillarRadius * (Left / (double) PillarTelegraph), 8, 0.1);
                }
                continue;
            }
            Each.remove();
            for (double Lift = 0.0; Lift < 18.0; Lift += 0.6) {
                World.sendParticles(CourtFx.Moonlight, Pillar.Spot().x, Pillar.Spot().y + Lift, Pillar.Spot().z, 2, 0.35, 0.2, 0.35, 0.0);
            }
            World.sendParticles(ParticleTypes.END_ROD, Pillar.Spot().x, Pillar.Spot().y + 6.0, Pillar.Spot().z, 30, 0.3, 6.0, 0.3, 0.02);
            CourtFx.ring(World, ParticleTypes.END_ROD, Pillar.Spot(), PillarRadius + 0.3, 16, 0.1);
            CourtFx.sound(World, Pillar.Spot(), "moon_pillar", 3.0f, 0.9f + random.nextFloat() * 0.2f);
            for (LivingEntity Victim : CourtFx.victimsNear(World, Pillar.Spot(), PillarRadius + 0.6)) {
                double Dx = Victim.getX() - Pillar.Spot().x;
                double Dz = Victim.getZ() - Pillar.Spot().z;
                if (Dx * Dx + Dz * Dz <= PillarRadius * PillarRadius + 0.3 && Victim.getY() - Pillar.Spot().y < 3.0 && !covered(World, Victim)) {
                    CourtFx.strike(World.damageSources().indirectMagic(this, this), Victim, PillarDamage);
                }
            }
        }
    }

    private void tickRays(ServerLevel World) {
        Iterator<Ray> Each = Rays.iterator();
        Vec3 From = eyes();
        while (Each.hasNext()) {
            Ray Beam = Each.next();
            Vec3 Way = Beam.To().subtract(From).normalize();
            Vec3 End = Beam.To().add(Way.scale(3.0));
            int Left = Beam.At() - tickCount;
            if (Left > 0) {
                if (Left % 3 == 0) {
                    CourtFx.line(World, CourtFx.Moonlight, From, End, 1.1, 0.0);
                }
                continue;
            }
            Each.remove();
            CourtFx.line(World, ParticleTypes.END_ROD, From, End, 0.6, 0.02);
            CourtFx.line(World, CourtFx.Silver, From, End, 0.5, 0.05);
            CourtFx.sound(World, From, "moon_full", 2.5f, 1.6f);
            for (LivingEntity Victim : CourtFx.victims(World, new AABB(From, End).inflate(1.5))) {
                if (CourtFx.segmentDistance(Victim.position().add(0.0, Victim.getBbHeight() * 0.5, 0.0), From, End) < 1.2) {
                    CourtFx.strike(World.damageSources().indirectMagic(this, this), Victim, BeamDamage);
                }
            }
        }
    }

    public void moonbeams(ServerLevel World) {
        List<LivingEntity> Marks = new ArrayList<>();
        for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(Dais).inflate(BeamReach), CourtFx::validTarget)) {
            Marks.add(Each);
        }
        if (Marks.isEmpty() && getTarget() != null && CourtFx.validTarget(getTarget())) {
            Marks.add(getTarget());
        }
        Marks.sort((First, Second) -> Double.compare(First.distanceToSqr(this), Second.distanceToSqr(this)));
        for (int Index = 0; Index < Math.min(2, Marks.size()); Index++) {
            Rays.add(new Ray(Marks.get(Index).position().add(0.0, 1.0, 0.0), tickCount + BeamTelegraph));
        }
    }

    private void tide(ServerLevel World, int Elapsed) {
        if (Elapsed == 1) {
            CourtFx.sound(World, position(), "moon_tide", 5.0f, Pushing ? 1.2f : 0.9f);
        }
        if (Elapsed < TideTelegraph && Elapsed % 2 == 0) {
            double Turn = Elapsed * 0.35;
            double Radius = TideRange * (1.0 - Elapsed / (double) TideTelegraph) + 2.0;
            for (int Arm = 0; Arm < 6; Arm++) {
                double Angle = Turn + Arm * Math.PI / 3.0;
                World.sendParticles(CourtFx.Moonlight, getX() + Math.cos(Angle) * Radius, getY() + 0.3, getZ() + Math.sin(Angle) * Radius, 1, 0.1, 0.05, 0.1, 0.0);
            }
        }
        if (Elapsed >= TideTelegraph && Elapsed < TideTelegraph + TidePull) {
            for (LivingEntity Victim : CourtFx.victimsNear(World, position(), TideRange)) {
                Vec3 Way = CourtFx.flat(position().subtract(Victim.position()));
                double Strength = Pushing ? -0.06 : 0.06;
                Victim.setDeltaMovement(Victim.getDeltaMovement().add(Way.x * Strength, 0.0, Way.z * Strength));
                Victim.hurtMarked = true;
            }
            if (Elapsed % 4 == 0) {
                CourtFx.ring(World, CourtFx.Indigo, position(), 6.0 + (Elapsed % 12), 24, 0.2);
            }
        }
        if (Elapsed == TideTelegraph + TidePull) {
            CourtFx.sound(World, position(), "moon_tide", 5.0f, 1.4f);
            CourtFx.ring(World, ParticleTypes.END_ROD, position(), TideBurst, 32, 0.4);
            CourtFx.ring(World, CourtFx.Moonlight, position(), TideBurst * 0.6, 20, 0.6);
            for (LivingEntity Victim : CourtFx.victimsNear(World, position(), TideBurst + 0.4)) {
                if (CourtFx.strike(this, Victim, TideDamage)) {
                    CourtFx.push(Victim, position(), 0.7, 0.3);
                }
            }
            if (Eclipsed) {
                Pushing = !Pushing;
            }
        }
    }

    private void summon(ServerLevel World, int Elapsed) {
        if (Elapsed == 2) {
            CourtFx.sound(World, eyes(), "moon_summon", 4.0f, 1.0f);
        }
        if (Elapsed < 20 && Elapsed % 4 == 0) {
            CourtFx.burst(World, CourtFx.Silver, eyes(), 10, 1.2, 0.02);
        }
        if (Elapsed == 20) {
            int Count = 3 + Math.min(2, Players - 1);
            for (int Index = 0; Index < Count; Index++) {
                double Angle = Index * Math.PI * 2.0 / Count;
                BlockPos Spot = BlockPos.containing(getX() + Math.cos(Angle) * 2.5, getY() + 3.0, getZ() + Math.sin(Angle) * 2.5);
                CourtMinion Made = ScarletCourt.Instance.summon(World, CourtKind.StarMoth, Spot, true);
                if (Made != null && getTarget() != null) {
                    Made.setTarget(getTarget());
                }
            }
        }
    }

    private void tickZones(ServerLevel World) {
        if (--ZoneTimer <= 0) {
            ZoneTimer = Eclipsed ? ZoneEvery * 5 / 8 : ZoneEvery;
            if (Zones.size() < (Eclipsed ? 5 : 3)) {
                Vec3 Spot = Vec3.atBottomCenterOf(CourtArena.scatter(World, Dais, 3.0, Math.max(5.0, CourtArena.floorRadius() * 0.6), random));
                Zones.add(new Zone(Spot, tickCount + ZoneLife));
            }
        }
        Iterator<Zone> Each = Zones.iterator();
        while (Each.hasNext()) {
            Zone Field = Each.next();
            if (Field.Until() < tickCount) {
                Each.remove();
                continue;
            }
            if (tickCount % 5 == 0) {
                CourtFx.ring(World, CourtFx.Moonlight, Field.Centre(), ZoneRadius, 20, 0.1);
                World.sendParticles(ParticleTypes.END_ROD, Field.Centre().x, Field.Centre().y + 0.5, Field.Centre().z, 2, ZoneRadius * 0.4, 0.4, ZoneRadius * 0.4, 0.01);
            }
            if (tickCount % 10 == 0) {
                for (LivingEntity Victim : CourtFx.victimsNear(World, Field.Centre(), ZoneRadius)) {
                    if (Victim.getY() - Field.Centre().y < 4.0) {
                        Victim.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, true, true));
                        Victim.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, false, true, true));
                    }
                }
            }
        }
    }

    @Override
    protected void arenaTick(ServerLevel World) {
        if (dying()) {
            return;
        }
        tickFalls(World);
        tickRays(World);
        tickZones(World);
        if (entering()) {
            return;
        }
        int[] Lengths = lengths();
        CycleTick++;
        if (CycleTick >= Lengths[moon()]) {
            moon((moon() + 1) % 4);
        }
        if (fullMoon() && CycleTick % BeamEvery == 10) {
            moonbeams(World);
        }
        int Every = newMoon() ? PillarEvery / 2 : PillarEvery;
        if (--PillarTimer <= 0) {
            PillarTimer = Every;
            List<Player> Marks = World.getEntitiesOfClass(Player.class, new AABB(Dais).inflate(CourtArena.playerRange()), CourtFx::validTarget);
            if (!Marks.isEmpty()) {
                LivingEntity Mark = Marks.get(random.nextInt(Marks.size()));
                for (int Index = 0; Index < (Eclipsed ? 3 : 1); Index++) {
                    drop(World, Mark.position().add(Index == 0 ? Vec3.ZERO : CourtFx.turn(new Vec3(2.6, 0.0, 0.0), Index * 120.0)));
                }
            } else if (getTarget() != null) {
                drop(World, getTarget().position());
            }
        } else if (PillarTimer > Every) {
            PillarTimer = Every;
        }
    }

    @Override
    protected void shiftTick(int Elapsed) {
        ServerLevel World = (ServerLevel) level();
        setDeltaMovement(getDeltaMovement().multiply(0.5, 1.0, 0.5));
        if (Elapsed == 1) {
            CourtFx.sound(World, eyes(), "moon_wane", 6.0f, 0.6f);
        }
        if (Elapsed % 4 == 0) {
            CourtFx.burst(World, CourtFx.Indigo, eyes(), 24, 2.0, 0.03);
            CourtFx.ring(World, CourtFx.Silver, position().add(0.0, getBbHeight(), 0.0), 2.5, 24, 0.0);
        }
        if (Elapsed == 30) {
            Eclipsed = true;
            Pushing = false;
            moon(Waxing);
            CourtFx.sound(World, eyes(), "moon_full", 6.0f, 0.6f);
            World.sendParticles(ParticleTypes.FLASH, eyes().x, eyes().y, eyes().z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void dyingTick(ServerLevel World, int Elapsed) {
        setDeltaMovement(0.0, 0.01, 0.0);
        entityData.set(FadeData, Mth.clamp(100 - Elapsed * 100 / DeathTicks, 0, 100));
        Vec3 Crown = position().add(0.0, getBbHeight() + 0.4, 0.0);
        if (Elapsed % 3 == 0) {
            CourtFx.burst(World, CourtFx.Silver, position().add(0.0, getBbHeight() * 0.5, 0.0), 10, 1.0, 0.02);
        }
        if (Elapsed == 20 || Elapsed == 40) {
            CourtFx.burst(World, ParticleTypes.END_ROD, Crown, 16, 0.8, 0.06);
            CourtFx.ring(World, CourtFx.Silver, Crown, 1.6, 20, 0.0);
            CourtFx.sound(World, Crown, "moon_wane", 3.0f, Elapsed == 20 ? 1.4f : 1.7f);
        }
        if (Elapsed == 76) {
            CourtFx.burst(World, CourtFx.Moonlight, position().add(0.0, getBbHeight() * 0.5, 0.0), 60, 1.6, 0.04);
        }
    }

    @Override
    protected void onDying(ServerLevel World) {
        Falls.clear();
        Rays.clear();
        Zones.clear();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Eclipsed", Eclipsed);
        Tag.putInt("Moon", moon());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Eclipsed = Tag.getBoolean("Eclipsed");
        entityData.set(MoonData, Mth.clamp(Tag.getInt("Moon"), Waxing, NewMoon));
    }
}
