package net.rai.scp_unleashed.chunkloader;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class ChunkLoaderConfig {
    public static final int DefaultMaxRadius = 16;
    public static final int DefaultBasePower = 4;
    public static final int DefaultPowerPerChunk = 2;
    public static final int DefaultGraceSeconds = 5;
    public static final int DefaultChunksPerTick = 2;
    public static ModConfigSpec.IntValue MaxRadius;
    public static ModConfigSpec.IntValue BasePower;
    public static ModConfigSpec.IntValue PowerPerChunk;
    public static ModConfigSpec.IntValue GraceSeconds;
    public static ModConfigSpec.IntValue ChunksPerTick;

    public static void define(ModConfigSpec.Builder Builder) {
        MaxRadius = Builder.comment("Largest radius in chunks a chunk loader may be set to. Radius 0 is just its own chunk, radius r holds a (2r+1) x (2r+1) square.")
                .translation("config.scp_unleashed.chunk_loader.max_radius").defineInRange("maxRadius", DefaultMaxRadius, 0, ChunkLoaderArea.HardMaxRadius);
        BasePower = Builder.comment("Flat SP/t every running chunk loader draws.")
                .translation("config.scp_unleashed.chunk_loader.base_power").defineInRange("basePower", DefaultBasePower, 0, 100000);
        PowerPerChunk = Builder.comment("Extra SP/t drawn for every chunk a loader holds.")
                .translation("config.scp_unleashed.chunk_loader.power_per_chunk").defineInRange("powerPerChunk", DefaultPowerPerChunk, 0, 10000);
        GraceSeconds = Builder.comment("Seconds a loader keeps its chunks while short of power before letting them go.")
                .translation("config.scp_unleashed.chunk_loader.grace_seconds").defineInRange("graceSeconds", DefaultGraceSeconds, 0, 60);
        ChunksPerTick = Builder.comment("How many new chunks one loader may pull in per tick while it spins up. Lower is gentler on the server.")
                .translation("config.scp_unleashed.chunk_loader.chunks_per_tick").defineInRange("chunksPerTick", DefaultChunksPerTick, 1, 32);
    }

    private ChunkLoaderConfig() {
    }

    private static int value(ModConfigSpec.IntValue Value) {
        return ScpConfigs.Server.isLoaded() ? Value.getAsInt() : Value.getDefault();
    }

    public static int maxRadius() {
        return Math.min(ChunkLoaderArea.HardMaxRadius, Math.max(0, value(MaxRadius)));
    }

    public static int basePower() {
        return value(BasePower);
    }

    public static int powerPerChunk() {
        return value(PowerPerChunk);
    }

    public static int graceTicks() {
        return value(GraceSeconds) * 20;
    }

    public static int chunksPerTick() {
        return Math.max(1, value(ChunksPerTick));
    }

    public static int demand(int Radius) {
        return ChunkLoaderArea.demand(Radius, basePower(), powerPerChunk());
    }
}
