package net.rai.scp_unleashed.scp003;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.Optional;

public class Scp003Host extends Scp003Creature {
    public static final int MaxBuilds = 3;
    public static final int BuildCooldown = 24000;
    public static final int FirstBuildDelay = 600;
    public static final int AbandonCooldown = 1200;
    public static final int ColonyCap = 6;
    public static final double ColonyRange = 96.0;
    public static final int RitualTicks = 100;
    public static final int PlaceTicks = 16;
    public static final int MaxReplacements = 6;
    public static final int SearchAttempts = 12;
    public static final int ApproachLimit = 600;
    public static final double PlaceReach = 2.6;
    public static final double TendRange = 4.5;
    public static final double QuarryRange = 12.0;
    public static final double SnapReach = 0.6;
    public static final double SteerReach = 1.75;
    public static final double StallSnapReach = 2.5;
    public static final int StallTicks = 60;
    public static final int MaxStalls = 3;
    public static final int ResiteDelay = 200;
    public static final Scp003Gait.Profile HostGait = new Scp003Gait.Profile(26.0, 0.0641, 0.5, 3.0, 5, new double[]{0.965, 0.465}, new float[]{1.0f, 1.0f},
            Scp003Registry.HostStep, Scp003Registry.HostThud, Scp003Registry.BarkCreak, Scp003Registry.RootShuffle,
            0.22f, 0.4f, 0.15f, 0.4f, 1.0f, 0.22f);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation PlacePlay = RawAnimation.begin().thenPlayAndHold("place");
    private static final RawAnimation RitualHold = RawAnimation.begin().thenPlayAndHold("ritual");
    private static final RawAnimation TendLoop = RawAnimation.begin().thenLoop("tend");
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");

    public enum BuildPhase {
        None,
        Seeking,
        Placing,
        Tending,
        Ritual
    }

    private BuildPhase Phase = BuildPhase.None;
    @Nullable
    private BlockPos Site;
    @Nullable
    private BlockPos PlacingCorner;
    private int Builds;
    private int Cooldown = FirstBuildDelay;
    private int Replacements;
    private int Searches;
    private int PhaseTicks;
    private double BestApproach = Double.MAX_VALUE;
    private int StallTimer;
    private int Stalls;

