package net.rai.scp_unleashed.scp3008;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp3008.network.Scp3008FadePayload;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp3008Transit {
    public static final int FadeTicks = 14;
    public static final int RevealTicks = 40;
    public static final int Grace = 60;
    public static final double Doorstep = 4.5;
    public static final String Prefix = "message.scp_unleashed.scp3008.";
    private static final Map<UUID, Pending> Waiting = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> Moved = new ConcurrentHashMap<>();

    private record Pending(boolean Inbound, long Due, ServerPlayer User) {
    }

    public record Destination(ServerLevel World, Vec3 Spot, float Yaw, boolean Lost) {
    }

    private Scp3008Transit() {
    }

    public static boolean busy(Player User) {
        return Waiting.containsKey(User.getUUID());
    }

    public static boolean settling(ServerPlayer User) {
        Long Last = Moved.get(User.getUUID());
        long Now = User.server.getTickCount();
        return Last != null && Now >= Last && Now - Last < Grace;
    }

    public static Vec3 doorstep(BlockPos Door, Direction Front) {
        return new Vec3(Door.getX() + 0.5 + Front.getStepX() * Doorstep, Door.getY(), Door.getZ() + 0.5 + Front.getStepZ() * Doorstep);
    }

    public static void enter(ServerPlayer User, BlockPos Door, Direction Front) {
        if (busy(User) || settling(User)) {
            return;
        }
        if (Scp3008World.level(User.server) == null) {
            User.displayClientMessage(Component.translatable(Prefix + "no_store").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            Moved.put(User.getUUID(), (long) User.server.getTickCount());
            return;
        }
        Scp3008State State = Scp3008State.get(User.server);
        BlockPos Anchor = State.door() != null && State.doorDimension() == User.serverLevel().dimension() && State.door().distManhattan(Door) < 12 ? State.door() : Door;
        Direction Facing = Anchor == Door ? Front : State.front();
        State.setReturn(User.getUUID(), new Scp3008State.Spot(User.serverLevel().dimension(), doorstep(Anchor, Facing), Facing.toYRot()));
        User.serverLevel().playSound(null, Door, Scp3008Registry.DoorSlide.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        User.serverLevel().playSound(null, Door, Scp3008Registry.DoorHiss.get(), SoundSource.BLOCKS, 0.6f, 1.1f);
        begin(User, true);
    }

    public static void enterDirect(ServerPlayer User) {
        if (busy(User)) {
            return;
        }
        Scp3008State.get(User.server).setReturn(User.getUUID(), new Scp3008State.Spot(User.serverLevel().dimension(), User.position(), User.getYRot()));
        begin(User, true);
    }

    public static void leave(ServerPlayer User, BlockPos Door) {
        if (busy(User) || settling(User)) {
            return;
        }
        User.serverLevel().playSound(null, Door, Scp3008Registry.ExitBar.get(), SoundSource.BLOCKS, 0.9f, 0.9f);
        User.serverLevel().playSound(null, Door, Scp3008Registry.ExitOpen.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
        begin(User, false);
    }

    private static void begin(ServerPlayer User, boolean Inbound) {
        Waiting.put(User.getUUID(), new Pending(Inbound, User.server.getTickCount() + FadeTicks, User));
        PacketDistributor.sendToPlayer(User, new Scp3008FadePayload(Scp3008FadePayload.FadeIn, FadeTicks));
    }

    public static void forget(UUID Id) {
        Waiting.remove(Id);
        Moved.remove(Id);
    }

    public static void clear() {
        Waiting.clear();
        Moved.clear();
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
            ServerPlayer Listed = Server.getPlayerList().getPlayer(Entry.getKey());
            ServerPlayer User = Listed != null ? Listed : Entry.getValue().User();
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

    public static void move(ServerPlayer User, ServerLevel World, Vec3 Spot, float Yaw) {
        User.stopRiding();
        if (User.isFallFlying()) {
            User.stopFallFlying();
        }
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
        Moved.put(User.getUUID(), (long) User.server.getTickCount());
    }

    public static boolean arrive(ServerPlayer User) {
        ServerLevel Store = Scp3008World.level(User.server);
        if (Store == null) {
            PacketDistributor.sendToPlayer(User, new Scp3008FadePayload(Scp3008FadePayload.Cancel, 0));
            User.displayClientMessage(Component.translatable(Prefix + "no_store").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return false;
        }
        Vec3 Spot = Scp3008World.arrival(User.server, Store.players().size());
        move(User, Store, Spot, Scp3008World.ArrivalYaw);
        Store.playSound(null, Spot.x, Spot.y, Spot.z, Scp3008Registry.DoorSlide.get(), SoundSource.BLOCKS, 0.7f, 0.85f);
        PacketDistributor.sendToPlayer(User, new Scp3008FadePayload(Scp3008FadePayload.FadeOut, RevealTicks));
        return true;
    }

    public static boolean depart(ServerPlayer User) {
        Destination Home = home(User);
        move(User, Home.World(), Home.Spot(), Home.Yaw());
        Scp3008State.get(User.server).clearReturn(User.getUUID());
        Home.World().playSound(null, Home.Spot().x, Home.Spot().y, Home.Spot().z, Scp3008Registry.DoorSlide.get(), SoundSource.BLOCKS, 0.8f, 1.1f);
        PacketDistributor.sendToPlayer(User, new Scp3008FadePayload(Scp3008FadePayload.FadeOut, RevealTicks));
        User.displayClientMessage(Component.translatable(Prefix + (Home.Lost() ? "left_lost" : "left")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        return true;
    }

    public static Destination home(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Scp3008State State = Scp3008State.get(Server);
        if (State.door() != null) {
            ServerLevel World = Server.getLevel(State.doorDimension());
            if (World != null && !Scp3008World.inside(World)) {
                return new Destination(World, doorstep(State.door(), State.front()), State.front().toYRot(), false);
            }
        }
        Scp3008State.Spot Back = State.returnOf(User.getUUID());
        if (Back != null) {
            ServerLevel World = Server.getLevel(Back.Dimension());
            if (World != null && !Scp3008World.inside(World)) {
                return new Destination(World, Back.Pos(), Back.Yaw(), false);
            }
        }
        DimensionTransition Respawn = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        ServerLevel World = Respawn.newLevel();
        if (Scp3008World.inside(World)) {
            World = Server.overworld();
            return new Destination(World, Vec3.atBottomCenterOf(World.getSharedSpawnPos()), 0.0f, true);
        }
        return new Destination(World, Respawn.pos(), Respawn.yRot(), true);
    }
}
