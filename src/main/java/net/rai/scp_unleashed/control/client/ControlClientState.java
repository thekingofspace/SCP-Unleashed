package net.rai.scp_unleashed.control.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ControlClientState {
    public record View(ResourceLocation Look, CompoundTag Identity) {
    }

    private static final Map<UUID, View> Views = new ConcurrentHashMap<>();
    private static int Revision;

    private ControlClientState() {
    }

    public static void set(UUID Player, boolean Active, ResourceLocation Look, CompoundTag Identity) {
        if (Active) {
            Views.put(Player, new View(Look, Identity.copy()));
        } else {
            Views.remove(Player);
        }
        Revision++;
    }

    public static boolean isControlled(UUID Player) {
        return Views.containsKey(Player);
    }

    @Nullable
    public static View view(UUID Player) {
        return Views.get(Player);
    }

    public static int revision() {
        return Revision;
    }

    public static void clear() {
        Views.clear();
        Revision++;
    }
}
