package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp610Spawning {
    public static final int RoomSearch = 3;

    private Scp610Spawning() {
    }

    @Nullable
    public static Scp610Creature raise(ServerLevel World, Scp610Form Form, Vec3 At, float Yaw, boolean Emerging) {
        EntityType<? extends Scp610Creature> Type = Scp610Registry.typeOf(Form);
        Scp610Creature Risen = Type.create(World);
        if (Risen == null) {
            return null;
        }
        Vec3 Spot = Form.mobile() ? room(World, Risen, At) : At;
        Risen.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Risen.setYHeadRot(Yaw);
        Risen.setYBodyRot(Yaw);
        Risen.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        if (!World.addFreshEntity(Risen)) {
            return null;
        }
        if (Emerging) {
            Risen.beginEmerge();
        }
        return Risen;
    }

    public static Vec3 room(ServerLevel World, Scp610Creature Risen, Vec3 At) {
        if (fits(World, Risen, At)) {
            return At;
        }
        for (int Reach = 1; Reach <= RoomSearch; Reach++) {
            for (int Dy = 0; Dy <= Reach; Dy++) {
                for (int Dx = -Reach; Dx <= Reach; Dx++) {
                    for (int Dz = -Reach; Dz <= Reach; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Reach && Dy != Reach) {
                            continue;
                        }
                        Vec3 Candidate = new Vec3(Math.floor(At.x + Dx) + 0.5, Math.floor(At.y) + Dy, Math.floor(At.z + Dz) + 0.5);
                        BlockPos Below = BlockPos.containing(Candidate).below();
                        if (fits(World, Risen, Candidate) && !World.getBlockState(Below).getCollisionShape(World, Below).isEmpty()) {
                            return Candidate;
                        }
                    }
                }
            }
        }
        return At;
    }

    private static boolean fits(ServerLevel World, Scp610Creature Risen, Vec3 Spot) {
        AABB Box = Risen.getType().getDimensions().makeBoundingBox(Spot);
        return World.noCollision(Risen, Box);
    }
}
