package net.rai.scp_unleashed.scp4666.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp4666DiaryPayload(int Volume) implements CustomPacketPayload {
    public static final Type<Scp4666DiaryPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp4666_diary"));
    public static final StreamCodec<ByteBuf, Scp4666DiaryPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp4666DiaryPayload::Volume,
            Scp4666DiaryPayload::new);

    @Override
    public Type<Scp4666DiaryPayload> type() {
        return PayloadType;
    }
}
