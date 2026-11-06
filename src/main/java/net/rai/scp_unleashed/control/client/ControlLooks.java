package net.rai.scp_unleashed.control.client;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ControlLooks {
    private static final Map<ResourceLocation, ControlLook> Looks = new ConcurrentHashMap<>();

    private ControlLooks() {
    }

    public static void register(ResourceLocation Id, ControlLook Look) {
        Looks.put(Id, Look);
    }

    @Nullable
    public static ControlLook get(ResourceLocation Id) {
        return Looks.get(Id);
    }
}
