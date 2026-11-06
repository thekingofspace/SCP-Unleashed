package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class Scp1730Defences {
    public static final double Point = 3.0;
    public static final double MinReach = 6.0;
    public static final float Share = 0.9f;

    private Scp1730Defences() {
    }

    public static double reach(float Radius) {
        return Math.max(MinReach, Radius * 2.0);
    }

    public static float falloff(double Distance, double Reach, float Full) {
        if (Distance <= Point) {
            return Float.MAX_VALUE;
        }
        if (Distance >= Reach) {
            return 0.0f;
        }
        return (float) (Full * Share * (1.0 - (Distance - Point) / (Reach - Point)));
    }

    public static void blast(ServerLevel World, Vec3 Centre, float Radius) {
        double Reach = reach(Radius);
        BlockPos Middle = BlockPos.containing(Centre);
        ChunkPos Chunk0 = new ChunkPos(Middle.offset((int) -Math.ceil(Reach), 0, (int) -Math.ceil(Reach)));
        ChunkPos Chunk1 = new ChunkPos(Middle.offset((int) Math.ceil(Reach), 0, (int) Math.ceil(Reach)));
        List<BlockEntity> Found = new ArrayList<>();
        for (int Cx = Chunk0.x; Cx <= Chunk1.x; Cx++) {
            for (int Cz = Chunk0.z; Cz <= Chunk1.z; Cz++) {
                if (!World.hasChunk(Cx, Cz)) {
                    continue;
                }
                LevelChunk Column = World.getChunk(Cx, Cz);
                for (BlockEntity Each : Column.getBlockEntities().values()) {
                    if ((Each instanceof Scp1730TurretBlockEntity || Each instanceof Scp1730LaserBlockEntity) && Vec3.atCenterOf(Each.getBlockPos()).distanceTo(Centre) < Reach) {
                        Found.add(Each);
                    }
                }
            }
        }
        for (BlockEntity Each : Found) {
            Vec3 Middle0 = Vec3.atCenterOf(Each.getBlockPos());
            double Distance = Middle0.distanceTo(Centre);
            if (Each instanceof Scp1730TurretBlockEntity Turret) {
                float Amount = falloff(Distance, Reach, Scp1730TurretBlockEntity.MaxHealth);
                if (Amount == Float.MAX_VALUE) {
                    Turret.wreck(World);
                } else {
                    Turret.damage(World, Amount, Middle0);
                }
            } else if (Each instanceof Scp1730LaserBlockEntity Laser) {
                float Amount = falloff(Distance, Reach, Scp1730LaserBlockEntity.MaxHealth);
                if (Amount == Float.MAX_VALUE) {
                    Laser.disable(World);
                } else {
                    Laser.damage(World, Amount, Middle0);
                }
            }
        }
    }
}
