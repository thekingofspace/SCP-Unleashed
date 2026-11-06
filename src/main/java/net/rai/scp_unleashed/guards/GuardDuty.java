package net.rai.scp_unleashed.guards;

import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.duty.DClassDuties;
import net.rai.scp_unleashed.dclass.duty.DClassDuty;
import net.rai.scp_unleashed.dclass.duty.DClassTask;
import org.jetbrains.annotations.Nullable;

public final class GuardDuty {
    private GuardDuty() {
    }

    @Nullable
    public static GuardOrders orders(Entity Body) {
        GuardOrders Found = Body.getExistingDataOrNull(GuardsRegistry.Orders.get());
        return Found != null && Found.valid() ? Found : null;
    }

    public static boolean has(Entity Body) {
        return orders(Body) != null;
    }

    public static boolean isGuard(Entity Body) {
        return has(Body);
    }

    @Nullable
    public static PatrolRoute route(Entity Body) {
        GuardOrders Found = orders(Body);
        return Found == null ? null : Found.route();
    }

    public static boolean sameRoute(Entity First, Entity Second) {
        PatrolRoute Left = route(First);
        PatrolRoute Right = route(Second);
        return Left != null && Right != null && Left.RouteId().equals(Right.RouteId());
    }

    public static void assign(DClass Guard, PatrolRoute Route) {
        DClassDuties.clear(Guard);
        GuardOrders Next = GuardOrders.of(Route);
        Next.setIndex(Next.nearest(Guard));
        Next.setStatus(Route.single() ? GuardStatus.Standing : GuardStatus.Returning);
        Guard.setData(GuardsRegistry.Orders.get(), Next);
        Guard.wake();
        Guard.getNavigation().stop();
        Guard.setTarget(null);
    }

    public static boolean clear(DClass Guard) {
        if (!Guard.hasData(GuardsRegistry.Orders.get())) {
            return false;
        }
        boolean Had = has(Guard);
        Guard.removeData(GuardsRegistry.Orders.get());
        Guard.getNavigation().stop();
        Guard.setTarget(null);
        return Had;
    }

    public static boolean pauses(DClassTask Task) {
        return Task == DClassTask.EatThis;
    }

    public static void yieldTo(DClass Guard, DClassDuty Duty) {
        if (Duty.Role().active() || Duty.Task().active() && !pauses(Duty.Task())) {
            clear(Guard);
        }
    }
}
