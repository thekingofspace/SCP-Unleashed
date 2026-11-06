package net.rai.scp_unleashed.scp205;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Scp205Light {
    public static final int Threshold = 10;

    private Scp205Light() {
    }

    public static int at(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return 0;
        }
        return World.getMaxLocalRawBrightness(Pos);
    }

    public static boolean bright(Level World, BlockPos Pos) {
        return at(World, Pos) >= Threshold;
    }

    public static boolean bright(Level World, Vec3 Point) {
        return bright(World, BlockPos.containing(Point));
    }

    public static boolean bright(Entity Subject) {
        return bright(Subject.level(), Subject.blockPosition());
    }
}
