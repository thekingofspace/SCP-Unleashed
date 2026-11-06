package net.rai.scp_unleashed.scp143.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp143CutPayload(int Id, float X, float Y, float Z) implements CustomPacketPayload {
    public static final Type<Scp143CutPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp143_cut"));
    public static final StreamCodec<ByteBuf, Scp143CutPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp143CutPayload::Id,
            ByteBufCodecs.FLOAT, Scp143CutPayload::X,
            ByteBufCodecs.FLOAT, Scp143CutPayload::Y,
            ByteBufCodecs.FLOAT, Scp143CutPayload::Z,
            Scp143CutPayload::new);

    @Override
    public Type<Scp143CutPayload> type() {
        return PayloadType;
    }
}
