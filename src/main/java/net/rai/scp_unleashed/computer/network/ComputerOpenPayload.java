package net.rai.scp_unleashed.computer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ComputerOpenPayload(BlockPos Pos, int Screen, CompoundTag Data) implements CustomPacketPayload {
    public static final int ComputerScreen = 0;
    public static final int DatabaseScreen = 1;
    public static final Type<ComputerOpenPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "computer_open"));
    public static final StreamCodec<ByteBuf, ComputerOpenPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerOpenPayload::Pos,
            ByteBufCodecs.VAR_INT, ComputerOpenPayload::Screen,
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, ComputerOpenPayload::Data,
            ComputerOpenPayload::new);

    @Override
    public Type<ComputerOpenPayload> type() {
        return PayloadType;
    }
}
