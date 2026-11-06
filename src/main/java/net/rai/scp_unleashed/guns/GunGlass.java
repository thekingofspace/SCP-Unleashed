package net.rai.scp_unleashed.guns;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GunGlass {
    public static final double RayTolerance = 1.0;
    public static final TagKey<Block> Shatterable = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gun_shatterable"));
    public static final TagKey<Block> Bulletproof = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "bulletproof"));
    public static final int MaxLayers = 4;
    public static final int MaxShattered = 24;
    public static final float BlockCarry = 0.7F;
    public static final float PaneCarry = 0.85F;
    public static final double Nudge = 1.0E-3;

    private GunGlass() {
    }

    public static boolean shatters(BlockState State) {
        return State.is(Shatterable) && !State.is(Bulletproof);
    }

    public static boolean mayShatter(ServerLevel World, Entity User, BlockPos Pos) {
        if (!(User instanceof ServerPlayer Gunner)) {
            return EventHooks.canEntityGrief(World, User);
        }
        return World.mayInteract(Gunner, Pos) && !Gunner.blockActionRestricted(World, Pos, Gunner.gameMode.getGameModeForPlayer());
    }

    public static boolean permitted(ServerLevel World, Entity User, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return shatters(State) && mayShatter(World, User, Pos) && (!(User instanceof ServerPlayer Gunner) || !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(World, Pos, State, Gunner)).isCanceled());
    }

    public static float carryOf(BlockState State) {
        return State.is(Tags.Blocks.GLASS_PANES) ? PaneCarry : BlockCarry;
    }

    public static Route trace(ServerLevel World, Entity User, Vec3 Eye, Vec3 End) {
        List<Shard> Shards = new ArrayList<>();
        Vec3 Heading = End.subtract(Eye).normalize();
        double Length = Eye.distanceToSqr(End);
        Vec3 From = Eye;
        while (true) {
            BlockHitResult Hit = World.clip(new ClipContext(From, End, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User));
            if (Hit.getType() == HitResult.Type.MISS) {
                return new Route(Shards, Hit, End, false);
            }
            if (!onRay(From, End, Hit.getLocation())) {
                return new Route(Shards, missAt(End, Heading), End, false);
            }
            BlockPos Pos = Hit.getBlockPos().immutable();
            BlockState State = World.getBlockState(Pos);
            if (!shatters(State) || !mayShatter(World, User, Pos)) {
                return new Route(Shards, Hit, Hit.getLocation(), false);
            }
            Shards.add(new Shard(Pos, Eye.distanceTo(Hit.getLocation()), carryOf(State), Hit.getLocation(), Hit.getDirection()));
            if (Shards.size() >= MaxLayers) {
                return new Route(Shards, Hit, Hit.getLocation(), true);
            }
            Optional<Vec3> Exit = new AABB(Pos).clip(End, Hit.getLocation());
            if (Exit.isEmpty()) {
                return new Route(Shards, missAt(End, Heading), End, false);
            }
            From = Exit.get().add(Heading.scale(Nudge));
            if (Eye.distanceToSqr(From) >= Length) {
                return new Route(Shards, missAt(End, Heading), End, false);
            }
        }
    }

    public static int shatter(ServerLevel World, Entity User, Collection<BlockPos> Shards) {
        int Broken = 0;
        for (BlockPos Pos : Shards) {
            if (Broken >= MaxShattered) {
                break;
            }
            if (shatters(World.getBlockState(Pos)) && World.destroyBlock(Pos, false, User)) {
                Broken++;
            }
        }
        return Broken;
    }

    public static void impact(ServerLevel World, Entity User, BlockHitResult Surface, float Damage) {
        BlockPos Pos = Surface.getBlockPos();
        if (permitted(World, User, Pos)) {
            World.destroyBlock(Pos, false, User);
            return;
        }
        strike(World, User, Surface, Damage);
    }

    public static void strike(ServerLevel World, Entity User, BlockHitResult Surface, float Damage) {
        BlockPos Pos = Surface.getBlockPos();
        BlockState State = World.getBlockState(Pos);
        if (Damage > 0.0F && State.getBlock() instanceof GunStruckBlock Struck) {
            Struck.onGunHit(World, Pos, State, Surface, User, Damage);
        }
    }

    public static boolean onRay(Vec3 From, Vec3 End, Vec3 Point) {
        if (!Double.isFinite(Point.x) || !Double.isFinite(Point.y) || !Double.isFinite(Point.z)) {
            return false;
        }
        Vec3 Span = End.subtract(From);
        double LengthSquared = Span.lengthSqr();
        double Along = LengthSquared <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, Point.subtract(From).dot(Span) / LengthSquared));
        return From.add(Span.scale(Along)).distanceToSqr(Point) <= RayTolerance * RayTolerance;
    }

    private static BlockHitResult missAt(Vec3 End, Vec3 Heading) {
        return BlockHitResult.miss(End, Direction.getNearest(-Heading.x, -Heading.y, -Heading.z), BlockPos.containing(End));
    }

    public record Shard(BlockPos Pos, double Distance, float Carry, Vec3 Point, Direction Face) {
    }

    public static final class Route {
        private final List<Shard> Shards;
        private final BlockHitResult Surface;
        private final Vec3 Limit;
        private final boolean Lodged;

        private Route(List<Shard> Shards, BlockHitResult Surface, Vec3 Limit, boolean Lodged) {
            this.Shards = Shards;
            this.Surface = Surface;
            this.Limit = Limit;
            this.Lodged = Lodged;
        }

        public Route settle(ServerLevel World, Entity User, double Stop, Map<BlockPos, Boolean> Verdicts) {
            for (int Index = 0; Index < Shards.size(); Index++) {
                Shard Piece = Shards.get(Index);
                if (Piece.Distance() >= Stop) {
                    return new Route(List.copyOf(Shards.subList(0, Index)), Surface, Limit, Lodged);
                }
                if (!Verdicts.computeIfAbsent(Piece.Pos(), Key -> permitted(World, User, Key))) {
                    return new Route(List.copyOf(Shards.subList(0, Index)), new BlockHitResult(Piece.Point(), Piece.Face(), Piece.Pos(), false), Piece.Point(), false);
                }
            }
            return this;
        }

        public float carryBefore(double Distance) {
            float Carry = 1.0F;
            for (Shard Piece : Shards) {
                if (Piece.Distance() < Distance) {
                    Carry *= Piece.Carry();
                }
            }
            return Carry;
        }

        public float carry() {
            return carryBefore(Double.MAX_VALUE);
        }

        public List<BlockPos> positions() {
            List<BlockPos> Found = new ArrayList<>(Shards.size());
            for (Shard Piece : Shards) {
                Found.add(Piece.Pos());
            }
            return Found;
        }

        public List<Shard> shards() {
            return Shards;
        }

        public BlockHitResult surface() {
            return Surface;
        }

        public Vec3 limit() {
            return Limit;
        }

        public boolean lodged() {
            return Lodged;
        }

        public boolean solid() {
            return !Lodged && Surface.getType() != HitResult.Type.MISS;
        }
    }
}
