package net.rai.scp_unleashed.scp444.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp444.Scp444Reading;

public record Scp444ClosedPayload() implements CustomPacketPayload {
    public static final Scp444ClosedPayload Instance = new Scp444ClosedPayload();
    public static final Type<Scp444ClosedPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp444_closed"));
    public static final StreamCodec<ByteBuf, Scp444ClosedPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<Scp444ClosedPayload> type() {
        return PayloadType;
    }

    public static void handle(Scp444ClosedPayload Payload, IPayloadContext Context) {
        Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp444Reading.closed(User);
            }
        });
    }
}
