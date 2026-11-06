package net.rai.scp_unleashed.computer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ComputerActionPayload(BlockPos Pos, int Action, CompoundTag Data) implements CustomPacketPayload {
    public static final Type<ComputerActionPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "computer_action"));
    public static final StreamCodec<ByteBuf, ComputerActionPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerActionPayload::Pos,
            ByteBufCodecs.VAR_INT, ComputerActionPayload::Action,
            ByteBufCodecs.COMPOUND_TAG, ComputerActionPayload::Data,
            ComputerActionPayload::new);

    @Override
    public Type<ComputerActionPayload> type() {
        return PayloadType;
    }
}
