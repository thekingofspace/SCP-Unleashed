package net.rai.scp_unleashed.z0004;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Z0004Ship extends Mob implements GeoEntity {
    public static final float Width = 6.5f;
    public static final float Height = 5.0f;
    public static final double MaxHealth = 320.0;
    public static final double Armor = 6.0;
    public static final double SailSpeed = 0.13;
    public static final double CrawlSpeed = 0.03;
    public static final float TurnRate = 1.6f;
    public static final double OrbitRadius = 22.0;
    public static final double HomeOrbit = 26.0;
    public static final double TargetRange = 64.0;
    public static final double CannonMinRange = 8.0;
    public static final double CannonRange = 40.0;
    public static final double BroadsideCos = 0.57;
    public static final int ReloadTicks = 90;
    public static final int FuseTicks = 20;
    public static final int Stagger = 3;
    public static final double[] CannonAlong = {3.75, 1.25, -1.25, -3.75};
    public static final double CannonOut = 3.5;
    public static final double CannonUp = 0.75;
    public static final double BallSpeed = 1.5;
    public static final double BallSpread = 1.5;
    public static final int RiseTicks = 100;
    public static final double RiseDepth = 12.0;
    public static final int SinkTicks = 120;
    public static final double SinkDepth = 9.0;
    public static final double DeckLift = 1.9;
    public static final int ReleaseTicks = 24;
    public static final double BarRange = 64.0;
    public static final double HomeLimit = 96.0;
    public static final int AmbientMin = 90;
    public static final int AmbientMax = 200;
    public static final int Rising = 0;
    public static final int Sailing = 1;
    public static final int Sinking = 2;
    public static final int Retreating = 3;
    public static final RawAnimation RiseHold = RawAnimation.begin().thenPlayAndHold("rise");
    public static final RawAnimation SailLoop = RawAnimation.begin().thenLoop("sail");
    public static final RawAnimation SinkHold = RawAnimation.begin().thenPlayAndHold("sink");
    public static final RawAnimation CrewLoop = RawAnimation.begin().thenLoop("crew");
    public static final RawAnimation FireLeft = RawAnimation.begin().thenPlay("fire_left");
    public static final RawAnimation FireRight = RawAnimation.begin().thenPlay("fire_right");
    private static final EntityDataAccessor<Byte> PhaseData = SynchedEntityData.defineId(Z0004Ship.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> VolleyData = SynchedEntityData.defineId(Z0004Ship.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Z0004ShipPart[] Parts;
    private final ServerBossEvent Bar = new ServerBossEvent(net.minecraft.network.chat.Component.translatable("entity.scp_unleashed.z0004_ship"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    @Nullable
    private BlockPos Home;
    @Nullable
    private UUID FightId;
    private int Surface = Integer.MIN_VALUE;
    private double SurfaceY;
    private float Heading;
    private int PhaseTicks;
    private int Reload = ReloadTicks / 2;
    private int Fuse;
    private int FuseSide;
    private int Volley = CannonAlong.length;
    private int VolleyWait;
    private int ShotsFired;
    private int Ambient = AmbientMin;
    @Nullable
    private LivingEntity Aim;
    private boolean Released;

    public Z0004Ship(EntityType<? extends Z0004Ship> Type, Level World) {
        super(Type, World);
        Parts = new Z0004ShipPart[]{new Z0004ShipPart(this, "bow", 4.5f, 4.0f, 8.5, -0.5), new Z0004ShipPart(this, "fore", 5.5f, 4.5f, 4.5, -0.5),
                new Z0004ShipPart(this, "aft", 6.0f, 5.5f, -5.0, -0.5), new Z0004ShipPart(this, "stern", 5.5f, 6.0f, -9.0, -0.5),
                new Z0004ShipPart(this, "rig", 4.0f, 10.0f, 0.0, 4.5)};
        setId(ENTITY_COUNTER.getAndAdd(Parts.length + 1) + 1);
        setNoGravity(true);
        noPhysics = true;
        xpReward = 40;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.ARMOR, Armor).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, TargetRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(PhaseData, (byte) Rising);
        Builder.define(VolleyData, 0);
    }

    public void prepare(Z0004Fight.Spot Spot, @Nullable BlockPos Chest, @Nullable UUID Fight) {
        Home = Chest == null ? null : Chest.immutable();
        FightId = Fight;
        Surface = Spot.Surface();
        SurfaceY = Spot.Position().y;
        Heading = Spot.Yaw();
        moveTo(Spot.Position().x, SurfaceY - RiseDepth, Spot.Position().z, Heading, 0.0f);
        setYBodyRot(Heading);
        setYHeadRot(Heading);
        setPhase(Rising);
        PhaseTicks = 0;
    }

    public void surfaceNow() {
        setPhase(Sailing);
        PhaseTicks = 0;
        setPos(getX(), SurfaceY, getZ());
    }

    public int phase() {
        return entityData.get(PhaseData);
    }

    private void setPhase(int Value) {
        entityData.set(PhaseData, (byte) Value);
    }

    public boolean sinking() {
        return phase() == Sinking || phase() == Retreating || isDeadOrDying();
    }

    public int volleys() {
        return entityData.get(VolleyData);
    }

    public boolean released() {
        return Released;
    }

    public int shotsFired() {
        return ShotsFired;
    }

    @Nullable
    public UUID fightId() {
        return FightId;
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    @Override
    public void setId(int Id) {
        super.setId(Id);
        for (int Index = 0; Index < Parts.length; Index++) {
            Parts[Index].setId(Id + Index + 1);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return Parts;
    }

    public Z0004ShipPart[] parts() {
        return Parts;
    }

    private void placeParts() {
        Vec3 Forward = Z0004Fight.forward(getYRot());
        for (Z0004ShipPart Part : Parts) {
            Vec3 Old = Part.position();
            Part.setPos(getX() + Forward.x * Part.Along, getY() + Part.Lift, getZ() + Forward.z * Part.Along);
            Part.xo = Old.x;
            Part.yo = Old.y;
            Part.zo = Old.z;
            Part.xOld = Old.x;
            Part.yOld = Old.y;
            Part.zOld = Old.z;
        }
    }

    @Override
    public void tick() {
        super.tick();
        placeParts();
        if (level() instanceof ServerLevel World) {
            if (!isDeadOrDying()) {
                step(World);
            }
        } else {
            clientEffects();
        }
    }

    @Override
    public void travel(Vec3 Input) {
    }

    private void step(ServerLevel World) {
        PhaseTicks++;
        int Phase = phase();
        if (Phase == Rising) {
            double Share = Mth.clamp(PhaseTicks / (double) RiseTicks, 0.0, 1.0);
            double Eased = Share * Share * (3.0 - 2.0 * Share);
            setPos(getX(), SurfaceY - RiseDepth * (1.0 - Eased), getZ());
            if (PhaseTicks % 5 == 0) {
                World.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), SurfaceY, getZ(), 30, 6.0, 0.5, 6.0, 0.1);
                World.sendParticles(ParticleTypes.SPLASH, getX(), SurfaceY + 0.2, getZ(), 40, 8.0, 0.3, 8.0, 0.1);
                World.sendParticles(ParticleTypes.CLOUD, getX(), SurfaceY + 1.0, getZ(), 8, 8.0, 1.0, 8.0, 0.01);
            }
            if (PhaseTicks == 1) {
                Z0004Registry.play(World, position(), Z0004Registry.SurfaceLayers, SoundSource.HOSTILE, 4.0f, 0.6f);
            }
            if (PhaseTicks >= RiseTicks) {
                setPhase(Sailing);
                PhaseTicks = 0;
                Z0004Registry.play(World, position(), Z0004Registry.BellLayers, SoundSource.HOSTILE, 3.0f, 0.5f);
            }
        } else if (Phase == Retreating) {
            setPos(getX(), SurfaceY - SinkDepth * Mth.clamp(PhaseTicks / (double) SinkTicks, 0.0, 1.0), getZ());
            if (PhaseTicks % 6 == 0) {
                World.sendParticles(ParticleTypes.BUBBLE, getX(), SurfaceY, getZ(), 30, 6.0, 0.5, 6.0, 0.1);
            }
            if (PhaseTicks >= SinkTicks) {
                discard();
            }
            return;
        } else {
            Aim = pickTarget(World);
            sail(World, Aim);
            cannons(World, Aim);
            ambient(World);
        }
        if (tickCount % 20 == 0) {
            tether(World);
            bar(World);
        }
    }

    private void tether(ServerLevel World) {
        if (Home == null || FightId == null) {
            return;
        }
        if (Z0004Fight.flatDistanceSqr(position(), Vec3.atCenterOf(Home)) > HomeLimit * HomeLimit) {
            retreat();
            return;
        }
        if (!World.isLoaded(Home)) {
            return;
        }
        Z0004ChestBlockEntity Chest = Z0004Fight.chestAt(World, Home);
        if (Chest == null || !Chest.tracks(FightId)) {
            retreat();
        }
    }

    private void bar(ServerLevel World) {
        Bar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0f, 1.0f));
        for (ServerPlayer Viewer : World.players()) {
            boolean Near = Viewer.distanceToSqr(this) < BarRange * BarRange && !sinking();
            if (Near && !Bar.getPlayers().contains(Viewer)) {
                Bar.addPlayer(Viewer);
            } else if (!Near && Bar.getPlayers().contains(Viewer)) {
                Bar.removePlayer(Viewer);
            }
        }
    }

    @Nullable
    private LivingEntity pickTarget(ServerLevel World) {
        LivingEntity Best = null;
        double BestDistance = TargetRange * TargetRange;
        for (Player User : World.players()) {
            if (!Z0004Fight.targetable(User)) {
                continue;
            }
            double Distance = User.distanceToSqr(this);
            if (Distance < BestDistance) {
                Best = User;
                BestDistance = Distance;
            }
        }
        if (Best == null) {
            for (LivingEntity Other : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(TargetRange * 0.5), Z0004Fight::targetable)) {
                double Distance = Other.distanceToSqr(this);
                if (Distance < BestDistance) {
                    Best = Other;
                    BestDistance = Distance;
                }
            }
        }
        return Best;
    }

    private void sail(ServerLevel World, @Nullable LivingEntity Target) {
        Vec3 Here = position();
        Vec3 Centre = Target != null ? Target.position() : Home != null ? Vec3.atCenterOf(Home) : Here;
        double Radius = Target != null ? OrbitRadius : HomeOrbit;
        Vec3 Away = new Vec3(Here.x - Centre.x, 0.0, Here.z - Centre.z);
        double Distance = Away.length();
        Vec3 Radial = Distance < 1.0e-3 ? Z0004Fight.left(Heading) : Away.scale(1.0 / Distance);
        Vec3 Tangent = new Vec3(-Radial.z, 0.0, Radial.x);
        Vec3 Forward = Z0004Fight.forward(Heading);
        if (Tangent.dot(Forward) < 0.0) {
            Tangent = Tangent.scale(-1.0);
        }
        double Correction = Mth.clamp((Distance - Radius) / 10.0, -1.0, 1.0);
        Vec3 Wanted = Tangent.subtract(Radial.scale(Correction));
        if (Home != null && Z0004Fight.flatDistanceSqr(Here, Vec3.atCenterOf(Home)) > Z0004Fight.Leash * Z0004Fight.Leash) {
            Wanted = new Vec3(Home.getX() + 0.5 - Here.x, 0.0, Home.getZ() + 0.5 - Here.z);
        }
        float WantedYaw = Z0004Fight.yawOf(Wanted);
        double Speed = SailSpeed;
        if (!clearAhead(World, Heading)) {
            Speed = CrawlSpeed;
            WantedYaw = Heading + 90.0f;
            for (float Offset : new float[]{30.0f, -30.0f, 60.0f, -60.0f, 100.0f, -100.0f}) {
                if (clearAhead(World, Heading + Offset)) {
                    WantedYaw = Heading + Offset;
                    break;
                }
            }
        }
        Heading = Mth.approachDegrees(Heading, WantedYaw, TurnRate);
        Vec3 Step = Z0004Fight.forward(Heading).scale(Speed);
        Vec3 Next = new Vec3(Here.x + Step.x, SurfaceY, Here.z + Step.z);
        if (Z0004Fight.deepAt(World, Next.x, Next.z, Surface)) {
            setPos(Next.x, Next.y, Next.z);
        }
        setYRot(Heading);
        setYBodyRot(Heading);
        setYHeadRot(Heading);
    }

    private boolean clearAhead(ServerLevel World, float Yaw) {
        Vec3 Forward = Z0004Fight.forward(Yaw);
        Vec3 Left = Z0004Fight.left(Yaw);
        Vec3 Bow = position().add(Forward.scale(Z0004Fight.HullHalfLength + 3.0));
        return Z0004Fight.deepAt(World, Bow.x, Bow.z, Surface) && Z0004Fight.deepAt(World, Bow.x + Left.x * Z0004Fight.HullHalfBeam, Bow.z + Left.z * Z0004Fight.HullHalfBeam, Surface)
                && Z0004Fight.deepAt(World, Bow.x - Left.x * Z0004Fight.HullHalfBeam, Bow.z - Left.z * Z0004Fight.HullHalfBeam, Surface);
    }

    private void cannons(ServerLevel World, @Nullable LivingEntity Target) {
        if (Fuse > 0) {
            Fuse--;
            if (Fuse % 4 == 0) {
                for (int Index = 0; Index < CannonAlong.length; Index++) {
                    Vec3 Port = port(Index, FuseSide);
                    World.sendParticles(ParticleTypes.SMOKE, Port.x, Port.y + 0.3, Port.z, 3, 0.1, 0.1, 0.1, 0.01);
                    World.sendParticles(ParticleTypes.SMALL_FLAME, Port.x, Port.y + 0.2, Port.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            if (Fuse == 0) {
                triggerAnim("guns", FuseSide > 0 ? "fire_left" : "fire_right");
                entityData.set(VolleyData, entityData.get(VolleyData) + 1);
                Volley = 0;
                VolleyWait = 0;
            }
            return;
        }
        if (Volley < CannonAlong.length) {
            if (VolleyWait-- <= 0) {
                fire(World, Target, Volley);
                Volley++;
                VolleyWait = Stagger - 1;
            }
            return;
        }
        if (Reload > 0) {
            Reload--;
            return;
        }
        if (Target == null || !Target.isAlive()) {
            return;
        }
        Vec3 Way = new Vec3(Target.getX() - getX(), 0.0, Target.getZ() - getZ());
        double Distance = Way.length();
        if (Distance < CannonMinRange || Distance > CannonRange) {
            return;
        }
        double Dot = Way.scale(1.0 / Distance).dot(Z0004Fight.left(getYRot()));
        if (Math.abs(Dot) < BroadsideCos) {
            return;
        }
        FuseSide = Dot > 0.0 ? 1 : -1;
        Fuse = FuseTicks;
        Reload = ReloadTicks;
        Vec3 Middle = port(1, FuseSide);
        World.playSound(null, Middle.x, Middle.y, Middle.z, Z0004Registry.Fuse.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
    }

    public Vec3 port(int Index, int Side) {
        Vec3 Forward = Z0004Fight.forward(getYRot());
        Vec3 Left = Z0004Fight.left(getYRot());
        return position().add(Forward.scale(CannonAlong[Index])).add(Left.scale(CannonOut * Side)).add(0.0, CannonUp, 0.0);
    }

    private void fire(ServerLevel World, @Nullable LivingEntity Target, int Index) {
        Vec3 Port = port(Index, FuseSide);
        Vec3 Aim0 = Target != null && Target.isAlive() ? Target.position().add(Target.getDeltaMovement().scale(10.0))
                : Port.add(Z0004Fight.left(getYRot()).scale(FuseSide * 20.0));
        Aim0 = Aim0.add((random.nextDouble() - 0.5) * 2.0 * BallSpread, 0.0, (random.nextDouble() - 0.5) * 2.0 * BallSpread);
        Vec3 Flat = new Vec3(Aim0.x - Port.x, 0.0, Aim0.z - Port.z);
        double Distance = Math.max(1.0, Flat.length());
        double Time = Distance / BallSpeed;
        double Rise = (Aim0.y - Port.y + 0.5 * Z0004Cannonball.Gravity * Time * Time) / Time;
        Vec3 Velocity = Flat.scale(BallSpeed / Distance).add(0.0, Mth.clamp(Rise, -1.0, 1.6), 0.0);
        Z0004Cannonball Ball = new Z0004Cannonball(World, this, Port, Velocity);
        World.addFreshEntity(Ball);
        ShotsFired++;
        World.sendParticles(ParticleTypes.FLAME, Port.x, Port.y, Port.z, 8, 0.2, 0.2, 0.2, 0.05);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Port.x, Port.y + 0.2, Port.z, 10, 0.4, 0.3, 0.4, 0.03);
        World.sendParticles(ParticleTypes.POOF, Port.x, Port.y, Port.z, 6, 0.3, 0.3, 0.3, 0.05);
        Z0004Registry.play(World, Port, Z0004Registry.CannonLayers, SoundSource.HOSTILE, 4.0f, 0.75f + random.nextFloat() * 0.15f);
    }

    private void ambient(ServerLevel World) {
        if (--Ambient > 0) {
            return;
        }
        Ambient = AmbientMin + random.nextInt(AmbientMax - AmbientMin);
        Z0004Registry.play(World, position().add(0.0, 2.0, 0.0), Z0004Registry.CreakLayers, SoundSource.HOSTILE, 2.0f, 0.6f + random.nextFloat() * 0.2f);
        if (random.nextInt(3) == 0) {
            World.playSound(null, getX(), getY() + 8.0, getZ(), Z0004Registry.Sails.get(), SoundSource.HOSTILE, 1.6f, 0.6f + random.nextFloat() * 0.2f);
        }
        if (random.nextInt(6) == 0) {
            Z0004Registry.play(World, position().add(0.0, 4.0, 0.0), Z0004Registry.BellLayers, SoundSource.HOSTILE, 2.5f, 0.5f);
        }
    }

    private void clientEffects() {
        if (phase() == Sailing && random.nextInt(3) == 0) {
            Vec3 Bow = position().add(Z0004Fight.forward(getYRot()).scale(10.5));
            level().addParticle(ParticleTypes.SPLASH, Bow.x + (random.nextDouble() - 0.5) * 2.0, getY() + 0.1, Bow.z + (random.nextDouble() - 0.5) * 2.0, 0.0, 0.05, 0.0);
        }
    }

    public boolean hurtPart(Z0004ShipPart Part, DamageSource Source, float Amount) {
        return hurt(Source, Amount);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide || isInvulnerableTo(Source) || phase() != Sailing || isDeadOrDying()) {
            return false;
        }
        if (Z0004Fight.fleet(Source.getEntity()) || Z0004Fight.fleet(Source.getDirectEntity())) {
            return false;
        }
        if (Source.is(DamageTypeTags.IS_DROWNING) || Source.is(DamageTypeTags.IS_FALL) || Source.is(DamageTypes.IN_WALL) || Source.is(DamageTypes.CRAMMING)
                || Source.is(DamageTypes.FELL_OUT_OF_WORLD) && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (Source.getEntity() instanceof Player User && level() instanceof ServerLevel World && Home != null) {
            Z0004ChestBlockEntity Chest = Z0004Fight.chestAt(World, Home);
            if (Chest != null && Chest.tracks(FightId)) {
                Chest.join(User.getUUID());
            }
        }
        boolean Hit = super.hurt(Source, Amount);
        if (Hit && level() instanceof ServerLevel World) {
            Vec3 Where = Source.getSourcePosition() != null ? Source.getSourcePosition() : position().add(0.0, 2.0, 0.0);
            World.playSound(null, getX(), getY() + 1.5, getZ(), Z0004Registry.ShipHurt.get(), SoundSource.HOSTILE, 2.0f, 0.7f + random.nextFloat() * 0.2f);
            World.sendParticles(ParticleTypes.CRIT, Where.x, Mth.clamp(Where.y, getY(), getY() + 4.0), Where.z, 10, 0.5, 0.5, 0.5, 0.1);
        }
        return Hit;
    }

    @Override
    public void die(DamageSource Source) {
        boolean First = !isRemoved() && !dead;
        super.die(Source);
        if (First && level() instanceof ServerLevel World) {
            setPhase(Sinking);
            Bar.removeAllPlayers();
            Z0004Registry.play(World, position(), Z0004Registry.BreakLayers, SoundSource.HOSTILE, 4.0f, 0.7f);
            Z0004Registry.play(World, position(), Z0004Registry.SinkLayers, SoundSource.HOSTILE, 4.0f, 0.6f);
            World.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.5, getZ(), 6, 5.0, 1.0, 5.0, 0.0);
        }
    }

    public void releaseCrew() {
        if (Released || !(level() instanceof ServerLevel World)) {
            return;
        }
        Released = true;
        List<UUID> Spawned = spawnCrew(World);
        if (Home != null) {
            Z0004ChestBlockEntity Chest = Z0004Fight.chestAt(World, Home);
            if (Chest != null && Chest.tracks(FightId)) {
                Chest.shipDestroyed(Spawned);
            }
        }
    }

    public List<UUID> spawnCrew(ServerLevel World) {
        List<UUID> Spawned = new ArrayList<>();
        Vec3 Forward = Z0004Fight.forward(getYRot());
        Vec3 Left = Z0004Fight.left(getYRot());
        for (Z0004DeckSlot Slot : Z0004DeckSlot.All) {
            Z0004Pirate Member = Slot.captain() ? Z0004Registry.Captain.get().create(World) : Z0004Registry.Crew.get().create(World);
            if (Member == null) {
                continue;
            }
            Vec3 Spot = position().add(Forward.scale(Slot.Forward())).add(Left.scale(Slot.Left())).add(0.0, Slot.Up(), 0.0);
            double Side = Slot.Left() >= 0.0 ? 1.0 : -1.0;
            Member.moveTo(Spot.x, Spot.y, Spot.z, getYRot(), 0.0f);
            if (!Slot.captain()) {
                Member.setVariant(Slot.Variant());
            }
            Member.enlist(Home, FightId);
            Member.setDeltaMovement(Left.scale(0.35 * Side).add(0.0, 0.35, 0.0));
            World.addFreshEntity(Member);
            Spawned.add(Member.getUUID());
            World.sendParticles(ParticleTypes.SPLASH, Spot.x, getY() + 0.3, Spot.z, 20, 0.6, 0.3, 0.6, 0.2);
        }
        World.playSound(null, getX(), getY(), getZ(), Z0004Registry.Dive.get(), SoundSource.HOSTILE, 2.0f, 0.7f);
        return Spawned;
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= ReleaseTicks) {
            releaseCrew();
        }
        if (level() instanceof ServerLevel World) {
            setPos(getX(), SurfaceY - SinkDepth * Mth.clamp(deathTime / (double) SinkTicks, 0.0, 1.0), getZ());
            if (deathTime % 4 == 0) {
                World.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), SurfaceY, getZ(), 25, 6.0, 0.4, 6.0, 0.1);
                World.sendParticles(ParticleTypes.SPLASH, getX(), SurfaceY + 0.2, getZ(), 20, 7.0, 0.2, 7.0, 0.1);
            }
            if (deathTime >= SinkTicks) {
                remove(RemovalReason.KILLED);
            }
        }
    }

    public void retreat() {
        if (phase() == Retreating || isDeadOrDying()) {
            return;
        }
        setPhase(Retreating);
        PhaseTicks = 0;
        Bar.removeAllPlayers();
        if (level() instanceof ServerLevel World) {
            Z0004Registry.play(World, position(), Z0004Registry.SinkLayers, SoundSource.HOSTILE, 3.0f, 0.7f);
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        Bar.removeAllPlayers();
        super.remove(Reason);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return ItemStack.EMPTY;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(14.0, 16.0, 14.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 256.0 * 256.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
        if (FightId != null) {
            Tag.putUUID("Fight", FightId);
        }
        Tag.putInt("Surface", Surface);
        Tag.putDouble("SurfaceY", SurfaceY);
        Tag.putFloat("Heading", Heading);
        Tag.putInt("Phase", phase());
        Tag.putInt("PhaseTicks", PhaseTicks);
        Tag.putInt("Reload", Reload);
        Tag.putInt("Shots", ShotsFired);
        Tag.putBoolean("Released", Released);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
        FightId = Tag.hasUUID("Fight") ? Tag.getUUID("Fight") : null;
        Surface = Tag.contains("Surface") ? Tag.getInt("Surface") : Mth.floor(getY());
        SurfaceY = Tag.contains("SurfaceY") ? Tag.getDouble("SurfaceY") : getY();
        Heading = Tag.getFloat("Heading");
        setPhase(Mth.clamp(Tag.getInt("Phase"), Rising, Retreating));
        PhaseTicks = Tag.getInt("PhaseTicks");
        Reload = Tag.getInt("Reload");
        ShotsFired = Tag.getInt("Shots");
        Released = Tag.getBoolean("Released");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "hull", 10, this::hullState));
        Controllers.add(new AnimationController<>(this, "crew", 0, State -> State.setAndContinue(CrewLoop)));
        Controllers.add(new AnimationController<>(this, "guns", 0, State -> PlayState.STOP).triggerableAnim("fire_left", FireLeft).triggerableAnim("fire_right", FireRight));
    }

    private PlayState hullState(AnimationState<Z0004Ship> State) {
        if (sinking()) {
            return State.setAndContinue(SinkHold);
        }
        if (phase() == Rising) {
            return State.setAndContinue(RiseHold);
        }
        return State.setAndContinue(SailLoop);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
