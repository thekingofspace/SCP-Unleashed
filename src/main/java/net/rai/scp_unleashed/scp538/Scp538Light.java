package net.rai.scp_unleashed.scp538;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public final class Scp538Light {
    public static final int Fading = 8;
    public static final int Calming = 12;

    private Scp538Light() {
    }

    public static int at(Level World, BlockPos Cell) {
        return World.getMaxLocalRawBrightness(Cell);
    }

    public static int artificial(Level World, BlockPos Cell) {
        return World.getBrightness(LightLayer.BLOCK, Cell);
    }

    public static int artificial(Entity Holder) {
        return artificial(Holder.level(), cellOf(Holder));
    }

    public static boolean calmed(int Light, int Artificial) {
        return bearable(Light) && calming(Artificial);
    }

    public static BlockPos cellOf(Entity Holder) {
        return BlockPos.containing(Holder.getX(), Holder.getY() + Holder.getBbHeight() * 0.5, Holder.getZ());
    }

    public static int at(Entity Holder) {
        return at(Holder.level(), cellOf(Holder));
    }

    public static boolean bearable(int Light) {
        return Light >= Fading;
    }

    public static boolean calming(int Light) {
        return Light >= Calming;
    }

    public static boolean restless(int Light) {
        return Light >= Fading && Light < Calming;
    }
}
