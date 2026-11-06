package net.rai.scp_unleashed.scp207.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp207TollPayload(float Toll, float Rate) implements CustomPacketPayload {
    public static final Type<Scp207TollPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp207_toll"));
    public static final StreamCodec<ByteBuf, Scp207TollPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp207TollPayload::Toll,
            ByteBufCodecs.FLOAT, Scp207TollPayload::Rate,
            Scp207TollPayload::new);

    @Override
    public Type<Scp207TollPayload> type() {
        return PayloadType;
    }
}
