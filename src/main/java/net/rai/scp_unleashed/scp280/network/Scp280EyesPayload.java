package net.rai.scp_unleashed.scp280.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp280.Scp280Registry;

import java.util.List;

public record Scp280EyesPayload(byte Kind, List<Spot> Spots) implements CustomPacketPayload {
    public static final byte Show = 0;
    public static final byte Clear = 1;
    public static final int MaxSpots = 8;
    public static final Type<Scp280EyesPayload> PayloadType = new Type<>(Scp280Registry.location("scp280_eyes"));

    public record Spot(float X, float Y, float Z, byte Variant, int Delay, int Life) {
        public static final StreamCodec<ByteBuf, Spot> SpotCodec = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Spot::X,
                ByteBufCodecs.FLOAT, Spot::Y,
                ByteBufCodecs.FLOAT, Spot::Z,
                ByteBufCodecs.BYTE, Spot::Variant,
                ByteBufCodecs.VAR_INT, Spot::Delay,
                ByteBufCodecs.VAR_INT, Spot::Life,
                Spot::new);
    }

    public static final StreamCodec<ByteBuf, Scp280EyesPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BYTE, Scp280EyesPayload::Kind,
            Spot.SpotCodec.apply(ByteBufCodecs.list(MaxSpots)), Scp280EyesPayload::Spots,
            Scp280EyesPayload::new);

    public static Scp280EyesPayload show(List<Spot> Spots) {
        return new Scp280EyesPayload(Show, List.copyOf(Spots.subList(0, Math.min(MaxSpots, Spots.size()))));
    }

    public static Scp280EyesPayload clear() {
        return new Scp280EyesPayload(Clear, List.of());
    }

    @Override
    public Type<Scp280EyesPayload> type() {
        return PayloadType;
    }
}
