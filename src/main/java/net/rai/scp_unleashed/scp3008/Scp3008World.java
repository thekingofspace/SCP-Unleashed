package net.rai.scp_unleashed.scp3008;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import org.jetbrains.annotations.Nullable;

public final class Scp3008World {
    public static final ResourceKey<Level> Key = Scp3008Registry.Dimension;
    public static final float ArrivalYaw = 0.0f;
    private static volatile ResourceKey<Level> Standin;

    private Scp3008World() {
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Found = Standin;
        return Found != null ? Found : Key;
    }

    public static void standIn(@Nullable ResourceKey<Level> Dimension) {
        if (Standin != Dimension) {
            Scp3008Clock.bind(null);
        }
        Standin = Dimension;
    }

    public static boolean standingIn() {
        return Standin != null;
    }

    public static boolean strict(@Nullable Level World) {
        return inside(World) && !standingIn();
    }

    public static boolean visitor(Player User) {
        if (!inside(User)) {
            return false;
        }
        if (!standingIn()) {
            return true;
        }
        return User instanceof ServerPlayer Shopper && (Scp3008State.get(Shopper.server).returnOf(Shopper.getUUID()) != null || Scp3008Transit.busy(Shopper)
                || Scp3008Transit.settling(Shopper));
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(key());
    }

    public static boolean inside(@Nullable Level World) {
        return Scp3008Hours.inStore(World);
    }

    public static boolean inside(Entity Target) {
        return inside(Target.level());
    }

    public static boolean exempt(Player User) {
        return User.isSpectator() || User.isCreative() && User.hasPermissions(2);
    }

    public static long seed(MinecraftServer Server) {
        return Server.getWorldData().worldGenOptions().seed();
    }

    public static BlockPos arrivalBlock(MinecraftServer Server) {
        int[] Spot = Scp3008Plan.arrival(seed(Server));
        return new BlockPos(Spot[0], Spot[1], Spot[2]);
    }

    public static Vec3 arrival(MinecraftServer Server, int Index) {
        BlockPos Spot = arrivalBlock(Server);
        double Offset = (Index % 3) * 0.7;
        return new Vec3(Spot.getX() + 0.5 + Offset, Spot.getY(), Spot.getZ() + 0.5);
    }
}
