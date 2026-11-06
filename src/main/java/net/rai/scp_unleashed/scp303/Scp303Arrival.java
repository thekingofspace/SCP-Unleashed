package net.rai.scp_unleashed.scp303;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public final class Scp303Arrival {
    private Scp303Arrival() {
    }

    public static boolean realm(Level World) {
        return World.dimension().equals(Level.OVERWORLD) && AnomalyRealm.allows(World);
    }

    public static boolean arrive(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        if (!realm(World)) {
            return false;
        }
        Scp303Unlock.unlock(Found.Investigator());
        Scp303State State = Scp303State.get(World.getServer());
        Scp303Doorman Present = loaded(World);
        if (Present != null) {
            State.favour(Found.Investigator().getUUID());
            return true;
        }
        Vec3 Spot = Vec3.atBottomCenterOf(Found.Pos());
        Vec3 Toward = Found.Investigator().position().subtract(Spot);
        float Yaw = (float) (Math.atan2(Toward.z, Toward.x) * 180.0 / Math.PI) - 90.0f;
        Optional<Scp303Doorman> Made = place(World, Spot, Yaw, Found.Investigator().getUUID());
        return Made.isPresent();
    }

    @Nullable
    public static Scp303Doorman loaded(ServerLevel World) {
        Scp303State State = Scp303State.get(World.getServer());
        UUID Current = State.current();
        if (!State.isActive() || Current == null) {
            return null;
        }
        Entity Found = World.getEntity(Current);
        return Found instanceof Scp303Doorman Doorman && Doorman.isAlive() ? Doorman : null;
    }

    public static Optional<Scp303Doorman> place(ServerLevel World, Vec3 Spot, float Yaw, @Nullable UUID Favourite) {
        Scp303Doorman Doorman = Scp303Registry.Doorman.get().create(World);
        if (Doorman == null) {
            return Optional.empty();
        }
        Doorman.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Doorman.setYHeadRot(Yaw);
        Doorman.setYBodyRot(Yaw);
        Doorman.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        Scp303State State = Scp303State.get(World.getServer());
        if (Favourite != null) {
            State.favour(Favourite);
        }
        if (!World.addFreshEntity(Doorman)) {
            return Optional.empty();
        }
        return Optional.of(Doorman);
    }
}
