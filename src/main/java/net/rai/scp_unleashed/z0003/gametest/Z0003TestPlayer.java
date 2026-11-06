package net.rai.scp_unleashed.z0003.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Z0003TestPlayer extends ServerPlayer {
    public final List<ClientboundContainerSetSlotPacket> SlotUpdates = new ArrayList<>();

    public Z0003TestPlayer(ServerLevel World, String Name) {
        super(World.getServer(), World, new GameProfile(UUID.randomUUID(), Name), ClientInformation.createDefault());
        new Link(World.getServer(), this);
        Field Grace = ObfuscationReflectionHelper.findField(ServerPlayer.class, "spawnInvulnerableTime");
        try {
            Grace.setInt(this, 0);
        } catch (IllegalAccessException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    @Override
    public void awardStat(Stat<?> Counted, int Amount) {
    }

    public void release(InteractionHand Hand, int HeldTicks) {
        startUsingItem(Hand);
        useItemRemaining = getUseItem().getUseDuration(this) - HeldTicks;
        releaseUsingItem();
    }

    @SuppressWarnings("unchecked")
    public void enlist(boolean Joining) {
        PlayerList Roster = server.getPlayerList();
        Field Players = ObfuscationReflectionHelper.findField(PlayerList.class, "players");
        Field ByUuid = ObfuscationReflectionHelper.findField(PlayerList.class, "playersByUUID");
        try {
            List<ServerPlayer> Listed = (List<ServerPlayer>) Players.get(Roster);
            Map<UUID, ServerPlayer> Keyed = (Map<UUID, ServerPlayer>) ByUuid.get(Roster);
            if (Joining) {
                Listed.add(this);
                Keyed.put(getUUID(), this);
            } else {
                Listed.remove(this);
                Keyed.remove(getUUID(), this);
            }
        } catch (IllegalAccessException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    public static final class Link extends ServerGamePacketListenerImpl {
        public Link(MinecraftServer Server, ServerPlayer User) {
            super(Server, Quiet.make(), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
        }

        @Override
        public void send(Packet<?> Sent) {
            record(Sent);
        }

        @Override
        public void send(Packet<?> Sent, @Nullable PacketSendListener Listener) {
            record(Sent);
        }

        private void record(Packet<?> Sent) {
            if (Sent instanceof ClientboundContainerSetSlotPacket Update && player instanceof Z0003TestPlayer Recorder) {
                Recorder.SlotUpdates.add(Update);
            }
        }

        @Override
        public void tick() {
        }

        @Override
        public void disconnect(DisconnectionDetails Details) {
        }
    }

    private static final class Quiet extends Connection {
        private Quiet() {
            super(PacketFlow.SERVERBOUND);
        }

        private static Quiet make() {
            Quiet Made = new Quiet();
            new EmbeddedChannel(Made);
            return Made;
        }

        @Override
        public void setListenerForServerboundHandshake(PacketListener Listener) {
        }
    }
}
