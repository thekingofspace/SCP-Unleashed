package net.rai.scp_unleashed.scp087;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

public final class Scp087Stairwell {
    public static final ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp087_stairwell"));

    private Scp087Stairwell() {
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(Key);
    }

    public static boolean inStairwell(@Nullable Level World) {
        return World != null && World.dimension() == Key;
    }

    public static boolean inStairwell(Entity Target) {
        return inStairwell(Target.level());
    }

    public static int originX(int Slot) {
        return Slot * Scp087Layout.SlotSpacing;
    }

    public static int slotAt(double X) {
        return (int) Math.floor((X + Scp087Layout.SlotSpacing / 2.0) / Scp087Layout.SlotSpacing);
    }

    public static AABB box(int Slot) {
        int X = originX(Slot);
        return new AABB(X - 2, Scp087Layout.BottomFloor - 4, -2, X + Scp087Layout.Width + 2, Scp087Layout.Ceiling + 8, Scp087Layout.Length + 2);
    }

    public static Vec3 arrival(int Slot) {
        return new Vec3(originX(Slot) + Scp087Layout.DoorX + 0.5, Scp087Layout.WalkTop, 2.2);
    }

    public static double localX(int Slot, double X) {
        return X - originX(Slot);
    }
}
