package net.rai.scp_unleashed.scp106;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp106Spawning {
    private Scp106Spawning() {
    }

    @Nullable
    public static Scp106OldMan emerge(ServerLevel World, Vec3 Spot, float Yaw, MobSpawnType Reason) {
        Scp106OldMan Creature = Scp106Registry.OldMan.get().create(World);
        if (Creature == null) {
            return null;
        }
        Creature.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), Reason, null);
        Creature.emergeAt(World, Spot, Yaw);
        if (!World.addFreshEntity(Creature)) {
            return null;
        }
        return Creature;
    }
}
