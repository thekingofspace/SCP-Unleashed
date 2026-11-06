package net.rai.scp_unleashed.scp017;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class Scp017Light {
    public static final int Threshold = 10;

    private Scp017Light() {
    }

    public static int at(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return 0;
        }
        return World.getMaxLocalRawBrightness(Pos);
    }

    public static boolean isLit(Level World, BlockPos Pos) {
        return at(World, Pos) > Threshold;
    }

    public static int of(Entity Subject) {
        return at(Subject.level(), Subject.blockPosition());
    }

    public static boolean inLight(Entity Subject) {
        return of(Subject) > Threshold;
    }
}
