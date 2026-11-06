package net.rai.scp_unleashed.scp3008.gear.gametest;

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
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.UUID;

public class Scp3008GearTestPlayer extends ServerPlayer {
    public Scp3008GearTestPlayer(ServerLevel World, String Name) {
        super(World.getServer(), World, new GameProfile(UUID.randomUUID(), Name), ClientInformation.createDefault());
        new Link(World.getServer(), this);
        setField(ServerPlayer.class, "spawnInvulnerableTime", 0);
    }

    private void setField(Class<?> Owner, String Name, int Value) {
        Field Found = ObfuscationReflectionHelper.findField(Owner, Name);
        try {
            Found.setInt(this, Value);
        } catch (IllegalAccessException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    public void holdUseFor(int Ticks) {
        Field Remaining = ObfuscationReflectionHelper.findField(LivingEntity.class, "useItemRemaining");
        try {
            Remaining.setInt(this, getUseItem().getUseDuration(this) - Ticks);
        } catch (IllegalAccessException Problem) {
            throw new IllegalStateException(Problem);
        }
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
