package net.rai.scp_unleashed.gateway.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.common.util.FakePlayer;

public class GatewayTraveller extends FakePlayer {
    private boolean Dozing;

    public GatewayTraveller(ServerLevel World, GameProfile Profile) {
        super(World, Profile);
        this.connection = new Quiet(World.getServer(), this);
    }

    public void doze(boolean Asleep) {
        Dozing = Asleep;
    }

    @Override
    public boolean isSleeping() {
        return Dozing || super.isSleeping();
    }

    @Override
    public boolean isSleepingLongEnough() {
        return Dozing;
    }

    @Override
    public void stopSleepInBed(boolean WakeImmediately, boolean UpdateLevel) {
        Dozing = false;
        if (super.isSleeping()) {
            super.stopSleepInBed(WakeImmediately, UpdateLevel);
        }
    }

    private static final class Line extends Connection {
        Line() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public void setListenerForServerboundHandshake(PacketListener Listener) {
        }
    }

    private static final class Quiet extends ServerGamePacketListenerImpl {
        Quiet(MinecraftServer Server, ServerPlayer User) {
            super(Server, new Line(), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
        }

        @Override
        public void tick() {
        }

        @Override
        public void resetPosition() {
        }

        @Override
        public void disconnect(Component Message) {
        }

        @Override
        public void send(Packet<?> Sent) {
        }

        @Override
        public void send(CustomPacketPayload Payload) {
        }

        @Override
        public boolean hasChannel(CustomPacketPayload.Type<?> Type) {
            return false;
        }

        @Override
        public boolean hasChannel(ResourceLocation Id) {
            return false;
        }
    }
}
