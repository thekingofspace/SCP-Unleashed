package net.rai.scp_unleashed.scp327.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Scp327Net {
    private Scp327Net() {
    }

    public static void send(ServerPlayer User, CustomPacketPayload Payload) {
        if (User.connection == null || User.hasDisconnected()) {
            return;
        }
        Connection Link = User.connection.getConnection();
        if (Link == null || Link.channel() == null || !User.connection.hasChannel(Payload.type())) {
            return;
        }
        PacketDistributor.sendToPlayer(User, Payload);
    }
}
