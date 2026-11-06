package net.rai.scp_unleashed.scp428.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp428HoldPayload(int Holders, int Presses, int Required) implements CustomPacketPayload {
    public static final Type<Scp428HoldPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp428_hold"));
    public static final StreamCodec<ByteBuf, Scp428HoldPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp428HoldPayload::Holders,
            ByteBufCodecs.VAR_INT, Scp428HoldPayload::Presses,
            ByteBufCodecs.INT, Scp428HoldPayload::Required,
            Scp428HoldPayload::new);
    public static final Scp428HoldPayload Released = new Scp428HoldPayload(0, 0, 0);

    @Override
    public Type<Scp428HoldPayload> type() {
        return PayloadType;
    }
}
