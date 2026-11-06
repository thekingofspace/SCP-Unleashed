package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.faction.Factions;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public abstract class Scp610Utility extends Scp610Creature {
    public static final int FleeTicks = 100;
    public static final double FleeDistance = 10.0;
    public static final double FleeSpeed = 1.35;
    @Nullable
    private LivingEntity FleeFrom;
    private int FleeLeft;

    protected Scp610Utility(EntityType<? extends Scp610Utility> Type, Level World, Scp610Form Form) {
        super(Type, World, Form);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp610Goals.HoldStill(this));
        goalSelector.addGoal(2, new Flee(this));
        registerWork();
        goalSelector.addGoal(6, new Scp610Goals.ReturnHome(this, 0.9));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    protected abstract void registerWork();

    public boolean fleeing() {
        return FleeLeft > 0 && FleeFrom != null && FleeFrom.isAlive();
    }

    @Nullable
    public LivingEntity fleeFrom() {
        return FleeFrom;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && isAlive() && Source.getEntity() instanceof LivingEntity Attacker && !isFlesh(Attacker)) {
            threatened(Attacker);
        }
        return Hurt;
    }

    protected void threatened(LivingEntity Attacker) {
        FleeFrom = Attacker;
        FleeLeft = FleeTicks;
        setTarget(null);
    }

    @Override
    protected void serverTick() {
        if (FleeLeft > 0) {
            FleeLeft--;
        }
        work();
    }

    protected void work() {
    }

    @Nullable
    public Scp610Hive hive() {
        if (!(level() instanceof ServerLevel World)) {
            return null;
        }
        MinecraftServer Server = World.getServer();
        int Site = Factions.bondOf(this).Site();
        if (Site >= 0) {
            Scp610Hive Found = Scp610Hives.hive(Server, Site);
            if (Found != null) {
                return Found;
            }
        }
        BlockPos Home = home();
        return Scp610Biomass.nearest(World, Home != null ? Home : blockPosition(), 48.0);
    }

    public boolean near(Vec3 Point, double Reach) {
        return position().distanceToSqr(Point) <= Reach * Reach;
    }

    public void walkTo(Vec3 Point, double Speed) {
        if (tickCount % 10 == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(Point.x, Point.y, Point.z, Speed);
        }
    }

    public static final class Flee extends Goal {
        private final Scp610Utility Runner;

        public Flee(Scp610Utility Runner) {
            this.Runner = Runner;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Runner.fleeing() && !Runner.locksMovement();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity From = Runner.fleeFrom();
            if (From == null || Runner.tickCount % 10 != 0) {
                return;
            }
            Vec3 Away = Runner.position().subtract(From.position());
            if (Away.lengthSqr() < 1.0e-4) {
                Away = new Vec3(Runner.getRandom().nextDouble() - 0.5, 0.0, Runner.getRandom().nextDouble() - 0.5);
            }
            Vec3 Spot = Runner.position().add(Away.normalize().scale(FleeDistance));
            Runner.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, FleeSpeed);
        }
    }
}
