package net.rai.scp_unleashed.scp093;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class Scp093Wastes {
    public static final ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, Scp093Registry.location("scp093_wastes"));

    private Scp093Wastes() {
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(Key);
    }

    public static boolean inside(@Nullable Level World) {
        return World != null && World.dimension() == Key;
    }

    public static boolean inside(Entity Target) {
        return inside(Target.level());
    }
}
