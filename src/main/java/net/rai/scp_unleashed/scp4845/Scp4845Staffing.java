package net.rai.scp_unleashed.scp4845;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;

import java.util.List;

public final class Scp4845Staffing {
    public static final int CheckTicks = 20;
    public static final int RestaffTicks = 600;
    public static final double WatchRange = 96.0;
    public static final int KitchenCrew = 3;
    public static final int CounterLook = 1;
    public static final int[] KitchenLooks = {0, 2, 3};
    public static final int[] KitchenNames = {1, 4, 7};
    public static final int CounterName = 0;

    private static int ShortTicks;

    private Scp4845Staffing() {
    }

    public static boolean ready(ServerLevel World, Scp4845Site Site) {
        AABB Area = Site.Protected;
        for (int ChunkX = Mth.floor(Area.minX) >> 4; ChunkX <= Mth.floor(Area.maxX) >> 4; ChunkX++) {
            for (int ChunkZ = Mth.floor(Area.minZ) >> 4; ChunkZ <= Mth.floor(Area.maxZ) >> 4; ChunkZ++) {
                if (!World.areEntitiesLoaded(ChunkPos.asLong(ChunkX, ChunkZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean complete(ServerLevel World, Scp4845Site Site) {
        List<Scp4845Employee> Present = crew(World, Site);
        int Kitchen = 0;
        boolean Counter = false;
        boolean Manager = false;
        for (Scp4845Employee Worker : Present) {
            switch (Worker.role()) {
                case Scp4845Employee.RoleCounter -> Counter = true;
                case Scp4845Employee.RoleManager -> Manager = true;
                default -> Kitchen++;
            }
        }
        return Counter && Manager && Kitchen >= KitchenCrew;
    }

    public static List<Scp4845Employee> crew(ServerLevel World, Scp4845Site Site) {
        return World.getEntitiesOfClass(Scp4845Employee.class, Site.Protected.inflate(16.0), Worker -> Worker.isAlive() && Worker.isBound());
    }

    public static int restaff(ServerLevel World, Scp4845Site Site) {
        if (!ready(World, Site)) {
            return 0;
        }
        List<Scp4845Employee> Present = crew(World, Site);
        int Counters = 0;
        int Kitchen = 0;
        int Managers = 0;
        for (Scp4845Employee Worker : Present) {
            switch (Worker.role()) {
                case Scp4845Employee.RoleCounter -> Counters++;
                case Scp4845Employee.RoleManager -> Managers++;
                default -> Kitchen++;
            }
        }
        int Hired = 0;
        if (Counters == 0) {
            hire(World, Site, Site.CounterWorker.Pos(), Scp4845Employee.RoleCounter, CounterLook, CounterName);
            Hired++;
        }
        if (Managers == 0) {
            hire(World, Site, Site.Manager.Pos(), Scp4845Employee.RoleManager, 0, Scp4845Employee.ManagerName);
            Hired++;
        }
        for (int Index = Kitchen; Index < KitchenCrew; Index++) {
            BlockPos Spot = Present.isEmpty() && Index < Site.KitchenSpawns.size() ? Site.KitchenSpawns.get(Index) : Site.Restaff;
            hire(World, Site, Spot, Scp4845Employee.RoleKitchen, KitchenLooks[Index % KitchenLooks.length], KitchenNames[Index % KitchenNames.length]);
            Hired++;
        }
        return Hired;
    }

    private static void hire(ServerLevel World, Scp4845Site Site, BlockPos Spot, byte Role, int Look, int Name) {
        Scp4845Employee Worker = Scp4845Registry.Employee.get().create(World);
        if (Worker == null) {
            return;
        }
        Worker.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Site.front().getOpposite().toYRot(), 0.0f);
        Worker.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot), MobSpawnType.EVENT, null);
        Worker.setup(Role, Look, Name, true);
        World.addFreshEntity(Worker);
        Scp4845State.get(World.getServer()).hire(Worker.getUUID());
        if (Spot.equals(Site.Restaff)) {
            Scp4845Registry.playLayers(World, null, Spot.getX() + 0.5, Spot.getY() + 1.0, Spot.getZ() + 0.5, Scp4845Registry.RestockLayers, SoundSource.NEUTRAL, 0.6f, 0.8f);
        }
    }

    public static void tick(ServerLevel World) {
        if (World.getGameTime() % CheckTicks != 7) {
            return;
        }
        Scp4845Site Site = Scp4845Restaurant.in(World);
        if (Site == null) {
            return;
        }
        Player Watcher = World.getNearestPlayer(Site.Centre.getX() + 0.5, Site.Centre.getY(), Site.Centre.getZ() + 0.5, WatchRange, false);
        if (Watcher == null || !World.isLoaded(Site.Restaff) || !ready(World, Site)) {
            return;
        }
        Scp4845State Data = Scp4845State.get(World.getServer());
        if (!Data.staffed()) {
            restaff(World, Site);
            Data.markStaffed();
            ShortTicks = 0;
            return;
        }
        if (complete(World, Site)) {
            ShortTicks = 0;
            return;
        }
        ShortTicks += CheckTicks;
        if (ShortTicks >= RestaffTicks) {
            ShortTicks = 0;
            restaff(World, Site);
        }
    }

    public static void forget() {
        ShortTicks = 0;
    }
}