    public Scp003Host(EntityType<? extends Scp003Host> Type, Level World) {
        super(Type, World);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.3).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public int stage() {
        return 3;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp003Goals.HoldStill(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(3, new BuildGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, Target -> Target.distanceToSqr(this) < QuarryRange * QuarryRange));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                Target -> isQuarry(Target) && Target.distanceToSqr(this) < QuarryRange * QuarryRange));
    }

    public BuildPhase buildPhase() {
        return Phase;
    }

    @Nullable
    public BlockPos site() {
        return Site;
    }

    public int builds() {
        return Builds;
    }

    public int cooldown() {
        return Cooldown;
    }

    public void growNow() {
        if (!isActing() || action() == ActionEmerge) {
            Cooldown = 0;
            if (Builds >= MaxBuilds) {
                Builds = MaxBuilds - 1;
            }
        }
    }

    public boolean colonyAllows() {
        return level().getEntitiesOfClass(Scp003Creature.class, getBoundingBox().inflate(ColonyRange), Creature -> Creature.isAlive() && !Creature.isTransferred()).size() < ColonyCap;
    }

    public boolean wantsToBuild() {
        return Phase != BuildPhase.None || Builds < MaxBuilds && Cooldown <= 0;
    }

    @Override
    protected void serverTick() {
        if (Cooldown > 0) {
            Cooldown--;
        }
        if (Phase == BuildPhase.Tending && action() == ActionNone && getNavigation().isDone() && tickCount % 80 == 0 && Site != null) {
            getLookControl().setLookAt(Vec3.atCenterOf(Scp003Ritual.corners(Site).get(random.nextInt(4))));
        }
    }

    @Nullable
    private BlockPos nextMissingCorner() {
        if (Site == null) {
            return null;
        }
        for (BlockPos Corner : Scp003Ritual.corners(Site)) {
            BlockState State = level().getBlockState(Corner);
            if (!(State.getBlock() instanceof Scp003PolypBlock) || State.getValue(Scp003PolypBlock.Withered)) {
                return Corner;
            }
        }
        return null;
    }

    private boolean allGrown() {
        if (Site == null) {
            return false;
        }
        for (BlockPos Corner : Scp003Ritual.corners(Site)) {
            if (!Scp003PolypBlock.isGrown(level().getBlockState(Corner))) {
                return false;
            }
        }
        return true;
    }

    public int grownCount() {
        if (Site == null) {
            return 0;
        }
        int Count = 0;
        for (BlockPos Corner : Scp003Ritual.corners(Site)) {
            if (Scp003PolypBlock.isGrown(level().getBlockState(Corner))) {
                Count++;
            }
        }
        return Count;
    }

    private void setPhase(BuildPhase Next) {
        Phase = Next;
        PhaseTicks = 0;
        BestApproach = Double.MAX_VALUE;
        StallTimer = 0;
        Stalls = 0;
    }

    private boolean fitsAt(ServerLevel World, BlockPos Target) {
        return World.noCollision(this, getBoundingBox().move(Target.getX() + 0.5 - getX(), Target.getY() - getY(), Target.getZ() + 0.5 - getZ()));
    }

    public void abandonBuild(boolean ApplyCooldown) {
        if (level() instanceof ServerLevel World) {
            Scp003Colony.get(World.getServer()).purge(lineage(), World.getServer());
        }
        Site = null;
        PlacingCorner = null;
        Replacements = 0;
        setPhase(BuildPhase.None);
        if (ApplyCooldown) {
            Cooldown = Math.max(Cooldown, AbandonCooldown);
        }
        if (action() == ActionPlace || action() == ActionRitual) {
            setAction(ActionNone, 0);
        }
    }

    private void walkTo(BlockPos Target, double Speed) {
        if (PhaseTicks % 10 == 1 || getNavigation().isDone()) {
            getNavigation().moveTo(Target.getX() + 0.5, Target.getY(), Target.getZ() + 0.5, Speed);
        }
    }

    private double horizontalDistanceSqr(BlockPos Target) {
        double Dx = getX() - (Target.getX() + 0.5);
        double Dz = getZ() - (Target.getZ() + 0.5);
        return Dx * Dx + Dz * Dz;
    }

    private void buildTick() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        PhaseTicks++;
        switch (Phase) {
            case Seeking -> seekTick(World);
            case Placing -> placeTick(World);
            case Tending -> tendTick(World);
            case Ritual -> ritualTick(World);
            default -> {
            }
        }
    }

    private void seekTick(ServerLevel World) {
        if (PhaseTicks % 20 != 1) {
            return;
        }
        if (!colonyAllows()) {
            Cooldown = AbandonCooldown;
            setPhase(BuildPhase.None);
            return;
        }
        Optional<BlockPos> Found = Scp003Ritual.findSite(World, this, random);
        if (Found.isPresent()) {
            Site = Found.get();
            Replacements = 0;
            Searches = 0;
            setPhase(BuildPhase.Placing);
        } else if (++Searches >= SearchAttempts) {
            Searches = 0;
            Cooldown = AbandonCooldown;
            setPhase(BuildPhase.None);
        }
    }

    private void placeTick(ServerLevel World) {
        if (Site == null || !Scp003Ritual.isOpenSite(World, Site, true)) {
            abandonBuild(true);
            return;
        }
        if (action() == ActionPlace) {
            return;
        }
        BlockPos Next = nextMissingCorner();
        if (Next == null) {
            setPhase(BuildPhase.Tending);
            return;
        }
        if (horizontalDistanceSqr(Next) > PlaceReach * PlaceReach || Math.abs(getY() - Next.getY()) > 1.5) {
            walkTo(Next, 0.9);
            if (PhaseTicks > ApproachLimit) {
                abandonBuild(true);
            }
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(Next.getX() + 0.5, Next.getY() + 0.2, Next.getZ() + 0.5);
        PlacingCorner = Next;
        setAction(ActionPlace, PlaceTicks);
    }

    private void tendTick(ServerLevel World) {
        if (Site == null) {
            setPhase(BuildPhase.None);
            return;
        }
        if (horizontalDistanceSqr(Site) > TendRange * TendRange) {
            walkTo(Site.relative(Direction.from2DDataValue(Math.floorMod(getId(), 4)), 2), 0.8);
        }
        if (PhaseTicks % 20 != 0) {
            return;
        }
        if (!Scp003Ritual.isOpenSite(World, Site, true)) {
            abandonBuild(true);
            return;
        }
        if (nextMissingCorner() != null) {
            if (++Replacements > MaxReplacements) {
                abandonBuild(true);
            } else {
                setPhase(BuildPhase.Placing);
            }
            return;
        }
        if (allGrown() && colonyAllows()) {
            setPhase(BuildPhase.Ritual);
        }
    }

    private void ritualTick(ServerLevel World) {
        if (action() == ActionRitual) {
            return;
        }
        if (Site == null || !Scp003Ritual.isOpenSite(World, Site, true) || !Scp003Ritual.canFormAt(World, Site)) {
            abandonBuild(true);
            return;
        }
        if (!allGrown()) {
            setPhase(BuildPhase.Tending);
            return;
        }
        double Distance = Math.sqrt(horizontalDistanceSqr(Site));
        boolean Flat = Math.abs(getY() - Site.getY()) <= 0.6;
        boolean Stalled = false;
        if (Distance < BestApproach - 0.2) {
            BestApproach = Distance;
            StallTimer = 0;
        } else if (++StallTimer >= StallTicks) {
            StallTimer = 0;
            BestApproach = Distance;
            Stalled = true;
            Stalls++;
        }
        boolean Close = Distance <= SnapReach || Stalled && Distance <= StallSnapReach;
        if (!(Flat && Close && fitsAt(World, Site))) {
            if (Stalls >= MaxStalls || PhaseTicks > ApproachLimit) {
                abandonBuild(false);
                Cooldown = Math.max(Cooldown, ResiteDelay);
            } else if (Flat && Distance <= SteerReach) {
                getNavigation().stop();
                getMoveControl().setWantedPosition(Site.getX() + 0.5, Site.getY(), Site.getZ() + 0.5, 0.6);
            } else {
                walkTo(Site, 0.75);
            }
            return;
        }
        getNavigation().stop();
        moveTo(Site.getX() + 0.5, Site.getY(), Site.getZ() + 0.5, getYRot(), getXRot());
        setAction(ActionRitual, RitualTicks);
        World.playSound(null, getX(), getY() + 1.0, getZ(), Scp003Registry.Ritual.get(), SoundSource.HOSTILE, 1.2f, 1.0f);
        World.playSound(null, getX(), getY() + 1.0, getZ(), Scp003Registry.RitualDrone.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Kind == ActionPlace && Remaining == PlaceTicks / 2 && PlacingCorner != null && Site != null) {
            BlockPos Corner = PlacingCorner;
            BlockState At = World.getBlockState(Corner);
            if ((At.isAir() || At.canBeReplaced() && !(At.getBlock() instanceof Scp003PolypBlock)) && At.getFluidState().isEmpty()) {
                World.setBlock(Corner, Scp003Registry.Polyp.get().defaultBlockState(), Block.UPDATE_ALL);
                Scp003Colony.get(World.getServer()).track(lineage(), World, Corner);
                World.playSound(null, Corner, Scp003Registry.HostPlace.get(), SoundSource.HOSTILE, 0.9f, 0.9f + random.nextFloat() * 0.2f);
                World.sendParticles(Scp003Registry.Flesh.get(), Corner.getX() + 0.5, Corner.getY() + 0.2, Corner.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.05);
            } else if (At.getBlock() instanceof Scp003PolypBlock && At.getValue(Scp003PolypBlock.Withered)) {
                World.setBlock(Corner, Scp003Registry.Polyp.get().defaultBlockState(), Block.UPDATE_ALL);
                Scp003Colony.get(World.getServer()).track(lineage(), World, Corner);
            }
        }
        if (Kind == ActionRitual && Site != null) {
            ritualEffects(World, Remaining);
        }
    }

    private void ritualEffects(ServerLevel World, int Remaining) {
        Vec3 Centre = new Vec3(Site.getX() + 0.5, Site.getY() + 0.4, Site.getZ() + 0.5);
        if (Remaining % 3 == 0) {
            for (BlockPos Corner : Scp003Ritual.corners(Site)) {
                Vec3 From = new Vec3(Corner.getX() + 0.5, Corner.getY() + 0.75, Corner.getZ() + 0.5);
                Vec3 Toward = Centre.subtract(From);
                World.sendParticles(Scp003Registry.Spore.get(), From.x, From.y, From.z, 0, Toward.x, Toward.y + 0.2, Toward.z, 0.09);
            }
        }
        if (Remaining % 10 == 0) {
            World.sendParticles(Scp003Registry.Flesh.get(), Centre.x, Centre.y, Centre.z, 3, 0.25, 0.05, 0.25, 0.02);
        }
        if (Remaining == RitualTicks / 2) {
            World.playSound(null, Centre.x, Centre.y, Centre.z, Scp003Registry.Ritual.get(), SoundSource.HOSTILE, 0.8f, 0.85f);
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionPlace) {
            PlacingCorner = null;
            return;
        }
        if (Kind == ActionRitual && level() instanceof ServerLevel World) {
            formBoard(World);
        }
    }

    private void formBoard(ServerLevel World) {
        BlockPos Centre = Site;
        if (Centre == null || !Scp003Ritual.canFormAt(World, Centre) || !allGrown()) {
            abandonBuild(true);
            return;
        }
        BlockState Formed = Scp003Registry.Board.get().defaultBlockState().setValue(Scp003BoardBlock.Facing, Direction.from2DDataValue(random.nextInt(4)));
        World.setBlock(Centre, Formed, Block.UPDATE_ALL);
        for (BlockPos Corner : Scp003Ritual.corners(Centre)) {
            Scp003PolypBlock.wither(World, Corner);
        }
        Scp003Colony.get(World.getServer()).forget(lineage());
        World.playSound(null, Centre, Scp003Registry.BoardForm.get(), SoundSource.HOSTILE, 1.2f, 1.0f);
        World.sendParticles(Scp003Registry.Flesh.get(), Centre.getX() + 0.5, Centre.getY() + 0.2, Centre.getZ() + 0.5, 20, 0.3, 0.1, 0.3, 0.08);
        World.sendParticles(Scp003Registry.Spore.get(), Centre.getX() + 0.5, Centre.getY() + 0.3, Centre.getZ() + 0.5, 16, 0.4, 0.3, 0.4, 0.02);
        Builds++;
        Cooldown = BuildCooldown;
        Site = null;
        Replacements = 0;
        setPhase(BuildPhase.None);
        moveTo(getX() + (random.nextBoolean() ? 1.0 : -1.0), getY(), getZ(), getYRot(), getXRot());
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit) {
            playSound(Scp003Registry.HostAttack.get(), 0.9f, 0.9f + random.nextFloat() * 0.2f);
        }
        return Hit;
    }

    @Override
    public void writeJade(CompoundTag Data) {
        Data.putString("Phase", Phase.name());
        Data.putInt("Builds", Builds);
        Data.putInt("MaxBuilds", MaxBuilds);
        Data.putInt("Cooldown", Cooldown);
        Data.putInt("Grown", grownCount());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp003Registry.HostAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp003Registry.HostHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp003Registry.HostDeath.get();
    }

    @Override
    protected Scp003Gait.Profile gaitProfile() {
        return HostGait;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putString("Phase", Phase.name());
        if (Site != null) {
            Tag.put("Site", NbtUtils.writeBlockPos(Site));
        }
        Tag.putInt("Builds", Builds);
        Tag.putInt("Cooldown", Cooldown);
        Tag.putInt("Replacements", Replacements);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        try {
            Phase = BuildPhase.valueOf(Tag.getString("Phase"));
        } catch (IllegalArgumentException Problem) {
            Phase = BuildPhase.None;
        }
        Site = NbtUtils.readBlockPos(Tag, "Site").orElse(null);
        Builds = Tag.getInt("Builds");
        Cooldown = Tag.contains("Cooldown") ? Tag.getInt("Cooldown") : FirstBuildDelay;
        Replacements = Tag.getInt("Replacements");
        if (Site == null && Phase != BuildPhase.Seeking) {
            Phase = BuildPhase.None;
        }
        if (Phase == BuildPhase.Ritual || Phase == BuildPhase.Placing) {
            Phase = Site == null ? BuildPhase.None : BuildPhase.Tending;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp003GaitController<>(this, "body", 5, Event -> {
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionPlace) {
                return Event.setAndContinue(PlacePlay);
            }
            if (Current == ActionRitual) {
                return Event.setAndContinue(RitualHold);
            }
            if (isStriding()) {
                return Event.setAndContinue(WalkLoop);
            }
            return Event.setAndContinue(Phase == BuildPhase.Tending ? TendLoop : IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("attack", AttackOnce));
    }

    private static final class BuildGoal extends Goal {
        private final Scp003Host Builder;

        private BuildGoal(Scp003Host Builder) {
            this.Builder = Builder;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Builder.getTarget() == null && !Builder.locksMovement() && Builder.wantsToBuild();
        }

        @Override
        public boolean canContinueToUse() {
            return Builder.getTarget() == null && Builder.Phase != BuildPhase.None && Builder.isAlive();
        }

        @Override
        public void start() {
            if (Builder.Phase == BuildPhase.None) {
                Builder.Searches = 0;
                Builder.setPhase(BuildPhase.Seeking);
            } else {
                Builder.setPhase(Builder.Phase);
            }
        }

        @Override
        public void stop() {
            if (Builder.action() != ActionRitual && Builder.action() != ActionPlace) {
                Builder.getNavigation().stop();
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Builder.buildTick();
        }
    }
}
