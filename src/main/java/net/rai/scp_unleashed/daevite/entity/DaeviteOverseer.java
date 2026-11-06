package net.rai.scp_unleashed.daevite.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.daevite.DaeviteCollapse;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;
import net.rai.scp_unleashed.daevite.DaeviteTargets;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class DaeviteOverseer extends DaeviteMob {
    public static final float Width = 0.6f;
    public static final float Height = 1.95f;
    public static final float EyeHeight = 1.7f;
    public static final double MaxHealth = 34.0;
    public static final double Armour = 4.0;
    public static final double Steadiness = 0.4;
    public static final double Speed = 0.3;
    public static final double Follow = 40.0;
    public static final double Hover = 0.55;
    public static final double ChannelLift = 1.4;
    public static final double Bob = 0.08;
    public static final float BoltDamage = 6.0f;
    public static final float BoltSpeed = 0.9f;
    public static final double BoltMin = 3.5;
    public static final double BoltMax = 24.0;
    public static final int BoltCooldown = 40;
    public static final int BoltJitter = 30;
    public static final float BurstDamage = 5.0f;
    public static final double BurstRadius = 4.0;
    public static final int BurstCooldown = 120;
    public static final double BindMin = 5.0;
    public static final double BindMax = 18.0;
    public static final int BindCooldown = 180;
    public static final int BindJitter = 60;
    public static final double HealRange = 14.0;
    public static final float HealAmount = 2.0f;
    public static final int HealPulse = 5;
    public static final int HealCooldown = 90;
    public static final float HealThreshold = 0.75f;
    public static final double EvadeRange = 3.2;
    public static final int EvadeCooldown = 70;
    public static final double EvadePace = 0.75;
    public static final double KeepNear = 8.0;
    public static final double KeepFar = 14.0;
    public static final int Experience = 15;
    public static final byte MoveBolt = 1;
    public static final byte MoveBurst = 2;
    public static final byte MoveBind = 3;
    public static final byte MoveHeal = 4;
    public static final byte MoveChannel = 5;
    public static final byte MoveEvade = 6;
    public static final String[] Clips = {"", "cast_bolt", "cast_burst", "cast_bind", "heal", "channel", "evade"};
    public static final int[] Length = {0, 16, 14, 20, 30, 0, 8};
    public static final int[] Hit = {0, 9, 7, 11, 0, 0, 0};
    public static final RawAnimation HoverPlay = RawAnimation.begin().thenLoop("hover");
    public static final RawAnimation GlidePlay = RawAnimation.begin().thenLoop("glide");
    public static final RawAnimation[] Plays = new RawAnimation[Clips.length];

    static {
        for (int Index = 1; Index < Clips.length; Index++) {
            Plays[Index] = Index == MoveHeal || Index == MoveChannel ? RawAnimation.begin().thenLoop(Clips[Index]) : RawAnimation.begin().thenPlayAndHold(Clips[Index]);
        }
    }

    private int BoltWait = 20;
    private int BurstWait;
    private int BindWait = 40;
    private int HealWait = 20;
    private int EvadeWait;
    private int HealTarget = -1;
    private Vec3 EvadeDir = Vec3.ZERO;
    @Nullable
    private Vec3 ChannelPoint;
    @Nullable
    private Vec3 ChannelAltar;
    private float Glide;

    public DaeviteOverseer(EntityType<? extends DaeviteOverseer> Type, Level World) {
        super(Type, World);
        setNoGravity(true);
        moveControl = new DaeviteHoverControl(this);
        xpReward = Experience;
        setPathfindingMalus(PathType.WATER, 0.0f);
        setPathfindingMalus(PathType.LAVA, 8.0f);
        setPathfindingMalus(PathType.DANGER_FIRE, 0.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.KNOCKBACK_RESISTANCE, Steadiness)
                .add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.ATTACK_DAMAGE, BoltDamage);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new DaeviteHoverNavigation(this, World);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new DaeviteGoals.Channel(this));
        goalSelector.addGoal(1, new DaeviteGoals.Fight(this));
        goalSelector.addGoal(2, new DaeviteGoals.Patrol(this));
        goalSelector.addGoal(3, new DaeviteGoals.ReturnHome(this));
        goalSelector.addGoal(5, new DaeviteGoals.Post(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public String clipFor(byte Move) {
        return Move > 0 && Move < Clips.length ? Clips[Move] : "";
    }

    @Override
    public boolean loopingMove() {
        byte Current = move();
        return Current == MoveHeal || Current == MoveChannel;
    }

    @Override
    protected boolean seesThroughRobes() {
        return true;
    }

    @Override
    public String roleName() {
        return "overseer";
    }

    @Override
    protected RawAnimation liveBody() {
        return Glide > 0.5f ? GlidePlay : HoverPlay;
    }

    @Override
    protected RawAnimation movePlay(byte Move) {
        return Move > 0 && Move < Plays.length ? Plays[Move] : null;
    }

    public float glide() {
        return Glide;
    }

    @Nullable
    public Vec3 channelPoint() {
        return ChannelPoint;
    }

    @Nullable
    public Vec3 channelAltar() {
        return ChannelAltar;
    }

    public void assignChannel(Vec3 Point, Vec3 Altar) {
        ChannelPoint = Point;
        ChannelAltar = Altar;
        clearPatrol();
        setTarget(null);
    }

    public boolean channeling() {
        return moving(MoveChannel);
    }

    public void holdChannel() {
        if (!moving(MoveChannel)) {
            startMove(MoveChannel);
        }
    }

    public void leaveChannelPose() {
        if (moving(MoveChannel)) {
            endMove();
        }
    }

    public void stopChannel() {
        ChannelPoint = null;
        ChannelAltar = null;
        leaveChannelPose();
    }

    public boolean mending() {
        return moving(MoveHeal);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            float Want = Dx * Dx + Dz * Dz > 0.0012 ? 1.0f : 0.0f;
            Glide += (Want - Glide) * 0.2f;
            if (isAlive() && tickCount % 4 == 0) {
                level().addParticle(DaeviteCollapse.Cinder, getRandomX(0.4), getY() + 0.05, getRandomZ(0.4), 0.0, -0.01, 0.0);
            }
        }
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide && isAlive()) {
            hover();
        }
        super.aiStep();
    }

    private void hover() {
        Vec3 Motion = getDeltaMovement();
        double Floor = floorAt(getX(), getZ());
        if (Motion.horizontalDistanceSqr() > 1.0e-4) {
            Vec3 Ahead = Motion.multiply(1.0, 0.0, 1.0).normalize().scale(0.9);
            Floor = Math.max(Floor, floorAt(getX() + Ahead.x, getZ() + Ahead.z));
        }
        double Lift = channeling() ? ChannelLift : Hover;
        double Want = Floor + Lift + Math.sin((tickCount + getId() * 7) * 0.08) * Bob;
        BlockPos Above = BlockPos.containing(getX(), Want + getBbHeight(), getZ());
        if (!level().getBlockState(Above).getCollisionShape(level(), Above).isEmpty()) {
            double Room = Above.getY() - Floor - getBbHeight() - 0.02;
            Want = Floor + Math.max(0.0, Math.min(Lift, Room));
        }
        double Rise = Mth.clamp((Want - getY()) * 0.2, -0.16, 0.14);
        setDeltaMovement(Motion.x, Rise, Motion.z);
    }

    private double floorAt(double X, double Z) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(Mth.floor(X), Mth.floor(getY() + 0.6), Mth.floor(Z));
        for (int Step = 0; Step < 14; Step++) {
            BlockState State = level().getBlockState(Cursor);
            if (!State.getFluidState().isEmpty()) {
                return Cursor.getY() + 0.9;
            }
            VoxelShape Shape = State.getCollisionShape(level(), Cursor);
            if (!Shape.isEmpty()) {
                return Cursor.getY() + Shape.max(Direction.Axis.Y);
            }
            Cursor.move(Direction.DOWN);
        }
        return getY() - 4.0;
    }

    @Override
    public void travel(Vec3 Input) {
        if (isDeadOrDying()) {
            super.travel(Input);
            return;
        }
        if (isControlledByLocalInstance()) {
            move(MoverType.SELF, getDeltaMovement());
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(Motion.x * 0.7, Motion.y * 0.6, Motion.z * 0.7);
        }
        calculateEntityAnimation(false);
    }

    @Override
    public boolean causeFallDamage(float Distance, float Scale, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Height, boolean Grounded, BlockState State, BlockPos Pos) {
        resetFallDistance();
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void fight(DaeviteGoals.Fight Pursuit, LivingEntity Prey) {
        getLookControl().setLookAt(Prey, 40.0f, 40.0f);
        byte Current = move();
        if (Current == MoveBolt || Current == MoveBind || Current == MoveBurst) {
            getNavigation().stop();
            turnTo(Prey, 25.0f);
            return;
        }
        if (Current == MoveEvade) {
            return;
        }
        double Gap = distanceTo(Prey);
        boolean Sees = getSensing().hasLineOfSight(Prey);
        if (Gap < EvadeRange && EvadeWait <= 0) {
            getNavigation().stop();
            if (Current == MoveHeal) {
                endMove();
            }
            if (BurstWait <= 0) {
                startMove(MoveBurst);
            } else {
                evadeFrom(Prey.position());
            }
            return;
        }
        if (Current == MoveHeal) {
            return;
        }
        if (Gap < KeepNear) {
            Vec3 Away = position().subtract(Prey.position()).multiply(1.0, 0.0, 1.0);
            Away = Away.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0.0f, getYRot() + 180.0f) : Away.normalize();
            Pursuit.steer(position().add(Away.scale(6.0)), 1.25);
        } else if (Gap > KeepFar || !Sees) {
            Pursuit.chase(Prey, 1.0);
        } else {
            getNavigation().stop();
        }
        if (!Sees) {
            return;
        }
        if (Gap >= BindMin && Gap <= BindMax && BindWait <= 0 && random.nextFloat() < 0.5f) {
            getNavigation().stop();
            startMove(MoveBind);
        } else if (Gap >= BoltMin && Gap <= BoltMax && BoltWait <= 0) {
            getNavigation().stop();
            startMove(MoveBolt);
        }
    }

    private void evadeFrom(Vec3 Threat) {
        Vec3 Away = position().subtract(Threat).multiply(1.0, 0.0, 1.0);
        if (Away.lengthSqr() < 1.0e-4) {
            Away = Vec3.directionFromRotation(0.0f, getYRot() + 180.0f);
        }
        float Swerve = (random.nextFloat() - 0.5f) * 1.2f;
        Away = Away.normalize().yRot(Swerve);
        EvadeDir = Away;
        EvadeWait = EvadeCooldown;
        getNavigation().stop();
        startMove(MoveEvade);
        playSound(DaeviteRegistry.Evade.get(), 1.0f, 0.9f + random.nextFloat() * 0.2f);
    }

    @Override
    protected void serverCombat(ServerLevel World) {
        BoltWait = Math.max(0, BoltWait - 1);
        BurstWait = Math.max(0, BurstWait - 1);
        BindWait = Math.max(0, BindWait - 1);
        HealWait = Math.max(0, HealWait - 1);
        EvadeWait = Math.max(0, EvadeWait - 1);
        byte Current = move();
        LivingEntity Prey = getTarget();
        if (Current == MoveNone) {
            if (HealWait <= 0 && tickCount % 10 == 0 && ChannelPoint == null && (Prey == null || distanceTo(Prey) > 6.0)) {
                LivingEntity Ally = woundedAlly(World);
                if (Ally != null) {
                    HealTarget = Ally.getId();
                    startMove(MoveHeal);
                    playSound(DaeviteRegistry.Heal.get(), 1.0f, 0.8f);
                    DaeviteCircle.mending(World, Ally);
                }
            }
            return;
        }
        int Time = moveTicks();
        switch (Current) {
            case MoveBolt -> {
                if (Time == 1) {
                    playSound(DaeviteRegistry.Cast.get(), 1.0f, 1.1f + random.nextFloat() * 0.1f);
                }
                if (Time == Hit[Current]) {
                    fireBolt(World, Prey);
                }
                if (Time >= Length[Current]) {
                    endMove();
                    BoltWait = BoltCooldown + random.nextInt(BoltJitter + 1);
                }
            }
            case MoveBurst -> {
                if (Time == 1) {
                    playSound(DaeviteRegistry.Cast.get(), 1.0f, 0.75f);
                }
                if (Time < Hit[Current] && Time % 2 == 0) {
                    World.sendParticles(DaeviteCollapse.Cinder, getX(), getY() + 1.1, getZ(), 6, 0.6, 0.5, 0.6, -0.05);
                }
                if (Time == Hit[Current]) {
                    burst(World);
                }
                if (Time >= Length[Current]) {
                    BurstWait = BurstCooldown;
                    if (Prey != null) {
                        evadeFrom(Prey.position());
                    } else {
                        endMove();
                    }
                }
            }
            case MoveBind -> {
                if (Time == 1) {
                    playSound(DaeviteRegistry.Cast.get(), 1.0f, 0.9f);
                }
                if (Time == Hit[Current] && Prey != null && isPrey(Prey)) {
                    DaeviteCircle.binding(World, this, Prey);
                }
                if (Time >= Length[Current]) {
                    endMove();
                    BindWait = BindCooldown + random.nextInt(BindJitter + 1);
                    BoltWait = Math.max(BoltWait, 12);
                }
            }
            case MoveHeal -> tickHeal(World, Time);
            case MoveEvade -> {
                Vec3 Motion = getDeltaMovement();
                setDeltaMovement(EvadeDir.x * EvadePace, Motion.y, EvadeDir.z * EvadePace);
                hasImpulse = true;
                World.sendParticles(DaeviteCollapse.Cinder, getX(), getY() + 0.8, getZ(), 3, 0.2, 0.4, 0.2, 0.01);
                if (Time >= Length[Current]) {
                    endMove();
                }
            }
            case MoveChannel -> {
                if (Time % 3 == 0 && ChannelAltar != null) {
                    beam(World, chest(), ChannelAltar.add(0.0, 1.2, 0.0), 10);
                }
            }
            default -> endMove();
        }
    }

    @Nullable
    private LivingEntity woundedAlly(ServerLevel World) {
        LivingEntity Best = null;
        float BestShare = HealThreshold;
        List<LivingEntity> Found = World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HealRange),
                Body -> Body != this && Body.isAlive() && DaeviteTargets.isDaevite(Body) && !(Body instanceof DaeviteMob Kin && Kin.doomed()));
        for (LivingEntity Body : Found) {
            float Share = Body.getHealth() / Body.getMaxHealth();
            if (Share < BestShare && hasLineOfSight(Body)) {
                BestShare = Share;
                Best = Body;
            }
        }
        return Best;
    }

    private void tickHeal(ServerLevel World, int Time) {
        getNavigation().stop();
        Entity Found = World.getEntity(HealTarget);
        if (!(Found instanceof LivingEntity Ally) || !Ally.isAlive() || distanceTo(Ally) > HealRange + 4.0 || Ally.getHealth() >= Ally.getMaxHealth()) {
            endMove();
            HealWait = HealCooldown / 2;
            return;
        }
        getLookControl().setLookAt(Ally, 30.0f, 30.0f);
        turnTo(Ally, 15.0f);
        if (Time % 2 == 0) {
            beam(World, chest(), Ally.position().add(0.0, Ally.getBbHeight() * 0.55, 0.0), 8);
        }
        if (Time % HealPulse == 0) {
            Ally.heal(HealAmount);
            World.sendParticles(ParticleTypes.HEART, Ally.getX(), Ally.getY() + Ally.getBbHeight() + 0.2, Ally.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            World.sendParticles(DaeviteCollapse.Mend, Ally.getX(), Ally.getY() + Ally.getBbHeight() * 0.5, Ally.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
        }
        if (Time >= Length[MoveHeal]) {
            endMove();
            HealWait = HealCooldown;
        }
    }

    public Vec3 chest() {
        return position().add(0.0, getBbHeight() * 0.72, 0.0);
    }

    public Vec3 hand() {
        Vec3 Side = Vec3.directionFromRotation(0.0f, getYRot() + 90.0f).scale(-0.38);
        Vec3 Front = Vec3.directionFromRotation(0.0f, getYRot()).scale(0.45);
        return position().add(Side).add(Front).add(0.0, getBbHeight() * 0.68, 0.0);
    }

    public static void beam(ServerLevel World, Vec3 From, Vec3 To, int Count) {
        Vec3 Span = To.subtract(From);
        for (int Index = 0; Index <= Count; Index++) {
            Vec3 At = From.add(Span.scale(Index / (double) Count));
            World.sendParticles(DaeviteCollapse.Beam, At.x, At.y, At.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void fireBolt(ServerLevel World, @Nullable LivingEntity Prey) {
        if (Prey == null || !isPrey(Prey)) {
            return;
        }
        Vec3 From = hand();
        DaeviteBolt Bolt = new DaeviteBolt(World, this, From, Prey, BoltDamage);
        Vec3 Aim = Prey.position().add(0.0, Prey.getBbHeight() * 0.55, 0.0).subtract(From);
        float Spread = switch (World.getDifficulty()) {
            case HARD -> 1.0f;
            case NORMAL -> 2.5f;
            default -> 4.0f;
        };
        Bolt.shoot(Aim.x, Aim.y, Aim.z, BoltSpeed, Spread);
        World.addFreshEntity(Bolt);
        playSound(DaeviteRegistry.BoltFire.get(), 1.0f, 0.9f + random.nextFloat() * 0.2f);
    }

    private void burst(ServerLevel World) {
        playSound(DaeviteRegistry.Burst.get(), 1.4f, 0.85f + random.nextFloat() * 0.1f);
        for (int Step = 0; Step < 24; Step++) {
            double Angle = Step / 24.0 * Mth.TWO_PI;
            World.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(Angle) * 1.2, getY() + 0.9, getZ() + Math.sin(Angle) * 1.2, 1, 0.0, 0.0, 0.0, 0.0);
            World.sendParticles(DaeviteCollapse.Cinder, getX() + Math.cos(Angle) * 2.6, getY() + 0.7, getZ() + Math.sin(Angle) * 2.6, 2, 0.2, 0.2, 0.2, 0.05);
        }
        World.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0, getZ(), 6, 0.4, 0.3, 0.4, 0.0);
        DamageSource Source = damageSources().source(DaeviteRegistry.BurstDamage, this);
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BurstRadius), this::isPrey)) {
            if (distanceTo(Body) > BurstRadius) {
                continue;
            }
            if (Body.hurt(Source, BurstDamage)) {
                Body.igniteForSeconds(3.0f);
                Vec3 Away = Body.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (Away.lengthSqr() > 1.0e-4) {
                    Body.knockback(1.1, -Away.x, -Away.z);
                    Body.hurtMarked = true;
                }
            }
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return DaeviteRegistry.OverseerIdle.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return DaeviteRegistry.OverseerHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DaeviteRegistry.OverseerDeath.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }
}
