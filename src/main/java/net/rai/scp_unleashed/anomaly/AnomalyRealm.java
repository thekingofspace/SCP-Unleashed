package net.rai.scp_unleashed.anomaly;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.rai.scp_unleashed.space.SpaceRegistry;
import net.rai.scp_unleashed.space.SpaceTags;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AnomalyRealm {
    private static final Map<ResourceLocation, Set<ResourceKey<Level>>> Exempt = new ConcurrentHashMap<>();

    private AnomalyRealm() {
    }

    public static void exempt(ResourceLocation DocumentId, ResourceKey<Level> Dimension) {
        Exempt.computeIfAbsent(DocumentId, Id -> ConcurrentHashMap.newKeySet()).add(Dimension);
    }

    public static boolean exempted(ResourceLocation DocumentId, ResourceKey<Level> Dimension) {
        Set<ResourceKey<Level>> Found = Exempt.get(DocumentId);
        return Found != null && Found.contains(Dimension);
    }

    public static boolean allows(Level World, ResourceLocation DocumentId) {
        return allows(World) || exempted(DocumentId, World.dimension());
    }

    public static boolean allows(Level World) {
        return allows(World.dimension(), World.dimensionTypeRegistration());
    }

    public static boolean allows(ResourceKey<Level> Dimension, Holder<DimensionType> Type) {
        return !Dimension.equals(SpaceRegistry.MoonLevel) && !Type.is(SpaceTags.Airless);
    }

    public static boolean allows(MinecraftServer Server, ResourceKey<Level> Dimension) {
        ServerLevel World = Server.getLevel(Dimension);
        return World != null ? allows(World) : !Dimension.equals(SpaceRegistry.MoonLevel);
    }
}
