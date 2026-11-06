package net.rai.scp_unleashed.z0002.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Z0002ChannelPayload(int EntityId, int Ticks, boolean MainHand) implements CustomPacketPayload {
    public static final Type<Z0002ChannelPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0002_channel"));
    public static final StreamCodec<ByteBuf, Z0002ChannelPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Z0002ChannelPayload::EntityId,
            ByteBufCodecs.VAR_INT, Z0002ChannelPayload::Ticks,
            ByteBufCodecs.BOOL, Z0002ChannelPayload::MainHand,
            Z0002ChannelPayload::new);

    @Override
    public Type<Z0002ChannelPayload> type() {
        return PayloadType;
    }
}
