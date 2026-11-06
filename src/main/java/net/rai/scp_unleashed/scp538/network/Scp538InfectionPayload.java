package net.rai.scp_unleashed.scp538.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp538InfectionPayload(int Stage, int Progress) implements CustomPacketPayload {
    public static final Type<Scp538InfectionPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp538_infection"));
    public static final StreamCodec<ByteBuf, Scp538InfectionPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp538InfectionPayload::Stage,
            ByteBufCodecs.VAR_INT, Scp538InfectionPayload::Progress,
            Scp538InfectionPayload::new);

    @Override
    public Type<Scp538InfectionPayload> type() {
        return PayloadType;
    }
}
