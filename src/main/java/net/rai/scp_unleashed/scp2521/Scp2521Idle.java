package net.rai.scp_unleashed.scp2521;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp2521Idle {
    public static final int StateRest = 0;
    public static final int StatePace = 1;
    public static final int StatePress = 2;
    public static final double Radius = 1.75;
    public static final double PaceMin = 0.6;
    public static final double PaceSpeed = 0.022;
    public static final double Arrive = 0.03;
    public static final double WallGap = 0.2;
    public static final double WallReach = Radius + 0.4;
    public static final double ProbeStep = 0.05;
    public static final double BodyLow = 0.1;
    public static final double BodyHigh = 1.8;
    public static final int Settle = 40;
    public static final int RestMin = 50;
    public static final int RestMax = 140;
    public static final int PaceLimit = 200;
    public static final int PressMin = 60;
    public static final int PressMax = 110;
    public static final int PaceWeight = 65;
    public static final int PressWeight = 15;
    public static final double WatchRange = 12.0;
    public static final double ReaderRange = 16.0;
    public static final double StareRange = 16.0;
    public static final double StareCone = 0.978;
    public static final float TurnRate = 3.0f;
    public static final float StepTurnRate = 5.0f;
    public static final float FaceSlack = 30.0f;
    public static final float GazeRise = 0.08f;
    public static final float GazeFall = 0.03f;
    public static final float LeanRate = 0.05f;
    public static final int Directions = 8;

    private int State = StateRest;
    private int Ticks = Settle;
    private boolean Started;
    private Vec3 Offset = Vec3.ZERO;
    private Vec3 Goal = Vec3.ZERO;
    private float PressYaw;
    private boolean Pressing;
    private boolean Placed;

    public int state() {
        return State;
    }

    public Vec3 offset() {
        return Offset;
    }

    public void reset() {
        State = StateRest;
        Ticks = Settle;
        Started = false;
        Offset = Vec3.ZERO;
        Goal = Vec3.ZERO;
        Pressing = false;
        Placed = true;
    }

    public static boolean walled(ServerLevel World, Scp2521 Creature, Vec3 Feet) {
        double Half = Scp2521.Width * 0.5;
        AABB Body = new AABB(Feet.x - Half, Feet.y + BodyLow, Feet.z - Half, Feet.x + Half, Feet.y + BodyHigh, Feet.z + Half);
        return !World.noCollision(Creature, Body);
    }

    public static boolean floored(ServerLevel World, Vec3 Feet) {
        BlockPos Below = BlockPos.containing(Feet.x, Feet.y - 0.5, Feet.z);
        return !World.getBlockState(Below).getCollisionShape(World, Below).isEmpty();
    }

    public static boolean free(ServerLevel World, Scp2521 Creature, Vec3 Feet) {
        return floored(World, Feet) && !walled(World, Creature, Feet);
    }

    private static Vec3 clamp(Vec3 Flat) {
        double Reach = Math.sqrt(Flat.x * Flat.x + Flat.z * Flat.z);
        return Reach > Radius ? new Vec3(Flat.x * Radius / Reach, 0.0, Flat.z * Radius / Reach) : new Vec3(Flat.x, 0.0, Flat.z);
    }

    public boolean pace(ServerLevel World, Scp2521 Creature, Vec3 Base, Vec3 Wanted) {
        Vec3 Target = clamp(Wanted);
        if (!free(World, Creature, Base.add(Target))) {
            return false;
        }
        State = StatePace;
        Ticks = PaceLimit;
        Goal = Target;
        Pressing = false;
        return true;
    }

    public boolean press(ServerLevel World, Scp2521 Creature, Vec3 Base) {
        double Best = Double.MAX_VALUE;
        Vec3 Chosen = null;
        float Yaw = 0.0f;
        for (int Index = 0; Index < Directions; Index++) {
            double Angle = Math.PI * 2.0 * Index / Directions;
            Vec3 Way = new Vec3(-Math.sin(Angle), 0.0, Math.cos(Angle));
            for (double Reach = ProbeStep; Reach <= WallReach; Reach += ProbeStep) {
                Vec3 Probe = Base.add(Way.scale(Reach));
                if (!walled(World, Creature, Probe)) {
                    if (!floored(World, Probe)) {
                        break;
                    }
                    continue;
                }
                double Stop = Reach - ProbeStep - WallGap;
                if (Reach < Best && Stop > 0.0 && Stop <= Radius && free(World, Creature, Base.add(Way.scale(Stop)))) {
                    Best = Reach;
                    Chosen = Way.scale(Stop);
                    Yaw = (float) (Angle * Mth.RAD_TO_DEG);
                }
                break;
            }
        }
        if (Chosen == null) {
            return false;
        }
        State = StatePress;
        Ticks = PaceLimit;
        Goal = Chosen;
        PressYaw = Yaw;
        Pressing = false;
        return true;
    }

    private void rest(RandomSource Random) {
        State = StateRest;
        Ticks = RestMin + Random.nextInt(RestMax - RestMin + 1);
        Pressing = false;
    }

    private Vec3 randomSpot(RandomSource Random) {
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Reach = PaceMin + Random.nextDouble() * (Radius - PaceMin);
        return new Vec3(Math.cos(Angle) * Reach, 0.0, Math.sin(Angle) * Reach);
    }

    private void choose(ServerLevel World, Scp2521 Creature, Vec3 Base) {
        RandomSource Random = Creature.getRandom();
        if (!Started) {
            Started = true;
            for (int Attempt = 0; Attempt < 6; Attempt++) {
                if (pace(World, Creature, Base, randomSpot(Random))) {
                    return;
                }
            }
            rest(Random);
            return;
        }
        int Roll = Random.nextInt(100);
        if (Roll < PaceWeight) {
            for (int Attempt = 0; Attempt < 6; Attempt++) {
                if (pace(World, Creature, Base, randomSpot(Random))) {
                    return;
                }
            }
        } else if (Roll < PaceWeight + PressWeight) {
            if (press(World, Creature, Base)) {
                return;
            }
        }
        rest(Random);
    }

    private void move(ServerLevel World, Scp2521 Creature, Vec3 Base) {
        Vec3 Left = Goal.subtract(Offset);
        double Gap = Left.length();
        if (Gap <= Arrive) {
            Offset = Goal;
            if (State == StatePress) {
                Pressing = true;
                Ticks = PressMin + Creature.getRandom().nextInt(PressMax - PressMin + 1);
            } else {
                rest(Creature.getRandom());
            }
            return;
        }
        if (Math.abs(Mth.wrapDegrees(Creature.getYRot() - heading(Left))) > FaceSlack) {
            return;
        }
        Vec3 Next = clamp(Offset.add(Left.scale(Math.min(PaceSpeed, Gap) / Gap)));
        if (!free(World, Creature, Base.add(Next))) {
            rest(Creature.getRandom());
            return;
        }
        Offset = Next;
    }

    public void tick(ServerLevel World, Scp2521 Creature, BlockPos Anchor) {
        Vec3 Base = Vec3.atBottomCenterOf(Anchor);
        RandomSource Random = Creature.getRandom();
        if (!Placed) {
            Placed = true;
            Offset = clamp(Creature.position().subtract(Base));
        }
        switch (State) {
            case StatePace -> {
                move(World, Creature, Base);
                if (State == StatePace && --Ticks <= 0) {
                    rest(Random);
                }
            }
            case StatePress -> {
                if (!Pressing) {
                    move(World, Creature, Base);
                    if (State == StatePress && !Pressing && --Ticks <= 0) {
                        rest(Random);
                    }
                } else if (--Ticks <= 0) {
                    rest(Random);
                }
            }
            default -> {
                if (--Ticks <= 0) {
                    choose(World, Creature, Base);
                }
            }
        }
        Offset = clamp(Offset);
        Vec3 Where = Base.add(Offset.x, 0.0, Offset.z);
        if (Creature.position().distanceToSqr(Where) > 1.0e-8) {
            Creature.setPos(Where.x, Where.y, Where.z);
        }
        Player Starer = starer(World, Creature);
        Creature.setGaze(approach(Creature.gaze(), Starer != null ? 1.0f : 0.0f, Starer != null ? GazeRise : GazeFall));
        Creature.setLean(approach(Creature.lean(), State == StatePress && Pressing ? 1.0f : 0.0f, LeanRate));
        Player Seen = Starer != null ? Starer : watcher(World, Base);
        Float Want = null;
        float Rate = TurnRate;
        Vec3 Way = Goal.subtract(Offset);
        boolean Walking = (State == StatePace || State == StatePress && !Pressing) && Way.horizontalDistanceSqr() > Arrive * Arrive;
        if (State == StatePress && Pressing) {
            Want = PressYaw;
        } else if (Walking) {
            Want = heading(Way);
            Rate = StepTurnRate;
        } else if (Seen != null) {
            Want = (float) (Mth.atan2(Seen.getZ() - Creature.getZ(), Seen.getX() - Creature.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        }
        if (Want != null) {
            Creature.turn(Mth.approachDegrees(Creature.getYRot(), Want, Rate));
        } else {
            Creature.turn(Creature.getYRot());
        }
    }

    public static float heading(Vec3 Way) {
        return (float) (Mth.atan2(Way.z, Way.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    private static float approach(float Current, float Target, float Step) {
        return Current < Target ? Math.min(Target, Current + Step) : Math.max(Target, Current - Step);
    }

    @Nullable
    public static Player starer(ServerLevel World, Scp2521 Creature) {
        Vec3 Head = Creature.getEyePosition();
        Vec3 Middle = Creature.position().add(0.0, Creature.getBbHeight() * 0.5, 0.0);
        Player Best = null;
        double Closest = StareRange * StareRange;
        for (ServerPlayer Each : World.players()) {
            if (Each.isSpectator() || !Each.isAlive()) {
                continue;
            }
            Vec3 Eye = Each.getEyePosition();
            double Distance = Eye.distanceToSqr(Middle);
            if (Distance > Closest) {
                continue;
            }
            Vec3 View = Each.getViewVector(1.0f);
            boolean Facing = View.dot(Head.subtract(Eye).normalize()) > StareCone || View.dot(Middle.subtract(Eye).normalize()) > StareCone;
            if (!Facing) {
                continue;
            }
            if (World.clip(new ClipContext(Eye, Middle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Each)).getType() != HitResult.Type.MISS) {
                continue;
            }
            Best = Each;
            Closest = Distance;
        }
        return Best;
    }

    @Nullable
    public static Player watcher(ServerLevel World, Vec3 Base) {
        Player Reader = null;
        double ReaderClosest = ReaderRange * ReaderRange;
        Player Anyone = null;
        double AnyoneClosest = WatchRange * WatchRange;
        for (ServerPlayer Each : World.players()) {
            if (Each.isSpectator() || !Each.isAlive()) {
                continue;
            }
            double Distance = Each.distanceToSqr(Base);
            if (Distance < ReaderClosest && Scp2521Rules.knows(Each)) {
                Reader = Each;
                ReaderClosest = Distance;
            }
            if (Distance < AnyoneClosest) {
                Anyone = Each;
                AnyoneClosest = Distance;
            }
        }
        return Reader != null ? Reader : Anyone;
    }
}
