package net.rai.scp_unleashed.scp682;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp173.Scp173Sight;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.scp457.Scp457;

import java.util.List;

public final class Scp682Rivals {
    public static final double StatueWatchRange = 24.0;
    public static final float WaryRange = 16.0f;
    public static final double WaryWalk = 1.0;
    public static final double WaryRun = 1.35;

    private Scp682Rivals() {
    }

    public static final class RivalTarget extends NearestAttackableTargetGoal<Scp682> {
        public RivalTarget(Mob Hunter) {
            super(Hunter, Scp682.class, 10, true, false, Found -> Found instanceof Scp682 Reptile && Reptile.isAlive() && !Reptile.isVanishing());
        }
    }

    public static final class Wariness extends AvoidEntityGoal<Scp682> {
        public Wariness(PathfinderMob Prey) {
            super(Prey, Scp682.class, WaryRange, WaryWalk, WaryRun);
        }
    }

    public static boolean mayAttack(Scp682 Owner, LivingEntity Target) {
        if (Target instanceof Scp682 || Owner.isCalm()) {
            return false;
        }
        return !Target.getType().is(Scp682Registry.Ignored) && !Target.getType().is(Scp682Registry.Soothing) && !Target.getType().is(Scp682Registry.Unseen);
    }

    public static boolean isPrey(Scp682 Owner, LivingEntity Body) {
        if (Body == Owner || !Body.isAlive() || Body.isRemoved() || Body.isSpectator() || !Targeting.isTargetable(Body) || !mayAttack(Owner, Body)) {
            return false;
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && User.level().getDifficulty() != Difficulty.PEACEFUL && !Scp268.isNeglected(User);
        }
        if (Body instanceof Scp457) {
            return Owner.holds(Scp682Signature.Cinder);
        }
        if (ControlBodies.isPersonLike(Body)) {
            return !ControlSessions.isBoundToSession(Body) && !(Body instanceof DClassLike Person && (Person.isSpecimen() || Person.isPuppet()));
        }
        return Body instanceof AbstractVillager || Body instanceof Animal || Body instanceof WaterAnimal || Body instanceof AbstractGolem;
    }

    public static boolean soothed(ServerLevel World, Scp682 Owner) {
        AABB Area = Owner.getBoundingBox().inflate(Scp682.CalmRange);
        return !World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found.isAlive() && Found.getType().is(Scp682Registry.Soothing)).isEmpty();
    }

    public static boolean unseenNear(ServerLevel World, Scp682 Owner) {
        AABB Area = Owner.getBoundingBox().inflate(Scp682.SealRange);
        return !World.getEntitiesOfClass(Entity.class, Area, Found -> Found.isAlive() && Found.getType().is(Scp682Registry.Unseen)).isEmpty();
    }

    public static void provoke(ServerLevel World, Scp682 Owner) {
        AABB Area = Owner.getBoundingBox().inflate(Scp682.ProvokeRange);
        for (Mob Hunter : World.getEntitiesOfClass(Mob.class, Area, Found -> Found.isAlive() && Found.getType().is(Scp682Registry.Rivals) && Found.getTarget() == null)) {
            if (Hunter.canAttack(Owner) && !Hunter.isNoAi()) {
                Hunter.setTarget(Owner);
            }
        }
    }

    public static void joined(Entity Joined) {
        if (!(Joined instanceof PathfinderMob Body) || Joined instanceof Scp682) {
            return;
        }
        if (Joined.getType().is(Scp682Registry.Rivals) && !has(Body.targetSelector.getAvailableGoals(), RivalTarget.class)) {
            Body.targetSelector.addGoal(2, new RivalTarget(Body));
        }
        if (Joined.getType().is(Scp682Registry.Wary) && !has(Body.goalSelector.getAvailableGoals(), Wariness.class)) {
            Body.goalSelector.addGoal(1, new Wariness(Body));
        }
    }

    private static boolean has(Iterable<WrappedGoal> Goals, Class<?> Kind) {
        for (WrappedGoal Each : Goals) {
            if (Kind.isInstance(Each.getGoal())) {
                return true;
            }
        }
        return false;
    }

    public static boolean watchesStatue(ServerLevel World, Scp173Statue Statue) {
        AABB Box = Statue.sightBox();
        List<Scp682> Near = World.getEntitiesOfClass(Scp682.class, Box.inflate(StatueWatchRange), Reptile -> Reptile.isAlive() && Reptile.eyes() == Scp682.EyeMany);
        if (Near.isEmpty()) {
            return false;
        }
        List<Vec3> Points = Scp173Sight.samples(Box);
        for (Scp682 Reptile : Near) {
            AABB Hide = Reptile.getBoundingBox();
            Vec3[] Eyes = {Reptile.getEyePosition(), Hide.getCenter().add(0.0, Hide.getYsize() * 0.45, 0.0),
                    new Vec3(Hide.minX + 0.1, Hide.getCenter().y, Hide.getCenter().z), new Vec3(Hide.maxX - 0.1, Hide.getCenter().y, Hide.getCenter().z),
                    new Vec3(Hide.getCenter().x, Hide.getCenter().y, Hide.minZ + 0.1), new Vec3(Hide.getCenter().x, Hide.getCenter().y, Hide.maxZ - 0.1)};
            for (Vec3 Eye : Eyes) {
                for (Vec3 Point : Points) {
                    if (Eye.distanceToSqr(Point) <= StatueWatchRange * StatueWatchRange && Scp173Sight.clear(World, Eye, Point)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean preyForAbel(Entity Hunter, LivingEntity Body) {
        return Body instanceof Scp682 Reptile && Reptile.isAlive() && !Reptile.isVanishing() && !Reptile.isCommandBusy();
    }

    public static boolean quarryForTitan(LivingEntity Body) {
        return Body instanceof Scp682 Reptile && Reptile.isAlive() && !Reptile.isVanishing() && !Reptile.isCommandBusy();
    }

    public static boolean preyForBurning(Scp457 Hunter, LivingEntity Body) {
        return Body instanceof Scp682 Reptile && Reptile.isAlive() && !Reptile.isVanishing() && !Reptile.isCommandBusy() && !Reptile.holds(Scp682Signature.Cinder);
    }

    public static boolean preyForStatue(Scp173Statue Statue, LivingEntity Body) {
        return Body instanceof Scp682 Reptile && Reptile.isAlive() && !Reptile.isVanishing() && !Reptile.isCommandBusy();
    }
}
