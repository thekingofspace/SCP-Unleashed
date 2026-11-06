package net.rai.scp_unleashed.scp303;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.scp173.Scp173Witness;
import net.rai.scp_unleashed.scp268.Scp268;

import java.util.List;

public final class Scp303Sight {
    public static final double ViewCone = 0.35;
    public static final double SightRange = 96.0;
    public static final double PersonRange = Scp173Witness.Range;
    public static final double DarkDistance = 12.0;
    public static final int DarkLight = 3;

    private Scp303Sight() {
    }

    public static boolean viewer(Player Viewer) {
        return Viewer.isAlive() && Targeting.canWitness(Viewer) && !Viewer.isSpectator() && !Scp268.isNeglected(Viewer);
    }

    public static boolean kind(Entity Found) {
        return ControlBodies.isPersonLike(Found) || Found instanceof HostileRaider;
    }

    public static boolean person(Entity Found) {
        if (Found instanceof Player User) {
            return viewer(User);
        }
        return Found instanceof LivingEntity Living && Living.isAlive() && Targeting.canWitness(Living) && !(Found instanceof Scp303Doorman) && kind(Found);
    }

    public static boolean watcher(LivingEntity Viewer) {
        if (Viewer instanceof Player User) {
            return viewer(User);
        }
        if (!person(Viewer) || Viewer.isRemoved() || Viewer.isSleeping() || ControlSessions.isBoundToSession(Viewer)) {
            return false;
        }
        return !(Viewer instanceof DClassLike Body) || !Body.eyesShut() && !Body.isPuppet() && !Body.isSpecimen();
    }

    public static boolean clear(Level World, Vec3 From, Vec3 To, Entity Viewer) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer)).getType() == HitResult.Type.MISS;
    }

    public static boolean facing(LivingEntity Viewer, Vec3 Point, double Cone) {
        Vec3 Toward = Point.subtract(CameraViews.eye(Viewer));
        double Length = Toward.length();
        if (Length < 1.0E-4) {
            return true;
        }
        return Toward.scale(1.0 / Length).dot(CameraViews.look(Viewer)) >= Cone;
    }

    public static Vec3[] marks(Vec3 Feet, double Height) {
        return new Vec3[]{Feet.add(0.0, Height * 0.9, 0.0), Feet.add(0.0, Height * 0.55, 0.0), Feet.add(0.0, Height * 0.15, 0.0)};
    }

    public static AABB box(Vec3 Feet, double Height) {
        double Half = Scp303Doorman.Width / 2.0;
        return new AABB(Feet.x - Half, Feet.y, Feet.z - Half, Feet.x + Half, Feet.y + Height, Feet.z + Half);
    }

    public static double reach(LivingEntity Viewer) {
        return Viewer instanceof Player ? SightRange : Scp173Witness.reach(Viewer);
    }

    public static boolean inLine(LivingEntity Viewer, Vec3 Feet, double Height) {
        double Reach = reach(Viewer);
        if (CameraViews.origin(Viewer).distanceToSqr(Feet) > Reach * Reach) {
            return false;
        }
        Vec3 Eye = CameraViews.eye(Viewer);
        for (Vec3 Mark : marks(Feet, Height)) {
            if (clear(Viewer.level(), Eye, Mark, Viewer)) {
                return true;
            }
        }
        return false;
    }

    public static boolean sees(LivingEntity Viewer, Vec3 Feet, double Height) {
        if (!(Viewer instanceof Player User)) {
            return watcher(Viewer) && Scp173Witness.canSee(Viewer, box(Feet, Height));
        }
        if (!viewer(User) || CameraViews.origin(User).distanceToSqr(Feet) > SightRange * SightRange) {
            return false;
        }
        Vec3 Eye = CameraViews.eye(User);
        for (Vec3 Mark : marks(Feet, Height)) {
            if (facing(User, Mark, ViewCone) && clear(User.level(), Eye, Mark, User)) {
                return true;
            }
        }
        return false;
    }

    public static boolean sees(LivingEntity Viewer, Entity Watched) {
        return Viewer.level() == Watched.level() && sees(Viewer, Watched.position(), Watched.getBbHeight());
    }

    public static List<LivingEntity> watchers(ServerLevel World, Vec3 Feet, double Range) {
        return World.getEntitiesOfClass(LivingEntity.class, new AABB(Feet, Feet).inflate(Range), Viewer -> !(Viewer instanceof Player) && watcher(Viewer));
    }

    public static boolean peopleSee(ServerLevel World, Vec3 Feet, double Height) {
        AABB Box = box(Feet, Height);
        for (LivingEntity Viewer : watchers(World, Feet, PersonRange + Height)) {
            if (Scp173Witness.canSee(Viewer, Box)) {
                return true;
            }
        }
        return false;
    }

    public static boolean seen(Entity Watched) {
        if (!(Watched.level() instanceof ServerLevel World)) {
            return false;
        }
        return watched(World, Watched.position(), Watched.getBbHeight());
    }

    public static boolean watched(ServerLevel World, Vec3 Feet, double Height) {
        for (Player Viewer : World.players()) {
            if (sees(Viewer, Feet, Height)) {
                return true;
            }
        }
        return peopleSee(World, Feet, Height);
    }

    public static boolean exposed(ServerLevel World, Vec3 Feet, double Height) {
        for (Player Viewer : World.players()) {
            if (viewer(Viewer) && inLine(Viewer, Feet, Height)) {
                return true;
            }
        }
        return peopleSee(World, Feet, Height);
    }

    public static boolean shaded(Level World, Vec3 Feet, LivingEntity Viewer) {
        return Viewer.position().distanceTo(Feet) >= DarkDistance && World.getMaxLocalRawBrightness(BlockPos.containing(Feet.add(0.0, 1.0, 0.0))) <= DarkLight;
    }
}
