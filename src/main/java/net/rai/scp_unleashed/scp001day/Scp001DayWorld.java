package net.rai.scp_unleashed.scp001day;

import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp001DayWorld {
    public static final ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001_day"));
    public static final int StandinBaseX = 3072000;
    public static final int StandinBaseZ = 3072000;
    private static volatile ResourceKey<Level> Standin;

    private Scp001DayWorld() {
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Borrowed = Standin;
        return Borrowed != null ? Borrowed : Key;
    }

    public static void standIn(@Nullable ResourceKey<Level> Dimension) {
        Standin = Dimension;
    }

    public static boolean standingIn() {
        return Standin != null;
    }

    public static int baseX() {
        return standingIn() ? StandinBaseX : 0;
    }

    public static int baseZ() {
        return standingIn() ? StandinBaseZ : 0;
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(key());
    }

    public static boolean inside(@Nullable Level World) {
        return World != null && World.dimension() == key();
    }

    public static boolean inside(@Nullable Entity Body) {
        return Body != null && inside(Body.level());
    }

    public static boolean strict(@Nullable Level World) {
        return inside(World) && !standingIn();
    }
}
