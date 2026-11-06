package net.rai.scp_unleashed.scp2521.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;

public record Scp2521ScenePayload(int Kind, int Ticks, int Target, float Yaw, float Pitch) implements CustomPacketPayload {
    public static final int Begin = 0;
    public static final int Void = 1;
    public static final int Fall = 2;
    public static final int Home = 3;
    public static final int Clear = 4;
    public static final Type<Scp2521ScenePayload> PayloadType = new Type<>(Scp2521Registry.location("scp2521_scene"));
    public static final StreamCodec<ByteBuf, Scp2521ScenePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp2521ScenePayload::Kind,
            ByteBufCodecs.VAR_INT, Scp2521ScenePayload::Ticks,
            ByteBufCodecs.VAR_INT, Scp2521ScenePayload::Target,
            ByteBufCodecs.FLOAT, Scp2521ScenePayload::Yaw,
            ByteBufCodecs.FLOAT, Scp2521ScenePayload::Pitch,
            Scp2521ScenePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
