package net.rai.scp_unleashed.scp4666.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp4666PosePayload(int EntityId, boolean Lying) implements CustomPacketPayload {
    public static final Type<Scp4666PosePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp4666_pose"));
    public static final StreamCodec<ByteBuf, Scp4666PosePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp4666PosePayload::EntityId,
            ByteBufCodecs.BOOL, Scp4666PosePayload::Lying,
            Scp4666PosePayload::new);

    @Override
    public Type<Scp4666PosePayload> type() {
        return PayloadType;
    }
}
