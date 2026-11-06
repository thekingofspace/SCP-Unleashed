package net.rai.scp_unleashed.scp127.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp127.Scp127Voice;

public record Scp127DryPayload() implements CustomPacketPayload {
    public static final Scp127DryPayload Instance = new Scp127DryPayload();
    public static final Type<Scp127DryPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp127_dry"));
    public static final StreamCodec<ByteBuf, Scp127DryPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<Scp127DryPayload> type() {
        return PayloadType;
    }

    public static void handle(Scp127DryPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User) {
            Scp127Voice.dry(User);
        }
    }
}
