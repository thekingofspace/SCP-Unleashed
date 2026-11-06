package net.rai.scp_unleashed.microhid;

import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class MicroHidGlow {
    public static final double NearPlane = 0.06;
    public static final double RibbonPullLimit = 0.35;
    public static final double HandReach = 1.0;
    public static final double SeenSlack = 0.08;
    public static final float FabulousAlpha = 0.05F;
    public static final double SurfaceMargin = 0.08;

    private MicroHidGlow() {
    }

    public static Vec3 pulled(Vec3 Local, double Pull) {
        double Distance = Local.length();
        if (Distance < 1.0E-6) {
            return Local;
        }
        double Kept = Math.max(Distance - Math.max(0.0, Pull), Math.min(Distance, NearPlane));
        return Local.scale(Kept / Distance);
    }

    public static double ribbonPull(double HalfWidth) {
        return Mth.clamp(HalfWidth, 0.0, RibbonPullLimit);
    }

    public static double handEdge(double MuzzleDistance) {
        return MuzzleDistance + HandReach;
    }

    public static boolean inHand(double Distance, double Edge) {
        return Edge > 0.0 && Distance <= Edge;
    }

    public static double crossing(double From, double To, double Edge) {
        if (Math.abs(To - From) < 1.0E-9) {
            return 0.5;
        }
        return Mth.clamp((Edge - From) / (To - From), 0.0, 1.0);
    }

    public static Vec3 cleared(Vec3 Local, Vec3 Impact, Vec3 Normal) {
        double Height = -Impact.dot(Normal);
        double Along = Local.dot(Normal);
        if (Along >= 0.0 || Along + Height >= SurfaceMargin) {
            return Local;
        }
        double Distance = Local.length();
        double Scale = Height <= SurfaceMargin ? 0.0 : (SurfaceMargin - Height) / Along;
        double Kept = Math.max(Distance * Scale, Math.min(Distance, NearPlane));
        return Local.scale(Kept / Distance);
    }

    public static boolean seen(BlockGetter World, Vec3 Eye, Vec3 Point) {
        Vec3 Toward = Eye.subtract(Point);
        double Gap = Toward.length();
        if (Gap <= SeenSlack) {
            return true;
        }
        Vec3 Stop = Point.add(Toward.scale(SeenSlack / Gap));
        return World.clip(new ClipContext(Eye, Stop, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }
}
