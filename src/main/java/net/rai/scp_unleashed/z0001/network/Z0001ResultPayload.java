package net.rai.scp_unleashed.z0001.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0001.Z0001Page;

public record Z0001ResultPayload(int Outcome, String Name) implements CustomPacketPayload {
    public static final Type<Z0001ResultPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0001_result"));
    public static final StreamCodec<ByteBuf, Z0001ResultPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Z0001ResultPayload::Outcome,
            ByteBufCodecs.stringUtf8(Z0001Page.MaxName), Z0001ResultPayload::Name,
            Z0001ResultPayload::new);

    @Override
    public Type<Z0001ResultPayload> type() {
        return PayloadType;
    }
}
