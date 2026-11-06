package net.rai.scp_unleashed.z0001.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0001.Z0001Page;

public record Z0001DreadPayload(String Name, int Ticks) implements CustomPacketPayload {
    public static final Type<Z0001DreadPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0001_dread"));
    public static final StreamCodec<ByteBuf, Z0001DreadPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(Z0001Page.MaxName), Z0001DreadPayload::Name,
            ByteBufCodecs.VAR_INT, Z0001DreadPayload::Ticks,
            Z0001DreadPayload::new);

    @Override
    public Type<Z0001DreadPayload> type() {
        return PayloadType;
    }
}
