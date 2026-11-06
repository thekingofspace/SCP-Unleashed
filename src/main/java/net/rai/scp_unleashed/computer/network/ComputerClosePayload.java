package net.rai.scp_unleashed.computer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ComputerClosePayload(BlockPos Pos, int Reason) implements CustomPacketPayload {
    public static final Type<ComputerClosePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "computer_close"));
    public static final StreamCodec<ByteBuf, ComputerClosePayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerClosePayload::Pos,
            ByteBufCodecs.VAR_INT, ComputerClosePayload::Reason,
            ComputerClosePayload::new);

    @Override
    public Type<ComputerClosePayload> type() {
        return PayloadType;
    }
}
