package net.rai.scp_unleashed.scp087.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record Scp087ShiftPayload(double Dx, double Dy, List<Integer> Ids) implements CustomPacketPayload {
    public static final int MaxIds = 512;
    public static final Type<Scp087ShiftPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp087_shift"));
    public static final StreamCodec<ByteBuf, Scp087ShiftPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Scp087ShiftPayload::Dx,
            ByteBufCodecs.DOUBLE, Scp087ShiftPayload::Dy,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MaxIds)), Scp087ShiftPayload::Ids,
            Scp087ShiftPayload::new);

    @Override
    public Type<Scp087ShiftPayload> type() {
        return PayloadType;
    }
}
