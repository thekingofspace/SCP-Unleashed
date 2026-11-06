package net.rai.scp_unleashed.space;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class CraterFeature extends Feature<CraterConfiguration> {
    public static final int ChunkReach = 16;
    public static final int FloorMargin = 8;
    public static final double OutlineSlack = 1.15;
    public static final double RimFalloff = 0.35;
    public static final double FreshChance = 0.3;
    public static final int[] Harmonics = {2, 3, 5};
    public static final double[] Wobbles = {0.06, 0.04, 0.03};

    public CraterFeature(Codec<CraterConfiguration> Codec) {
        super(Codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CraterConfiguration> Context) {
        WorldGenLevel World = Context.level();
        RandomSource Random = Context.random();
        CraterConfiguration Config = Context.config();
        BlockPos Origin = Context.origin();
        int LocalX = Origin.getX() & 15;
        int LocalZ = Origin.getZ() & 15;
        int Allowed = Math.min(Math.min(LocalX + ChunkReach, ChunkReach * 2 - 1 - LocalX), Math.min(LocalZ + ChunkReach, ChunkReach * 2 - 1 - LocalZ));
        double Radius = Config.Radius().sample(Random) + Random.nextDouble() * 0.8;
        Radius = Math.min(Radius, (Allowed - 1) / (Config.Ejecta() * OutlineSlack));
        if (Radius < 1.5) {
            return false;
        }
        int Extent = Math.min(Allowed - 1, Mth.ceil(Radius * Config.Ejecta() * OutlineSlack));
        int BaseY = World.getHeight(Heightmap.Types.WORLD_SURFACE_WG, Origin.getX(), Origin.getZ()) - 1;
        int MinY = World.getMinBuildHeight() + FloorMargin;
        double Depth = Math.max(1.0, Radius * Config.Depth());
        double Rim = Radius >= 3.0 ? Math.max(1.0, Radius * Config.Rim()) : Radius * Config.Rim();
        boolean Fresh = Random.nextDouble() < FreshChance;
        double[] Phases = new double[Harmonics.length];
        for (int Index = 0; Index < Phases.length; Index++) {
            Phases[Index] = Random.nextDouble() * Math.PI * 2.0;
        }
        BlockState Dust = SpaceRegistry.LunarDust.get().defaultBlockState();
        BlockState Regolith = SpaceRegistry.LunarRegolith.get().defaultBlockState();
        BlockState Stone = SpaceRegistry.LunarStone.get().defaultBlockState();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Extent; Dx <= Extent; Dx++) {
            for (int Dz = -Extent; Dz <= Extent; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Distance > Extent) {
                    continue;
                }
                double Angle = Math.atan2(Dz, Dx);
                double Edge = Radius;
                for (int Index = 0; Index < Harmonics.length; Index++) {
                    Edge += Radius * Wobbles[Index] * Math.sin(Harmonics[Index] * Angle + Phases[Index]);
                }
                double Ratio = Distance / Edge;
                if (Ratio > Config.Ejecta()) {
                    continue;
                }
                int X = Origin.getX() + Dx;
                int Z = Origin.getZ() + Dz;
                int Top = World.getHeight(Heightmap.Types.WORLD_SURFACE_WG, X, Z) - 1;
                double Target;
                if (Ratio < 1.0) {
                    double Blend = Ratio * Ratio * Ratio * Ratio;
                    Target = BaseY + (Top - BaseY) * Blend - Depth * (1.0 - Ratio * Ratio) + Rim * Math.pow(Ratio, 6.0);
                } else {
                    double Out = (Ratio - 1.0) / RimFalloff;
                    Target = Top + Rim * Math.exp(-Out * Out);
                }
                int NewTop = Math.max(MinY, Mth.floor(Target + 0.5));
                if (NewTop < Top) {
                    for (int Y = NewTop + 1; Y <= Top; Y++) {
                        World.setBlock(Cursor.set(X, Y, Z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                } else if (NewTop > Top) {
                    for (int Y = Top + 1; Y < NewTop; Y++) {
                        World.setBlock(Cursor.set(X, Y, Z), Regolith, Block.UPDATE_CLIENTS);
                    }
                }
                boolean Inside = Ratio < 1.0;
                BlockState Surface = Dust;
                if (Inside && Fresh && Ratio > 0.35 && Random.nextDouble() < 0.55) {
                    Surface = Regolith;
                } else if (Inside && Ratio < 0.4 && Radius >= 6.0 && Random.nextDouble() < 0.35) {
                    Surface = Regolith;
                } else if (!Inside) {
                    double Falloff = 1.0 - (Ratio - 1.0) / Math.max(0.01, Config.Ejecta() - 1.0);
                    if (Random.nextDouble() < (Fresh ? 0.55 : 0.25) * Falloff) {
                        Surface = Regolith;
                    }
                }
                if (NewTop != Top || Surface != Dust || Inside) {
                    World.setBlock(Cursor.set(X, NewTop, Z), Surface, Block.UPDATE_CLIENTS);
                    if (NewTop - 1 > MinY && !World.getBlockState(Cursor.set(X, NewTop - 1, Z)).is(SpaceRegistry.LunarStone.get())) {
                        World.setBlock(Cursor, Regolith, Block.UPDATE_CLIENTS);
                    }
                }
                double RockChance = Config.Rocks() * (Inside ? 0.15 : (1.0 - (Ratio - 1.0) / Math.max(0.01, Config.Ejecta() - 1.0))) * (Fresh ? 1.6 : 0.6);
                if (Ratio > 0.55 && Random.nextDouble() < RockChance * 0.07) {
                    World.setBlock(Cursor.set(X, NewTop + 1, Z), Stone, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }
}
