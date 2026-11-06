package net.rai.scp_unleashed.scp140.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp140.Scp140Reading;

public record Scp140SeenPayload(int Page) implements CustomPacketPayload {
    public static final Type<Scp140SeenPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp140_seen"));
    public static final StreamCodec<ByteBuf, Scp140SeenPayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.VAR_INT, Scp140SeenPayload::Page, Scp140SeenPayload::new);

    @Override
    public Type<Scp140SeenPayload> type() {
        return PayloadType;
    }

    public static void handle(Scp140SeenPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User) {
            Scp140Reading.seen(User, Payload.Page());
        }
    }
}
