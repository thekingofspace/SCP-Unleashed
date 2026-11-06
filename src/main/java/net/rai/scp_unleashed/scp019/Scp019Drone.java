package net.rai.scp_unleashed.scp019;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp019Drone extends Scp019Creature {
    public static final double Speed = 0.25;
    public static final double SearchSpeed = 0.8;
    public static final double RunSpeed = 1.3;
    public static final double SightRange = 10.0;
    public static final int ScanTicks = 5;
    public static final int EmergeTicks = 32;
    public static final int FormTicks = 24;
    public static final int EnterTicks = 24;
    public static final int SquealTicks = 16;
    public static final int MinShare = 3;
    public static final int MaxShare = 6;
    public static final int IgnoreTicks = 200;
    public static final double ReportReach = 2.2;
    public static final float SquealVolume = 2.4f;
    public static final float ChirpVolume = 1.1f;
    public static final int HideGiveUpTicks = 200;
    public static final Scp019Gait.Shape DroneGait = new Scp019Gait.Shape(
            new Scp019Gait.Profile(6.0, 0.0868, 0.5, 2.6, new double[]{0.969, 0.469}, new float[]{1.0f, 1.0f}),
            new Scp019Gait.Profile(5.0, 0.2143, 0.6, 2.2, new double[]{0.968, 0.468}, new float[]{1.0f, 1.0f}),
            0.18, 0.14, 1, Scp019Registry.DroneSkitter, Scp019Registry.DronePatter, 0.22f, 0.14f, 1.0f, 14.0, true);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SquealPlay = RawAnimation.begin().thenPlayAndHold("squeal");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation FormPlay = RawAnimation.begin().thenPlayAndHold("form");
    private static final RawAnimation EnterPlay = RawAnimation.begin().thenPlayAndHold("enter");

    @Nullable
    private UUID Quarry;
    @Nullable
    private Vec3 QuarrySeen;
    private boolean Reporting;
    private boolean Returning;
    private boolean Hiding;
    private int ChirpDelay;
    private int Share;
    @Nullable
    private Vec3 WaypointCentre;
    private final Map<UUID, Long> Ignored = new HashMap<>();

    public Scp019Drone(EntityType<? extends Scp019Drone> Type, Level World) {
        super(Type, World);
        Share = MinShare + random.nextInt(MaxShare - MinShare + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    public int emergeTicks() {
        return EmergeTicks;
    }

    @Override
    public int formTicks() {
        return FormTicks;
    }

    @Override
    public double leash() {
        return Scp019PotBlockEntity.SearchRadius;
    }

    @Override
    protected Scp019Gait.Shape gaitShape() {
        return DroneGait;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp019Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp019Goals.DroneReport(this));
        goalSelector.addGoal(3, new Scp019Goals.DroneReturn(this));
        goalSelector.addGoal(4, new Scp019Goals.DroneSearch(this));
        goalSelector.addGoal(5, new Scp019Goals.Wander(this, SearchSpeed, 10));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public String state() {
        byte Current = action();
        if (Hiding && Current == ActionNone) {
            return "hiding";
        }
        if (Current == ActionEmerge || Current == ActionForm) {
            return "emerging";
        }
        if (Current == ActionEnter) {
            return "entering";
        }
        if (Current == ActionSqueal) {
            return "squealing";
        }
        if (Reporting) {
            return "reporting";
        }
        if (Returning) {
            return "returning";
        }
        return isOwned() ? "searching" : "wandering";
    }

    public boolean isReporting() {
        return Reporting;
    }

    public boolean isReturning() {
        return Returning;
    }

    public boolean isHiding() {
        return Hiding;
    }

    public void hide() {
        if (Reporting) {
            return;
        }
        Hiding = true;
        Returning = true;
        WaypointCentre = null;
        Scp019PotBlockEntity Home = home();
        if (Home != null && Home.map() != null) {
            Home.map().release(getId());
        }
        getNavigation().stop();
        ChirpDelay = 2 + random.nextInt(18);
    }

    public void calm() {
        if (!Hiding) {
            return;
        }
        Hiding = false;
        ChirpDelay = 0;
        Returning = Share <= 0;
    }

    public void setReturning(boolean Value) {
        Returning = Value;
    }

    public int share() {
        return Share;
    }

    public void spendShare() {
        Share--;
        if (Share <= 0) {
            Returning = true;
        }
    }

    @Nullable
    public Vec3 waypointCentre() {
        return WaypointCentre;
    }

    public void setWaypointCentre(@Nullable Vec3 Value) {
        WaypointCentre = Value;
    }

    @Nullable
    public Vec3 quarrySeen() {
        return QuarrySeen;
    }

    @Nullable
    public LivingEntity quarry() {
        if (Quarry == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(Quarry) instanceof LivingEntity Found && Found.isAlive() ? Found : null;
    }

    public void refreshQuarry() {
        LivingEntity Found = quarry();
        if (Found != null && isValidTarget(Found) && getSensing().hasLineOfSight(Found)) {
            QuarrySeen = Found.position();
        }
    }

    public void delivered() {
        if (Quarry != null) {
            Ignored.put(Quarry, level().getGameTime() + IgnoreTicks);
        }
        Quarry = null;
        QuarrySeen = null;
        Reporting = false;
        Scp019PotBlockEntity Home = home();
        if (Home != null && Home.alarmed()) {
            hide();
        }
    }

    private boolean ignored(LivingEntity Target) {
        Long Until = Ignored.get(Target.getUUID());
        return Until != null && Until > level().getGameTime();
    }

    public Vec3 centre() {
        Scp019PotBlockEntity Home = home();
        return Home != null ? Home.centre() : anchor();
    }

    @Override
    protected void serverTick() {
        if (tickCount % 200 == 0) {
            long Now = level().getGameTime();
            Ignored.values().removeIf(Until -> Until <= Now);
        }
        if (ChirpDelay > 0 && --ChirpDelay == 0 && Hiding) {
            Scp019PotBlockEntity Home = home();
            if (Home != null && Home.tryChirp()) {
                playSound(Scp019Registry.DroneSquealCry.get(), ChirpVolume, 1.3f + random.nextFloat() * 0.2f);
            }
        }
        if (Reporting || Hiding || locksMovement() || (tickCount + getId()) % ScanTicks != 0) {
            return;
        }
        Vec3 Centre = centre();
        boolean Spare = sparesPlayers();
        AABB Box = getBoundingBox().inflate(SightRange, 4.0, SightRange);
        List<LivingEntity> Found = level().getEntitiesOfClass(LivingEntity.class, Box, Target -> isValidTarget(Target, Spare) && !ignored(Target)
                && Target.distanceToSqr(this) <= SightRange * SightRange && Scp019PotBlockEntity.inLeash(Centre, Target.position(), Scp019PotBlockEntity.SearchRadius));
        if (Found.isEmpty()) {
            return;
        }
        Found.sort(Comparator.comparingDouble(Target -> Target.distanceToSqr(this)));
        for (int Index = 0; Index < Math.min(3, Found.size()); Index++) {
            LivingEntity Target = Found.get(Index);
            if (getSensing().hasLineOfSight(Target)) {
                spotted(Target);
                return;
            }
        }
    }

    public void spotted(LivingEntity Target) {
        if (!isValidTarget(Target)) {
            return;
        }
        Quarry = Target.getUUID();
        QuarrySeen = Target.position();
        Reporting = true;
        Hiding = false;
        WaypointCentre = null;
        Scp019PotBlockEntity Home = home();
        if (Home != null && Home.map() != null) {
            Home.map().release(getId());
        }
        if (Home != null) {
            Home.raiseAlarm(Target, this);
        }
        getNavigation().stop();
        getLookControl().setLookAt(Target, 60.0f, 60.0f);
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        setAction(ActionSqueal, SquealTicks);
        float Pitch = 0.95f + random.nextFloat() * 0.12f;
        playSound(Scp019Registry.DroneSqueal.get(), SquealVolume, Pitch);
        playSound(Scp019Registry.DroneSquealShrill.get(), SquealVolume * 0.8f, Pitch);
        playSound(Scp019Registry.DroneSquealCry.get(), SquealVolume * 0.7f, Pitch);
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind == ActionSqueal) {
            LivingEntity Target = quarry();
            if (Target != null) {
                getLookControl().setLookAt(Target, 60.0f, 60.0f);
            }
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionEnter) {
            Scp019PotBlockEntity Home = home();
            if (Home != null) {
                Home.droneGone(this);
            }
            discard();
        }
    }

    public void beginEntering(float Yaw) {
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
        setAction(ActionEnter, EnterTicks);
        float Pitch = 0.95f + random.nextFloat() * 0.1f;
        playSound(Scp019Registry.Enter.get(), 0.6f, Pitch);
        playSound(Scp019Registry.EnterScrabble.get(), 0.45f, Pitch);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && isAlive() && !Reporting && !Hiding && !locksMovement() && Source.getEntity() instanceof LivingEntity Attacker
                && isValidTarget(Attacker)) {
            spotted(Attacker);
        }
        return Hurt;
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isActing() ? null : Scp019Registry.DroneChitter.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp019Registry.DroneHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp019Registry.DroneDeath.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Share", Share);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Share")) {
            Share = Tag.getInt("Share");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp019GaitController<>(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp019GaitController.blendFor(Current));
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionForm) {
                return Event.setAndContinue(FormPlay);
            }
            if (Current == ActionEnter) {
                return Event.setAndContinue(EnterPlay);
            }
            if (Current == ActionSqueal) {
                return Event.setAndContinue(SquealPlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
    }
}
