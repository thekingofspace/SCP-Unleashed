package net.rai.scp_unleashed.scp902.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp902.Scp902Registry;

public record Scp902ScenePayload(int Kind, int Ticks) implements CustomPacketPayload {
    public static final int Depart = 0;
    public static final int Arrive = 1;
    public static final int Home = 2;
    public static final int Clear = 3;
    public static final Type<Scp902ScenePayload> PayloadType = new Type<>(Scp902Registry.location("scp902_scene"));
    public static final StreamCodec<ByteBuf, Scp902ScenePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp902ScenePayload::Kind,
            ByteBufCodecs.VAR_INT, Scp902ScenePayload::Ticks,
            Scp902ScenePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
