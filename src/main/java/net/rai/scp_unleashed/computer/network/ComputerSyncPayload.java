package net.rai.scp_unleashed.computer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ComputerSyncPayload(BlockPos Pos, int Section, CompoundTag Data) implements CustomPacketPayload {
    public static final Type<ComputerSyncPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "computer_sync"));
    public static final StreamCodec<ByteBuf, ComputerSyncPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerSyncPayload::Pos,
            ByteBufCodecs.VAR_INT, ComputerSyncPayload::Section,
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, ComputerSyncPayload::Data,
            ComputerSyncPayload::new);

    @Override
    public Type<ComputerSyncPayload> type() {
        return PayloadType;
    }
}
