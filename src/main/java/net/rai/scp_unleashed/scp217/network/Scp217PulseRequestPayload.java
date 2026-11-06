package net.rai.scp_unleashed.scp217.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp217.Scp217Pulse;

public record Scp217PulseRequestPayload() implements CustomPacketPayload {
    public static final Scp217PulseRequestPayload Instance = new Scp217PulseRequestPayload();
    public static final Type<Scp217PulseRequestPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217_pulse_request"));
    public static final StreamCodec<ByteBuf, Scp217PulseRequestPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<Scp217PulseRequestPayload> type() {
        return PayloadType;
    }

    public static void handle(Scp217PulseRequestPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User) {
            Scp217Pulse.request(User);
        }
    }
}
