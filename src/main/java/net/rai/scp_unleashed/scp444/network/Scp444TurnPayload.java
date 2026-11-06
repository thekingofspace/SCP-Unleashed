package net.rai.scp_unleashed.scp444.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp444.Scp444Reading;

public record Scp444TurnPayload(int Page) implements CustomPacketPayload {
    public static final Type<Scp444TurnPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp444_turn"));
    public static final StreamCodec<ByteBuf, Scp444TurnPayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.VAR_INT, Scp444TurnPayload::Page, Scp444TurnPayload::new);

    @Override
    public Type<Scp444TurnPayload> type() {
        return PayloadType;
    }

    public static void handle(Scp444TurnPayload Payload, IPayloadContext Context) {
        Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp444Reading.turned(User, Payload.Page());
            }
        });
    }
}
