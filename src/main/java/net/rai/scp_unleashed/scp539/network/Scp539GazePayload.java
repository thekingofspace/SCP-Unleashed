package net.rai.scp_unleashed.scp539.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp539GazePayload(int TargetId, double X, double Y, double Z, int Ticks) implements CustomPacketPayload {
    public static final Type<Scp539GazePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp539_gaze"));
    public static final StreamCodec<ByteBuf, Scp539GazePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp539GazePayload::TargetId,
            ByteBufCodecs.DOUBLE, Scp539GazePayload::X,
            ByteBufCodecs.DOUBLE, Scp539GazePayload::Y,
            ByteBufCodecs.DOUBLE, Scp539GazePayload::Z,
            ByteBufCodecs.VAR_INT, Scp539GazePayload::Ticks,
            Scp539GazePayload::new);

    @Override
    public Type<Scp539GazePayload> type() {
        return PayloadType;
    }
}
