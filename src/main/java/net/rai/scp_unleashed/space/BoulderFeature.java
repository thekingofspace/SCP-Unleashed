package net.rai.scp_unleashed.space;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;

public class BoulderFeature extends Feature<CountConfiguration> {
    public static final int MaxRadius = 4;

    public BoulderFeature(Codec<CountConfiguration> Codec) {
        super(Codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CountConfiguration> Context) {
        WorldGenLevel World = Context.level();
        RandomSource Random = Context.random();
        int Radius = Math.max(1, Math.min(MaxRadius, Context.config().count().sample(Random)));
        BlockPos Origin = Context.origin();
        int Ground = World.getHeight(Heightmap.Types.WORLD_SURFACE_WG, Origin.getX(), Origin.getZ()) - 1;
        if (World.isEmptyBlock(new BlockPos(Origin.getX(), Ground, Origin.getZ()))) {
            return false;
        }
        double Wide = Radius + 0.35 + Random.nextDouble() * 0.5;
        double Long = Radius + 0.35 + Random.nextDouble() * 0.9;
        double Tall = Radius * (0.7 + Random.nextDouble() * 0.3) + 0.3;
        double Turn = Random.nextDouble() * Math.PI;
        double Cos = Math.cos(Turn);
        double Sin = Math.sin(Turn);
        double CentreY = Ground + Tall * 0.35;
        BlockState Stone = SpaceRegistry.LunarStone.get().defaultBlockState();
        BlockState Regolith = SpaceRegistry.LunarRegolith.get().defaultBlockState();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        int Reach = Radius + 2;
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double U = (Dx * Cos + Dz * Sin) / Long;
                double V = (-Dx * Sin + Dz * Cos) / Wide;
                for (int Dy = -Reach; Dy <= Reach; Dy++) {
                    double W = (Ground + Dy - CentreY) / Tall;
                    double Shape = U * U + V * V + W * W;
                    if (Shape <= 1.0 + (Random.nextDouble() - 0.5) * 0.25) {
                        Cursor.set(Origin.getX() + Dx, Ground + Dy, Origin.getZ() + Dz);
                        World.setBlock(Cursor, Shape > 0.8 && Dy <= 0 && Random.nextDouble() < 0.4 ? Regolith : Stone, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return true;
    }
}
