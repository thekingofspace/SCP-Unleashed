package net.rai.scp_unleashed.scp427.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp427StatePayload(boolean Open, int Stage, int Transform) implements CustomPacketPayload {
    public static final Type<Scp427StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp427_state"));
    public static final StreamCodec<ByteBuf, Scp427StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp427StatePayload::Open,
            ByteBufCodecs.VAR_INT, Scp427StatePayload::Stage,
            ByteBufCodecs.VAR_INT, Scp427StatePayload::Transform,
            Scp427StatePayload::new);

    @Override
    public Type<Scp427StatePayload> type() {
        return PayloadType;
    }
}
