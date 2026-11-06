package net.rai.scp_unleashed.scp020;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.goggles.Goggles;
import net.rai.scp_unleashed.scp020.network.Scp020SporesPayload;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp020Spores {
    public static final int SyncTicks = 10;
    public static final double CameraRange = 32.0;
    public static final double GogglesRange = 24.0;
    public static final int MaxHosts = 512;
    private static final long[] Empty = new long[0];
    private static final Map<UUID, Feed> Feeds = new ConcurrentHashMap<>();
    private static volatile int Revision;

    private record Feed(ResourceKey<Level> Dimension, @Nullable BlockPos Camera, long[] Hosts, byte[] Masks, int Revision, long Checked) {
    }

    private record Spot(long Host, byte Mask, double Distance) {
    }

    private Scp020Spores() {
    }

    public static boolean onCamera(ServerPlayer User) {
        return CameraViews.isViewing(User);
    }

    public static boolean goggled(@Nullable LivingEntity Wearer) {
        return Goggles.isWearing(Wearer, Goggles.Kind.Hyperspectral);
    }

    public static boolean canSee(ServerPlayer User) {
        return User.isAlive() && !User.isRemoved() && !Senses.isBlindfolded(User) && (onCamera(User) || goggled(User));
    }

    public static Vec3 lens(ServerPlayer User) {
        return onCamera(User) ? CameraViews.eye(User) : User.getEyePosition();
    }

    public static double range(ServerPlayer User) {
        return onCamera(User) ? CameraRange : GogglesRange;
    }

    public static boolean isSyncing(ServerPlayer User) {
        return Feeds.containsKey(User.getUUID());
    }

    public static void touch() {
        Revision++;
    }

    public static void forget(UUID Viewer) {
        Feeds.remove(Viewer);
    }

    public static void clearAll() {
        Feeds.clear();
    }

    public static void tick(MinecraftServer Server) {
        long Now = Server.getTickCount();
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            update(User, Now, false);
        }
        if (!Feeds.isEmpty()) {
            Feeds.keySet().removeIf(Key -> Server.getPlayerList().getPlayer(Key) == null);
        }
    }

    public static void update(ServerPlayer User, long Now, boolean Force) {
        UUID Key = User.getUUID();
        Feed Old = Feeds.get(Key);
        if (!canSee(User)) {
            if (Old != null) {
                Feeds.remove(Key);
                if (Old.Hosts().length > 0) {
                    send(User, Scp020SporesPayload.Off);
                }
            }
            return;
        }
        ServerLevel World = User.serverLevel();
        BlockPos Camera = onCamera(User) ? CameraViews.camera(User) : null;
        if (Old != null && !Old.Dimension().equals(World.dimension())) {
            Old = null;
        }
        boolean Moved = Old == null || !Objects.equals(Old.Camera(), Camera);
        if (!Force && !Moved && Old.Revision() == Revision && Now - Old.Checked() < SyncTicks) {
            return;
        }
        Scp020SporesPayload Payload = snapshot(World, lens(User), range(User), Camera != null);
        long[] Held = Old == null ? Empty : Old.Hosts();
        byte[] HeldMasks = Old == null ? new byte[0] : Old.Masks();
        boolean HeldCamera = Old != null && Old.Camera() != null;
        boolean Same = Arrays.equals(Held, Payload.Hosts()) && Arrays.equals(HeldMasks, Payload.Masks()) && (Held.length == 0 || HeldCamera == (Camera != null));
        Feeds.put(Key, new Feed(World.dimension(), Camera, Payload.Hosts(), Payload.Masks(), Revision, Now));
        if (!Same) {
            send(User, Payload.size() == 0 ? Scp020SporesPayload.Off : Payload);
        }
    }

    public static Scp020SporesPayload snapshot(Level World, Vec3 Lens, double Range, boolean Camera) {
        List<Spot> Found = new ArrayList<>();
        double Limit = Range * Range;
        int MinChunkX = (int) Math.floor(Lens.x - Range) >> 4;
        int MaxChunkX = (int) Math.floor(Lens.x + Range) >> 4;
        int MinChunkZ = (int) Math.floor(Lens.z - Range) >> 4;
        int MaxChunkZ = (int) Math.floor(Lens.z + Range) >> 4;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int ChunkX = MinChunkX; ChunkX <= MaxChunkX; ChunkX++) {
            for (int ChunkZ = MinChunkZ; ChunkZ <= MaxChunkZ; ChunkZ++) {
                Scp020Growth Colony = Scp020Infection.colonyIn(Scp020Infection.chunkAt(World, ChunkX, ChunkZ));
                if (Colony == null) {
                    continue;
                }
                Colony.forEach((Host, Mask) -> {
                    Cursor.set(Host);
                    int Kept = 0;
                    double Nearest = Double.MAX_VALUE;
                    for (Direction Side : Direction.values()) {
                        if (!Scp020Growth.hasFace(Mask, Side)) {
                            continue;
                        }
                        double Distance = Scp020Infection.faceCentre(Cursor, Side).distanceToSqr(Lens);
                        if (Distance <= Limit) {
                            Kept |= Scp020Growth.bit(Side);
                            Nearest = Math.min(Nearest, Distance);
                        }
                    }
                    if (Kept != 0) {
                        Found.add(new Spot(Host, (byte) Kept, Nearest));
                    }
                });
            }
        }
        Found.sort(Comparator.comparingDouble(Spot::Distance).thenComparingLong(Spot::Host));
        int Count = Math.min(MaxHosts, Found.size());
        long[] Hosts = new long[Count];
        byte[] Masks = new byte[Count];
        for (int Index = 0; Index < Count; Index++) {
            Hosts[Index] = Found.get(Index).Host();
            Masks[Index] = Found.get(Index).Mask();
        }
        return new Scp020SporesPayload(true, Camera, Hosts, Masks);
    }

    private static void send(ServerPlayer User, Scp020SporesPayload Payload) {
        if (User.connection != null && User.connection.hasChannel(Scp020SporesPayload.PayloadType)) {
            PacketDistributor.sendToPlayer(User, Payload);
        }
    }
}
