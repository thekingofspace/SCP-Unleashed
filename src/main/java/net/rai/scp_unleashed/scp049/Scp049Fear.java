package net.rai.scp_unleashed.scp049;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class Scp049Fear {
    public static final double HandRange = 8.0;
    public static final double DropRange = 6.0;
    public static final double WatchRange = 16.0;
    public static final double ShunRadius = 10.0;
    public static final double BerthRadius = 8.0;
    public static final double ShunVertical = 4.0;
    public static final int ShunTicks = 600;
    public static final int BerthTicks = 600;
    public static final double ForgetRange = 64.0;
    public static final int MaxSpots = 8;

    public record Source(Entity Body, boolean Dropped, double Distance) {
    }

    public static final class Spot {
        private final UUID Id;
        private final boolean Dropped;
        private Vec3 Centre;
        private int Until;

        private Spot(UUID Id, boolean Dropped, Vec3 Centre, int Until) {
            this.Id = Id;
            this.Dropped = Dropped;
            this.Centre = Centre;
            this.Until = Until;
        }

        public Vec3 centre() {
            return Centre;
        }

        public boolean dropped() {
            return Dropped;
        }

        public double radius() {
            return Dropped ? BerthRadius : ShunRadius;
        }
    }

    private final Scp049Doctor Owner;
    private final List<Spot> Spots = new ArrayList<>();

    public Scp049Fear(Scp049Doctor Owner) {
        this.Owner = Owner;
    }

    public static boolean repels(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(Scp049Registry.Repellent);
    }

    public static boolean isBearer(@Nullable Entity Found) {
        if (!(Found instanceof LivingEntity Body) || !Body.isAlive() || Body.isRemoved() || Body instanceof Scp049Doctor || Body instanceof Scp049Cured) {
            return false;
        }
        if (Body instanceof Player User) {
            if (User.isSpectator()) {
                return false;
            }
        } else if (!(Body instanceof AbstractVillager) && !Body.getType().is(Scp049Registry.PersonLike)) {
            return false;
        }
        return repels(Body.getMainHandItem()) || repels(Body.getOffhandItem());
    }

    public static boolean isRepellentDrop(@Nullable Entity Found) {
        return Found instanceof ItemEntity Drop && Drop.isAlive() && !Drop.isRemoved() && repels(Drop.getItem());
    }

    public static boolean stillRepels(@Nullable Entity Found, boolean Dropped) {
        return Dropped ? isRepellentDrop(Found) : isBearer(Found);
    }

    public int count() {
        return Spots.size();
    }

    public void clear() {
        Spots.clear();
    }

    @Nullable
    public Source scan(ServerLevel World) {
        int Now = Owner.tickCount;
        AABB Box = Owner.getBoundingBox().inflate(WatchRange);
        Source Nearest = null;
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, Box, Scp049Fear::isBearer)) {
            double Distance = Owner.distanceTo(Body);
            if (Distance > WatchRange || !Owner.hasLineOfSight(Body)) {
                continue;
            }
            remember(Body, false, Now);
            if (Distance <= HandRange && (Nearest == null || Distance < Nearest.Distance())) {
                Nearest = new Source(Body, false, Distance);
            }
        }
        for (ItemEntity Drop : World.getEntitiesOfClass(ItemEntity.class, Box, Scp049Fear::isRepellentDrop)) {
            double Distance = Owner.distanceTo(Drop);
            if (Distance > WatchRange || !Owner.hasLineOfSight(Drop)) {
                continue;
            }
            remember(Drop, true, Now);
            if (Distance <= DropRange && (Nearest == null || Distance < Nearest.Distance())) {
                Nearest = new Source(Drop, true, Distance);
            }
        }
        tidy(World, Now);
        return Nearest;
    }

    private void remember(Entity Body, boolean Dropped, int Now) {
        int Until = Now + (Dropped ? BerthTicks : ShunTicks);
        for (Spot Each : Spots) {
            if (Each.Id.equals(Body.getUUID())) {
                Each.Centre = Body.position();
                Each.Until = Math.max(Each.Until, Until);
                return;
            }
        }
        Spots.add(new Spot(Body.getUUID(), Dropped, Body.position(), Until));
        while (Spots.size() > MaxSpots) {
            Spot Oldest = Spots.get(0);
            for (Spot Each : Spots) {
                if (Each.Until < Oldest.Until) {
                    Oldest = Each;
                }
            }
            Spots.remove(Oldest);
        }
    }

    private void tidy(ServerLevel World, int Now) {
        Iterator<Spot> Each = Spots.iterator();
        while (Each.hasNext()) {
            Spot Entry = Each.next();
            Entity Body = World.getEntity(Entry.Id);
            if (Entry.Dropped) {
                if (Entry.Until < Now || !isRepellentDrop(Body) || Body.position().distanceTo(Owner.position()) > ForgetRange) {
                    Each.remove();
                    continue;
                }
                Entry.Centre = Body.position();
                continue;
            }
            if (Entry.Until < Now || Entry.Centre.distanceTo(Owner.position()) > ForgetRange) {
                Each.remove();
                continue;
            }
            if (Body instanceof LivingEntity Bearer && !isBearer(Bearer) && Owner.hasLineOfSight(Bearer)) {
                Each.remove();
            }
        }
    }

    public boolean shuns(int X, int Y, int Z) {
        if (Spots.isEmpty()) {
            return false;
        }
        return inside(X + 0.5, Y, Z + 0.5);
    }

    public boolean covers(Vec3 Point) {
        return covering(Point) != null;
    }

    @Nullable
    public Spot covering(Vec3 Point) {
        Spot Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Spot Each : Spots) {
            double Radius = Each.radius();
            double Dx = Point.x - Each.Centre.x;
            double Dz = Point.z - Each.Centre.z;
            double Distance = Dx * Dx + Dz * Dz;
            if (Math.abs(Point.y - Each.Centre.y) <= ShunVertical && Distance < Radius * Radius && Distance < BestDistance) {
                BestDistance = Distance;
                Best = Each;
            }
        }
        return Best;
    }

    private boolean inside(double X, double Y, double Z) {
        for (Spot Each : Spots) {
            double Radius = Each.radius();
            double Ox = Owner.getX() - Each.Centre.x;
            double Oz = Owner.getZ() - Each.Centre.z;
            if (Ox * Ox + Oz * Oz < Radius * Radius) {
                continue;
            }
            double Dx = X - Each.Centre.x;
            double Dz = Z - Each.Centre.z;
            if (Math.abs(Y - Each.Centre.y) <= ShunVertical && Dx * Dx + Dz * Dz < Radius * Radius) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Spot nearest(Vec3 From) {
        Spot Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Spot Each : Spots) {
            double Distance = Each.Centre.distanceToSqr(From);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Each;
            }
        }
        return Best;
    }
}
