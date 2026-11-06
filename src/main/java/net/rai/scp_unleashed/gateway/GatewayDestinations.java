package net.rai.scp_unleashed.gateway;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GatewayDestinations {
    private static final Map<ResourceLocation, GatewayDestination> ById = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<ResourceKey<Level>, GatewayDestination> ByDimension = new ConcurrentHashMap<>();

    private GatewayDestinations() {
    }

    public static GatewayDestination register(GatewayDestination Destination) {
        GatewayDestination Before = ById.putIfAbsent(Destination.Id(), Destination);
        if (Before != null) {
            throw new IllegalStateException("gateway destination " + Destination.Id() + " is already registered");
        }
        ByDimension.put(Destination.Dimension(), Destination);
        return Destination;
    }

    @Nullable
    public static GatewayDestination get(@Nullable ResourceLocation Id) {
        return Id == null ? null : ById.get(Id);
    }

    @Nullable
    public static GatewayDestination of(ItemStack Stack) {
        return get(Stack.get(GatewayRegistry.Destination.get()));
    }

    @Nullable
    public static GatewayDestination inDimension(@Nullable ResourceKey<Level> Dimension) {
        return Dimension == null ? null : ByDimension.get(Dimension);
    }

    public static boolean sealed(@Nullable ResourceKey<Level> Dimension) {
        GatewayDestination Found = inDimension(Dimension);
        return Found != null && Found.Rules().StashBelongings();
    }

    public static Collection<GatewayDestination> all() {
        synchronized (ById) {
            return List.copyOf(ById.values());
        }
    }
}
