package net.rai.scp_unleashed.rocketry.rocket;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record RocketryInputPayload(boolean Thrust) implements CustomPacketPayload {
    public static final Type<RocketryInputPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "rocketry_input"));
    public static final StreamCodec<ByteBuf, RocketryInputPayload> PayloadCodec = ByteBufCodecs.BOOL.map(RocketryInputPayload::new, RocketryInputPayload::Thrust);

    @Override
    public Type<RocketryInputPayload> type() {
        return PayloadType;
    }
}
