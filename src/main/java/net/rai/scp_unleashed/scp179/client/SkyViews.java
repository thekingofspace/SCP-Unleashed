package net.rai.scp_unleashed.scp179.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp179.Scp179Registry;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkyViews {
    private static final Map<ResourceLocation, SkyView> Views = new ConcurrentHashMap<>();

    static {
        register(Scp179Registry.TargetId, new Scp179View());
    }

    private SkyViews() {
    }

    public static void register(ResourceLocation Target, SkyView View) {
        Views.put(Target, View);
    }

    @Nullable
    public static SkyView get(@Nullable ResourceLocation Target) {
        return Target == null ? null : Views.get(Target);
    }
}
