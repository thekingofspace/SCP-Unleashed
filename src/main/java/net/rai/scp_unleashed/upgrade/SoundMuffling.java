package net.rai.scp_unleashed.upgrade;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.Scp_unleashed;

public final class SoundMuffling {
    public static final float Factor = 0.2f;
    public static final double Reach = 0.25;
    public static final TagKey<Block> Muffleable = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "muffleable"));

    private SoundMuffling() {
    }

    public static boolean heard(SoundSource Source) {
        return Source == SoundSource.BLOCKS || Source == SoundSource.AMBIENT || Source == SoundSource.RECORDS;
    }

    public static boolean inside(LongSet Cells, double X, double Y, double Z) {
        return Cells.contains(BlockPos.asLong(Mth.floor(X), Mth.floor(Y), Mth.floor(Z)));
    }

    public static boolean near(LongSet Cells, double X, double Y, double Z) {
        for (int CellX = Mth.floor(X - Reach); CellX <= Mth.floor(X + Reach); CellX++) {
            for (int CellY = Mth.floor(Y - Reach); CellY <= Mth.floor(Y + Reach); CellY++) {
                for (int CellZ = Mth.floor(Z - Reach); CellZ <= Mth.floor(Z + Reach); CellZ++) {
                    if (Cells.contains(BlockPos.asLong(CellX, CellY, CellZ))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static float factor(LongSet Cells, double X, double Y, double Z, SoundSource Source) {
        if (Cells.isEmpty() || !heard(Source) || !Double.isFinite(X) || !Double.isFinite(Y) || !Double.isFinite(Z)) {
            return 1.0f;
        }
        return inside(Cells, X, Y, Z) || near(Cells, X, Y, Z) ? Factor : 1.0f;
    }

    public static float factor(ServerLevel World, double X, double Y, double Z, SoundSource Source) {
        MufflerData Data = MufflerData.peek(World);
        return Data == null ? 1.0f : factor(Data.cells(), X, Y, Z, Source);
    }
}
