package net.rai.scp_unleashed.scp179;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkyTargets {
    private static final Map<ResourceLocation, SkyTarget> Known = new ConcurrentHashMap<>();

    private SkyTargets() {
    }

    public static void register(SkyTarget Target) {
        Known.put(Target.Id(), Target);
    }

    @Nullable
    public static SkyTarget get(@Nullable ResourceLocation Id) {
        return Id == null ? null : Known.get(Id);
    }

    public static Component label(@Nullable ResourceLocation Id) {
        SkyTarget Target = get(Id);
        if (Target != null) {
            return Target.label();
        }
        return Id == null ? Component.translatable("satellite.scp_unleashed.target.blank") : Component.literal(Id.getPath().toUpperCase(Locale.ROOT));
    }
}
