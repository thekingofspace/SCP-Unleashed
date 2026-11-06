package net.rai.scp_unleashed.scp140.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp140.Scp140Reading;

public record Scp140ClosedPayload() implements CustomPacketPayload {
    public static final Scp140ClosedPayload Instance = new Scp140ClosedPayload();
    public static final Type<Scp140ClosedPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp140_closed"));
    public static final StreamCodec<ByteBuf, Scp140ClosedPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<Scp140ClosedPayload> type() {
        return PayloadType;
    }

    public static void handle(Scp140ClosedPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User) {
            Scp140Reading.closed(User);
        }
    }
}
