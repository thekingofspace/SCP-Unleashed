package net.rai.scp_unleashed.guards;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class GuardOrders {
    public static final GuardOrders None = new GuardOrders(null, 0, false);
    public static final Codec<GuardOrders> OrdersCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            PatrolRoute.RouteCodec.optionalFieldOf("Route").forGetter(Saved -> Optional.ofNullable(Saved.Route)),
            Codec.INT.optionalFieldOf("Index", 0).forGetter(Saved -> Saved.Index),
            Codec.BOOL.optionalFieldOf("Reverse", false).forGetter(Saved -> Saved.Reverse)
    ).apply(Instance, (Route, Index, Reverse) -> new GuardOrders(Route.orElse(null), Index, Reverse)));

    @Nullable
    private final PatrolRoute Route;
    private int Index;
    private boolean Reverse;
    private GuardStatus Status = GuardStatus.Patrolling;

    public GuardOrders(@Nullable PatrolRoute Route, int Index, boolean Reverse) {
        this.Route = Route;
        this.Index = Route == null || Route.isEmpty() ? 0 : Math.floorMod(Index, Route.size());
        this.Reverse = Reverse;
    }

    public static GuardOrders blank() {
        return new GuardOrders(null, 0, false);
    }

    public static GuardOrders of(PatrolRoute Route) {
        return new GuardOrders(Route, 0, false);
    }

    public boolean valid() {
        return Route != null && !Route.isEmpty();
    }

    @Nullable
    public PatrolRoute route() {
        return Route;
    }

    public int index() {
        return Index;
    }

    public boolean reverse() {
        return Reverse;
    }

    public PatrolPoint current() {
        return Route.point(Index);
    }

    public boolean onDuty(Entity Body) {
        return valid() && Body.level().dimension().equals(Route.Dimension());
    }

    public GuardStatus status() {
        return Status;
    }

    public void setStatus(GuardStatus Next) {
        Status = Next;
    }

    public void setIndex(int Next) {
        if (valid()) {
            Index = Math.floorMod(Next, Route.size());
        }
    }

    public void advance() {
        if (!valid() || Route.single()) {
            return;
        }
        int Count = Route.size();
        if (!Route.PingPong()) {
            Index = (Index + 1) % Count;
            return;
        }
        if (Reverse) {
            Index--;
            if (Index < 0) {
                Reverse = false;
                Index = Math.min(1, Count - 1);
            }
        } else {
            Index++;
            if (Index >= Count) {
                Reverse = true;
                Index = Math.max(0, Count - 2);
            }
        }
    }

    public int nearest(Entity Body) {
        if (!valid()) {
            return 0;
        }
        int Best = 0;
        double BestDistance = Double.MAX_VALUE;
        for (int Point = 0; Point < Route.size(); Point++) {
            double Distance = Body.distanceToSqr(Vec3.atBottomCenterOf(Route.point(Point).Pos()));
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Point;
            }
        }
        return Best;
    }
}
