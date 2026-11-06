package net.rai.scp_unleashed.scp001day.gametest;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.scp001day.Scp001DayDestination;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;
import net.rai.scp_unleashed.scp001day.world.Scp001DayChunkGenerator;

public final class Scp001DayCityLevel {
    public static final String Run = Long.toHexString(System.currentTimeMillis());
    private static final ChunkProgressListener Quiet = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(ChunkPos Centre) {
        }

        @Override
        public void onStatusChange(ChunkPos Pos, @Nullable ChunkStatus Status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    private Scp001DayCityLevel() {
    }

    public record City(ResourceKey<Level> Key, ServerLevel World, GatewayDestination Destination) {
    }

    public static City city(MinecraftServer Server, String Name) {
        ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, Scp001DayRegistry.location("sdayvoid_" + Name + "_" + Run));
        return new City(Key, open(Server, Key), destination(Key));
    }

    public static GatewayDestination destination(ResourceKey<Level> Key) {
        ResourceLocation Id = Key.location();
        GatewayDestination Found = GatewayDestinations.get(Id);
        return Found != null ? Found : GatewayDestinations.register(Scp001DayDestination.make(Id, Key));
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceKey<Level>, ServerLevel> levels(MinecraftServer Server) {
        return (Map<ResourceKey<Level>, ServerLevel>) ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, Server, "levels");
    }

    public static ServerLevel open(MinecraftServer Server, ResourceKey<Level> Key) {
        ServerLevel Known = Server.getLevel(Key);
        if (Known != null) {
            return Known;
        }
        RegistryAccess Access = Server.registryAccess();
        Holder<DimensionType> Type = Access.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE, Scp001DayRegistry.location("scp001_day")));
        Holder<Biome> Ground = Access.registryOrThrow(Registries.BIOME).getHolderOrThrow(ResourceKey.create(Registries.BIOME, Scp001DayRegistry.location("scp001_day_city")));
        LevelStem Stem = new LevelStem(Type, new Scp001DayChunkGenerator(new FixedBiomeSource(Ground)));
        Executor Pool = ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, Server, "executor");
        LevelStorageSource.LevelStorageAccess Storage = ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, Server, "storageSource");
        long Seed = BiomeManager.obfuscateSeed(Server.getWorldData().worldGenOptions().seed());
        ServerLevel Made = new ServerLevel(Server, Pool, Storage, new DerivedLevelData(Server.getWorldData(), Server.getWorldData().overworldData()), Key, Stem, Quiet, false, Seed,
                ImmutableList.of(), false, Server.overworld().getRandomSequences());
        levels(Server).put(Key, Made);
        Server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(Made));
        return Made;
    }

    public static ServerLevel restart(MinecraftServer Server, ResourceKey<Level> Key) {
        ServerLevel Old = Server.getLevel(Key);
        if (Old != null) {
            Old.save(null, true, false);
            NeoForge.EVENT_BUS.post(new LevelEvent.Unload(Old));
            levels(Server).remove(Key);
            Server.markWorldsDirty();
        }
        Scp001DayPlan.forget();
        return open(Server, Key);
    }
}
