package net.rai.scp_unleashed.scp5001.mekhane.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record MekhaneConsolePayload(BlockPos Pos, int Action) implements CustomPacketPayload {
    public static final int Engage = 1;
    public static final Type<MekhaneConsolePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "mekhane_console"));
    public static final StreamCodec<ByteBuf, MekhaneConsolePayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MekhaneConsolePayload::Pos,
            ByteBufCodecs.VAR_INT, MekhaneConsolePayload::Action,
            MekhaneConsolePayload::new);

    @Override
    public Type<MekhaneConsolePayload> type() {
        return PayloadType;
    }
}
