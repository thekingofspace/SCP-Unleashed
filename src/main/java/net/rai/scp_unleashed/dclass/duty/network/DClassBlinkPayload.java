package net.rai.scp_unleashed.dclass.duty.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record DClassBlinkPayload(int EntityId) implements CustomPacketPayload {
    public static final Type<DClassBlinkPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass_blink"));
    public static final StreamCodec<ByteBuf, DClassBlinkPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DClassBlinkPayload::EntityId,
            DClassBlinkPayload::new);

    @Override
    public Type<DClassBlinkPayload> type() {
        return PayloadType;
    }
}
