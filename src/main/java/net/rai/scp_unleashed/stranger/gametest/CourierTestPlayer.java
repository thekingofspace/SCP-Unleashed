package net.rai.scp_unleashed.stranger.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CourierTestPlayer extends ServerPlayer {
    public CourierTestPlayer(ServerLevel World, String Name) {
        super(World.getServer(), World, new GameProfile(UUID.randomUUID(), Name), ClientInformation.createDefault());
        new Link(World.getServer(), this);
    }

    @Override
    public void awardStat(Stat<?> Counted, int Amount) {
    }

    public static final class Link extends ServerGamePacketListenerImpl {
        public Link(MinecraftServer Server, ServerPlayer User) {
            super(Server, Quiet.make(), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
        }

        @Override
        public void send(Packet<?> Sent) {
        }

        @Override
        public void send(Packet<?> Sent, @Nullable PacketSendListener Listener) {
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
