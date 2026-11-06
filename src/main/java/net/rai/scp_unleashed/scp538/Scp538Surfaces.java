package net.rai.scp_unleashed.scp538;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

public final class Scp538Surfaces {
    public static final double Reach = 0.96;
    public static final double Inset = 0.4;
    public static final double[] Insets = {0.4, 0.0, -0.3};
    public static final double Lift = 0.002;
    public static final double Squeeze = 0.006;
    public static final double CentreProbe = 0.04;
    public static final int MaxSettled = 320;
    public static final int Radius = 6;
    public static final double MinScore = 2.5;
    public static final double DistancePenalty = 0.12;
    public static final double TurnCost = 0.25;
    public static final double CrowdPenalty = 4.0;
    public static final double CrowdRadius = 1.25;
    public static final double Improvement = 0.6;
    public static final double UndersideScore = 3.0;
    public static final double WallScore = 0.5;
    public static final double CornerScore = 1.5;
    public static final double LowCoverScore = 2.5;
    public static final double TightScore = 1.0;
    public static final double OpenSkyPenalty = 1.5;
    public static final double DimScore = 0.2;
    public static final double FleeWeight = 1.0;
    public static final double FleeShade = 0.3;
    public static final double FleeMargin = 1.0;

    public enum Goal {
        Calm,
        Flee
    }

    public record Node(BlockPos Cell, Direction Side, Vec3 Position) {
    }

    private record Key(BlockPos Cell, Direction Side) {
    }

    private record Entry(double Cost, Node At) {
    }

    private Scp538Surfaces() {
    }

    public static Vec3 feet(Entity Spider, Direction Side, Vec3 Hit, Vec3 Centre) {
        double Width = Spider.getBbWidth();
        double Height = Spider.getBbHeight();
        return switch (Side) {
            case DOWN -> new Vec3(Centre.x, Hit.y + Lift, Centre.z);
            case UP -> new Vec3(Centre.x, Hit.y - Height - Lift, Centre.z);
            default -> new Vec3(Side.getStepX() != 0 ? Hit.x - Side.getStepX() * (Width * 0.5 + Lift) : Centre.x, Centre.y - Height * 0.5,
                    Side.getStepZ() != 0 ? Hit.z - Side.getStepZ() * (Width * 0.5 + Lift) : Centre.z);
        };
    }

    public static AABB box(Entity Spider, Vec3 Feet) {
        return Spider.getDimensions(Spider.getPose()).makeBoundingBox(Feet).deflate(Squeeze);
    }

