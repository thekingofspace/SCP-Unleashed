package net.rai.scp_unleashed.scp001scarlet.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;

public record ScarletSyncPayload(float Manifestation, int Broken, byte Phase, boolean Seen, float Sky, boolean HasThrone, BlockPos Throne) implements CustomPacketPayload {
    public static final Type<ScarletSyncPayload> PayloadType = new Type<>(Scp001ScarletRegistry.location("scp001_scarlet_sync"));
    public static final StreamCodec<ByteBuf, ScarletSyncPayload> PayloadCodec = StreamCodec.of(ScarletSyncPayload::write, ScarletSyncPayload::read);

    private static void write(ByteBuf Buffer, ScarletSyncPayload Payload) {
        ByteBufCodecs.FLOAT.encode(Buffer, Payload.Manifestation());
        ByteBufCodecs.VAR_INT.encode(Buffer, Payload.Broken());
        ByteBufCodecs.BYTE.encode(Buffer, Payload.Phase());
        ByteBufCodecs.BOOL.encode(Buffer, Payload.Seen());
        ByteBufCodecs.FLOAT.encode(Buffer, Payload.Sky());
        ByteBufCodecs.BOOL.encode(Buffer, Payload.HasThrone());
        BlockPos.STREAM_CODEC.encode(Buffer, Payload.Throne());
    }

    private static ScarletSyncPayload read(ByteBuf Buffer) {
        return new ScarletSyncPayload(ByteBufCodecs.FLOAT.decode(Buffer), ByteBufCodecs.VAR_INT.decode(Buffer), ByteBufCodecs.BYTE.decode(Buffer),
                ByteBufCodecs.BOOL.decode(Buffer), ByteBufCodecs.FLOAT.decode(Buffer), ByteBufCodecs.BOOL.decode(Buffer), BlockPos.STREAM_CODEC.decode(Buffer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
