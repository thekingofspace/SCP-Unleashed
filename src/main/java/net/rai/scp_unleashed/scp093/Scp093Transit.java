package net.rai.scp_unleashed.scp093;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp093.network.Scp093FlashPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp093Transit {
    public static final int FadeTicks = 16;
    public static final int RevealTicks = 44;
    public static final double CompanionReach = 10.0;
    public static final double Doorstep = 1.3;
    public static final int Grace = 600;
    public static final String Prefix = "message.scp_unleashed.scp093.";
    private static final Map<UUID, Pending> Waiting = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> Arrivals = new ConcurrentHashMap<>();

    private record Pending(boolean Inbound, long Due) {
    }

    private record Destination(ServerLevel World, Vec3 Spot, float Yaw, boolean Lost) {
    }

    private Scp093Transit() {
    }

    public static boolean busy(Player User) {
        return Waiting.containsKey(User.getUUID());
    }

    public static long arrivedAt(Player User) {
        return Arrivals.getOrDefault(User.getUUID(), Long.MIN_VALUE / 2);
    }

    public static boolean inGrace(Player User, long Now) {
        return Now - arrivedAt(User) < Grace;
    }

    public static Vec3 doorstep(BlockPos Lower, Direction Front) {
        return new Vec3(Lower.getX() + 0.5 + Front.getStepX() * Doorstep, Lower.getY(), Lower.getZ() + 0.5 + Front.getStepZ() * Doorstep);
    }

    public static void enterByMirror(ServerPlayer User, BlockPos Lower, Direction Front) {
        if (busy(User)) {
            return;
        }
        Scp093World.get(User.server).setOrigin(User.getUUID(), new Scp093World.Origin(User.serverLevel().dimension(), Lower.immutable(), doorstep(Lower, Front), Front.toYRot()));
        play(User.serverLevel(), Vec3.atCenterOf(Lower.above()), Scp093Registry.TransitLayers, 1.0f, 1.0f);
        begin(User, true);
    }

    public static void enterDirect(ServerPlayer User) {
        if (busy(User)) {
            return;
        }
        Scp093World.get(User.server).setOrigin(User.getUUID(), new Scp093World.Origin(User.serverLevel().dimension(), null, User.position(), User.getYRot()));
        play(User.serverLevel(), User.position(), Scp093Registry.TransitLayers, 0.9f, 1.0f);
        begin(User, true);
    }

    public static void leave(ServerPlayer User) {
        if (busy(User)) {
            return;
        }
        play(User.serverLevel(), User.position(), Scp093Registry.TransitLayers, 1.0f, 0.85f);
        begin(User, false);
    }

    private static void begin(ServerPlayer User, boolean Inbound) {
        Waiting.put(User.getUUID(), new Pending(Inbound, User.server.getTickCount() + FadeTicks));
        PacketDistributor.sendToPlayer(User, new Scp093FlashPayload(Scp093FlashPayload.FadeIn, FadeTicks));
    }

    public static void forget(UUID Id) {
        Waiting.remove(Id);
        Arrivals.remove(Id);
    }

    public static void clear() {
        Waiting.clear();
        Arrivals.clear();
    }

    public static void tick(MinecraftServer Server) {
        if (Waiting.isEmpty()) {
            return;
        }
        long Now = Server.getTickCount();
        for (Map.Entry<UUID, Pending> Entry : new ArrayList<>(Waiting.entrySet())) {
            if (Entry.getValue().Due() > Now) {
                continue;
            }
            Waiting.remove(Entry.getKey());
            ServerPlayer User = Server.getPlayerList().getPlayer(Entry.getKey());
            if (User == null || !User.isAlive() || User.isRemoved()) {
                continue;
            }
            if (Entry.getValue().Inbound()) {
                arrive(User);
            } else {
                depart(User);
            }
        }
    }

    private static void arrive(ServerPlayer User) {
        ServerLevel Wastes = Scp093Wastes.level(User.server);
        if (Wastes == null) {
            PacketDistributor.sendToPlayer(User, new Scp093FlashPayload(Scp093FlashPayload.Cancel, 0));
            User.displayClientMessage(Component.translatable(Prefix + "no_dimension").withStyle(ChatFormatting.RED), true);
            return;
        }
        Scp093World.Origin From = Scp093World.get(User.server).originOf(User.getUUID());
        Vec3 Anchor = From != null && From.Gateway() != null ? Vec3.atCenterOf(From.Gateway()) : User.position();
        Scp093Arrival.Gate Frame = Scp093Arrival.gateFor(Wastes, (int) Math.floor(Anchor.x), (int) Math.floor(Anchor.z));
        List<Mob> Companions = companions(User);
        move(User, Wastes, Frame.Spot(), Frame.Yaw());
        bring(User, Companions, Wastes, Frame.Spot(), Frame.Yaw());
        Arrivals.put(User.getUUID(), Wastes.getGameTime());
        play(Wastes, Frame.Spot(), Scp093Registry.ArriveLayers, 1.0f, 1.0f);
        PacketDistributor.sendToPlayer(User, new Scp093FlashPayload(Scp093FlashPayload.FadeOut, RevealTicks));
    }

    private static void depart(ServerPlayer User) {
        Destination Home = home(User);
        List<Mob> Companions = companions(User);
        move(User, Home.World(), Home.Spot(), Home.Yaw());
        bring(User, Companions, Home.World(), Home.Spot(), Home.Yaw());
        Scp093World.get(User.server).clearOrigin(User.getUUID());
        Arrivals.remove(User.getUUID());
        play(Home.World(), Home.Spot(), Scp093Registry.ArriveLayers, 0.8f, 1.25f);
        PacketDistributor.sendToPlayer(User, new Scp093FlashPayload(Scp093FlashPayload.FadeOut, RevealTicks));
        if (Home.Lost()) {
            User.displayClientMessage(Component.translatable(Prefix + "way_closed").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        }
    }

    public static boolean mirrorReady(ServerLevel World, BlockPos Lower) {
        BlockState State = World.getBlockState(Lower);
        if (!(State.getBlock() instanceof Scp093MirrorBlock) || !State.getValue(Scp093MirrorBlock.Seated)) {
            return false;
        }
        return World.getBlockEntity(Lower) instanceof Scp093MirrorBlockEntity Holder && Holder.hasDisc();
    }

    private static Destination home(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Scp093World.Origin From = Scp093World.get(Server).originOf(User.getUUID());
        if (From != null && From.Dimension() != Scp093Wastes.Key) {
            ServerLevel World = Server.getLevel(From.Dimension());
            if (World != null && (From.Gateway() == null || mirrorReady(World, From.Gateway()))) {
                return new Destination(World, From.Spot(), From.Yaw(), false);
            }
        }
        DimensionTransition Respawn = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        ServerLevel World = Respawn.newLevel();
        if (Scp093Wastes.inside(World)) {
            World = Server.overworld();
            return new Destination(World, Vec3.atBottomCenterOf(World.getSharedSpawnPos()), 0.0f, true);
        }
        return new Destination(World, Respawn.pos(), Respawn.yRot(), true);
    }

    private static List<Mob> companions(ServerPlayer User) {
        List<Mob> Found = new ArrayList<>();
        for (Mob Candidate : User.serverLevel().getEntitiesOfClass(Mob.class, User.getBoundingBox().inflate(CompanionReach))) {
            if (Candidate.isAlive() && Candidate instanceof Leashable Tied && Tied.getLeashHolder() == User && ControlBodies.isPersonLike(Candidate)) {
                Found.add(Candidate);
            }
        }
        return Found;
    }

    private static void bring(ServerPlayer User, List<Mob> Companions, ServerLevel Target, Vec3 Spot, float Yaw) {
        int Index = 0;
        for (Mob Companion : Companions) {
            double Angle = Math.toRadians(Yaw + 150.0 + 60.0 * (Index++ % 2 == 0 ? 0 : 1));
            Vec3 Place = Spot.add(-Math.sin(Angle) * 1.2, 0.0, Math.cos(Angle) * 1.2);
            Companion.stopRiding();
            Entity Moved = Companion.level() == Target ? teleportWithin(Companion, Place, Yaw)
                    : Companion.changeDimension(new DimensionTransition(Target, Place, Vec3.ZERO, Yaw, 0.0f, DimensionTransition.DO_NOTHING));
            if (Moved instanceof Mob Arrived && Arrived instanceof Leashable Tied && Arrived.isAlive()) {
                Tied.setLeashedTo(User, true);
            }
        }
    }

    private static Entity teleportWithin(Mob Companion, Vec3 Place, float Yaw) {
        Companion.teleportTo(Place.x, Place.y, Place.z);
        Companion.setYRot(Yaw);
        return Companion;
    }

    public static void move(ServerPlayer User, ServerLevel World, Vec3 Spot, float Yaw) {
        User.stopRiding();
        if (User.isFallFlying()) {
            User.stopFallFlying();
        }
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.PLAYERS, Volume, Pitch);
        }
    }

    @Nullable
    public static Scp093World.Origin originOf(ServerPlayer User) {
        return Scp093World.get(User.server).originOf(User.getUUID());
    }

    public static boolean isPersonLike(LivingEntity Target) {
        return ControlBodies.isPersonLike(Target);
    }
}