    @Nullable
    public static Node node(Level World, Entity Spider, BlockPos Cell, Direction Side) {
        if (!World.isLoaded(Cell)) {
            return null;
        }
        Vec3 Centre = Vec3.atCenterOf(Cell);
        BlockHitResult Hit = null;
        for (double Back : Insets) {
            Vec3 Start = Centre.add(-Side.getStepX() * Back, -Side.getStepY() * Back, -Side.getStepZ() * Back);
            if (!World.noCollision(new AABB(Start, Start).inflate(CentreProbe))) {
                continue;
            }
            double Length = Reach - Inset + Back;
            Vec3 Far = Start.add(Side.getStepX() * Length, Side.getStepY() * Length, Side.getStepZ() * Length);
            Hit = World.clip(new ClipContext(Start, Far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            break;
        }
        if (Hit == null || Hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        Vec3 Feet = feet(Spider, Side, Hit.getLocation(), Centre);
        if (!World.noCollision(Spider, box(Spider, Feet))) {
            return null;
        }
        return new Node(Cell.immutable(), Side, Feet);
    }

    public static final class Search {
        private final Level World;
        private final Entity Spider;
        private final Map<Key, Optional<Node>> Nodes = new HashMap<>();
        private final Map<BlockPos, Integer> Lights = new HashMap<>();
        private final List<Vec3> Claims;

        public Search(Level World, Entity Spider, List<Vec3> Claims) {
            this.World = World;
            this.Spider = Spider;
            this.Claims = Claims;
        }

        @Nullable
        public Node node(BlockPos Cell, Direction Side) {
            return Nodes.computeIfAbsent(new Key(Cell.immutable(), Side), Found -> Optional.ofNullable(Scp538Surfaces.node(World, Spider, Found.Cell(), Found.Side()))).orElse(null);
        }

        public int light(BlockPos Cell) {
            return Lights.computeIfAbsent(Cell.immutable(), Found -> Scp538Light.at(World, Found));
        }

        public double score(Node At) {
            double Score = At.Side() == Direction.UP ? UndersideScore : At.Side() == Direction.DOWN ? 0.0 : WallScore;
            if (At.Side().getAxis().isVertical()) {
                int Walls = 0;
                for (Direction Side : Direction.Plane.HORIZONTAL) {
                    if (node(At.Cell(), Side) != null) {
                        Walls++;
                    }
                }
                Score += CornerScore * Math.min(2, Walls);
                if (node(At.Cell(), At.Side().getOpposite()) != null) {
                    Score += At.Side() == Direction.DOWN ? LowCoverScore : TightScore;
                }
            }
            if (World.canSeeSky(At.Cell())) {
                Score -= OpenSkyPenalty;
            }
            return Score + DimScore * (15 - light(At.Cell()));
        }

        public double crowd(Node At) {
            int Count = 0;
            for (Vec3 Claim : Claims) {
                if (Claim.distanceToSqr(At.Position()) < CrowdRadius * CrowdRadius) {
                    Count++;
                }
            }
            return CrowdPenalty * Count;
        }

        public boolean target(Node At) {
            return At.Side().getAxis().isVertical();
        }

        public double value(Node At, double Cost, Goal Wanted, @Nullable Vec3 Threat) {
            if (Wanted == Goal.Flee && Threat != null) {
                return FleeWeight * At.Position().distanceTo(Threat) + FleeShade * score(At) - 0.05 * Cost - crowd(At);
            }
            return score(At) - DistancePenalty * Cost - crowd(At);
        }

        public boolean clear(Vec3 From, Vec3 To) {
            int Samples = From.distanceToSqr(To) > 0.36 ? 3 : 1;
            for (int Index = 1; Index <= Samples; Index++) {
                Vec3 At = From.lerp(To, Index / (double) (Samples + 1));
                if (!World.noCollision(Spider, box(Spider, At))) {
                    return false;
                }
            }
            return true;
        }

        public List<Node> neighbours(Node At) {
            List<Node> Found = new ArrayList<>(9);
            for (Direction Side : Direction.values()) {
                if (Side != At.Side()) {
                    Node Turn = node(At.Cell(), Side);
                    if (Turn != null) {
                        Found.add(Turn);
                    }
                }
            }
            for (Direction Way : Direction.values()) {
                if (Way.getAxis() == At.Side().getAxis()) {
                    continue;
                }
                Node Step = node(At.Cell().relative(Way), At.Side());
                if (Step != null) {
                    Found.add(Step);
                }
            }
            return Found;
        }

        public List<Node> plan(Node Start, int MinLight, Goal Wanted, @Nullable Vec3 Threat) {
            PriorityQueue<Entry> Open = new PriorityQueue<>((First, Second) -> Double.compare(First.Cost(), Second.Cost()));
            Map<Key, Double> Costs = new HashMap<>();
            Map<Key, Node> Parents = new HashMap<>();
            Key StartKey = new Key(Start.Cell(), Start.Side());
            Costs.put(StartKey, 0.0);
            Open.add(new Entry(0.0, Start));
            double StartValue = target(Start) ? value(Start, 0.0, Wanted, Threat) : Double.NEGATIVE_INFINITY;
            Node Best = null;
            double BestValue = Double.NEGATIVE_INFINITY;
            int Settled = 0;
            while (!Open.isEmpty() && Settled < MaxSettled) {
                Entry Here = Open.poll();
                Key HereKey = new Key(Here.At().Cell(), Here.At().Side());
                if (Here.Cost() > Costs.getOrDefault(HereKey, Double.POSITIVE_INFINITY) + 1.0e-9) {
                    continue;
                }
                Settled++;
                if (Here.At() != Start && target(Here.At())) {
                    double Value = value(Here.At(), Here.Cost(), Wanted, Threat);
                    if (Value > BestValue) {
                        BestValue = Value;
                        Best = Here.At();
                    }
                }
                for (Node Next : neighbours(Here.At())) {
                    if (chebyshev(Next.Cell(), Start.Cell()) > Radius) {
                        continue;
                    }
                    if (light(Next.Cell()) < MinLight || !clear(Here.At().Position(), Next.Position())) {
                        continue;
                    }
                    double Cost = Here.Cost() + Here.At().Position().distanceTo(Next.Position()) + (Next.Side() != Here.At().Side() ? TurnCost : 0.0);
                    Key NextKey = new Key(Next.Cell(), Next.Side());
                    if (Cost + 1.0e-9 < Costs.getOrDefault(NextKey, Double.POSITIVE_INFINITY)) {
                        Costs.put(NextKey, Cost);
                        Parents.put(NextKey, Here.At());
                        Open.add(new Entry(Cost, Next));
                    }
                }
            }
            if (Best == null) {
                return List.of();
            }
            if (Wanted == Goal.Calm && (BestValue < MinScore || BestValue < StartValue + Improvement)) {
                return List.of();
            }
            if (Wanted == Goal.Flee && Threat != null && Best.Position().distanceTo(Threat) < Start.Position().distanceTo(Threat) + FleeMargin) {
                return List.of();
            }
            List<Node> Route = new ArrayList<>();
            Node Walk = Best;
            while (Walk != null && Walk != Start) {
                Route.add(Walk);
                Walk = Parents.get(new Key(Walk.Cell(), Walk.Side()));
            }
            Collections.reverse(Route);
            return Route;
        }
    }

    public static int chebyshev(BlockPos First, BlockPos Second) {
        return Math.max(Math.abs(First.getX() - Second.getX()), Math.max(Math.abs(First.getY() - Second.getY()), Math.abs(First.getZ() - Second.getZ())));
    }
}
